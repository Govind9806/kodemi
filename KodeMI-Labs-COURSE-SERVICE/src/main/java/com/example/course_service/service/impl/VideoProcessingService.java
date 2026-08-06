package com.example.course_service.service.impl;

import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.service.CourseService;
import com.example.course_service.service.FileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class VideoProcessingService {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);

    private static final String STATUS_READY = "READY";
    private static final String STATUS_FAILED = "FAILED";
    private static final String OWNER_ONLY_PERMISSIONS = "rwx------";
    private static final String THUMBNAIL_CONTENT_TYPE = "image/jpeg";

    private final S3Client s3Client;
    private final FileService fileService;
    private final LessonRepository lessonRepository;
    private final CourseService courseService;
    private final String ffprobePath;
    private final String ffmpegPath;

    public VideoProcessingService(S3Client s3Client,
                                  FileService fileService,
                                  LessonRepository lessonRepository,
                                  CourseService courseService,
                                  @Value("${ffprobe.path}") String ffprobePath,
                                  @Value("${ffmpeg.path}") String ffmpegPath) {
        this.s3Client = s3Client;
        this.fileService = fileService;
        this.lessonRepository = lessonRepository;
        this.courseService = courseService;
        this.ffprobePath = ffprobePath;
        this.ffmpegPath = ffmpegPath;
    }

    @Async
    public void processUploadedVideoAsync(LessonEntity lesson, CourseEntity course, LessonEntity.ContentItem item) {
        if (lesson == null || course == null || item == null || item.getKey() == null) {
            return;
        }

        Path tempDir = null;
        Path tempVideo = null;
        Path tempThumbnail = null;

        try {
            tempDir = createPrivateTempDirectory();
            tempVideo = Files.createTempFile(tempDir, "course-video-", ".tmp");
            tempThumbnail = Files.createTempFile(tempDir, "course-thumb-", ".jpg");

            processVideo(lesson, course, item, tempVideo, tempThumbnail);
        } catch (InterruptedException e) {
            log.error("Video processing interrupted for lesson {}: {}", lesson.getLessonId(), e.getMessage());
            Thread.currentThread().interrupt();
            markFailed(lesson, item);
        } catch (IOException | RuntimeException e) {
            log.error("Video processing failed for lesson {}: {}", lesson.getLessonId(), e.getMessage(), e);
            markFailed(lesson, item);
        } finally {
            deleteIfExists(tempVideo);
            deleteIfExists(tempThumbnail);
            deleteIfExists(tempDir);
        }
    }

    private void processVideo(LessonEntity lesson, CourseEntity course, LessonEntity.ContentItem item,
                              Path tempVideo, Path tempThumbnail) throws IOException, InterruptedException {
        downloadFromS3(item.getKey(), tempVideo);

        int durationSeconds = probeDuration(tempVideo);
        generateThumbnail(tempVideo, tempThumbnail);

        String thumbnailKey = buildThumbnailKey(course, lesson);
        uploadThumbnail(thumbnailKey, tempThumbnail);

        item.setProcessedKey(thumbnailKey);
        item.setStatus(STATUS_READY);
        lesson.setDuration(durationSeconds);
        lesson.setUpdatedAt(Instant.now());
        lessonRepository.save(lesson);

        courseService.refreshCourseStats(course.getCourseId());

        log.info("Video processing complete lessonId={} thumbnailKey={}", lesson.getLessonId(), thumbnailKey);
    }

    private String buildThumbnailKey(CourseEntity course, LessonEntity lesson) {
        return "%s/%s/%s/thumbnail-%s.jpg".formatted(
                course.getCourseId(),
                lesson.getModuleId(),
                lesson.getLessonId(),
                UUID.randomUUID());
    }

    private Path createPrivateTempDirectory() throws IOException {
        try {
            // Create a private temp directory atomically (owner-only) at creation time on POSIX systems
            FileAttribute<Set<PosixFilePermission>> ownerOnlyAttr =
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(OWNER_ONLY_PERMISSIONS));
            return Files.createTempDirectory("course-video-dir-", ownerOnlyAttr);
        } catch (UnsupportedOperationException e) {
            log.debug("POSIX permissions not supported on this OS, using user home directory fallback");
            Path userHomeDir = Paths.get(System.getProperty("user.home"), ".course-service-temp");
            if (!Files.exists(userHomeDir)) {
                Files.createDirectories(userHomeDir);
            }
            Path dir = userHomeDir.resolve("course-video-dir-" + UUID.randomUUID());
            Files.createDirectory(dir);

            File file = dir.toFile();
            boolean readable = file.setReadable(true, true);
            boolean writable = file.setWritable(true, true);
            boolean executable = file.setExecutable(true, true);
            if (!readable || !writable || !executable) {
                log.warn("Could not restrict permissions on temp directory: {}", dir);
            }
            return dir;
        }
    }

    private void downloadFromS3(String key, Path destination) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(fileService.getBucketName())
                .key(key)
                .build();

        s3Client.getObject(request, ResponseTransformer.toFile(destination));
    }

    private int probeDuration(Path videoFile) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                ffprobePath,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoFile.toAbsolutePath().toString()
        );
        Process process = builder.start();
        try {
            String output;
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.readLine();
            }
            int exitCode = process.waitFor();
            if (exitCode != 0 || output == null) {
                throw new IllegalStateException("ffprobe failed to extract duration");
            }
            return (int) Math.round(Double.parseDouble(output.trim()));
        } finally {
            process.destroyForcibly();
        }
    }

    private void generateThumbnail(Path videoFile, Path thumbnailFile) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-i", videoFile.toAbsolutePath().toString(),
                "-ss", "00:00:01",
                "-vframes", "1",
                "-q:v", "2",
                thumbnailFile.toAbsolutePath().toString()
        );
        Process process = builder.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("ffmpeg failed to generate thumbnail");
            }
        } finally {
            process.destroyForcibly();
        }
    }

    private void uploadThumbnail(String thumbnailKey, Path thumbnailFile) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(fileService.getBucketName())
                .key(thumbnailKey)
                .contentType(THUMBNAIL_CONTENT_TYPE)
                .build();
        s3Client.putObject(request, RequestBody.fromFile(thumbnailFile));
    }

    private void markFailed(LessonEntity lesson, LessonEntity.ContentItem item) {
        if (lesson == null || item == null) {
            return;
        }
        item.setStatus(STATUS_FAILED);
        lessonRepository.save(lesson);
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}: {}", path, e.getMessage());
        }
    }
}