package com.example.course_service.service;

import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.service.impl.VideoProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VideoProcessingServiceTest {

    private VideoProcessingService videoProcessingService;
    private S3Client s3Client;
    private FileService fileService;
    private LessonRepository lessonRepository;
    private CourseService courseService;

    @BeforeEach
    void setup() {
        s3Client = mock(S3Client.class);
        fileService = mock(FileService.class);
        lessonRepository = mock(LessonRepository.class);
        courseService = mock(CourseService.class);
        videoProcessingService = new VideoProcessingService(s3Client, fileService, lessonRepository, courseService, "ffprobe", "ffmpeg");
    }

    @Test
    void processUploadedVideoAsync_NullLesson_ReturnsEarly() {
        videoProcessingService.processUploadedVideoAsync(null, new CourseEntity(), new LessonEntity.ContentItem());
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void processUploadedVideoAsync_NullCourse_ReturnsEarly() {
        videoProcessingService.processUploadedVideoAsync(new LessonEntity(), null, new LessonEntity.ContentItem());
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void processUploadedVideoAsync_NullItem_ReturnsEarly() {
        videoProcessingService.processUploadedVideoAsync(new LessonEntity(), new CourseEntity(), null);
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void processUploadedVideoAsync_NullItemKey_ReturnsEarly() {
        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setKey(null);
        videoProcessingService.processUploadedVideoAsync(new LessonEntity(), new CourseEntity(), item);
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void processUploadedVideoAsync_AllNull_ReturnsEarly() {
        videoProcessingService.processUploadedVideoAsync(null, null, null);
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void processUploadedVideoAsync_S3DownloadFails_MarksLessonFailed() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L1");
        lesson.setContentKey(new ArrayList<>());

        CourseEntity course = new CourseEntity();
        course.setCourseId("C1");

        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setKey("videos/test.mp4");

        when(fileService.getBucketName()).thenReturn("test-bucket");
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenThrow(new RuntimeException("S3 error"));

        videoProcessingService.processUploadedVideoAsync(lesson, course, item);

        verify(lessonRepository).save(lesson);
        assertEquals("FAILED", item.getStatus());
    }

    @Test
    void processUploadedVideoAsync_FfprobeNotFound_MarksFailed() {
        VideoProcessingService service = new VideoProcessingService(
                s3Client, fileService, lessonRepository, courseService, "non-existent-ffprobe-binary-12345", "ffmpeg");

        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L1");
        lesson.setModuleId("M1");
        CourseEntity course = new CourseEntity();
        course.setCourseId("C1");
        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setKey("videos/test.mp4");

        when(fileService.getBucketName()).thenReturn("test-bucket");

        service.processUploadedVideoAsync(lesson, course, item);

        verify(lessonRepository).save(lesson);
        assertEquals("FAILED", item.getStatus());
    }

    @Test
    void processUploadedVideoAsync_InterruptedException_MarksFailedAndInterrupts() {
        VideoProcessingService service = new VideoProcessingService(
                s3Client, fileService, lessonRepository, courseService, "java", "java");

        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L1");
        CourseEntity course = new CourseEntity();
        course.setCourseId("C1");
        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setKey("videos/test.mp4");

        when(fileService.getBucketName()).thenReturn("test-bucket");
        doAnswer(invocation -> {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Interrupted");
        }).when(s3Client).getObject(any(GetObjectRequest.class), any(ResponseTransformer.class));

        service.processUploadedVideoAsync(lesson, course, item);

        verify(lessonRepository).save(lesson);
        assertEquals("FAILED", item.getStatus());
    }
}