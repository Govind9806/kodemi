package com.example.course_service.service.impl;

import com.example.course_service.dto.request.UploadAbortRequest;
import com.example.course_service.dto.request.UploadCompleteRequest;
import com.example.course_service.dto.request.UploadInitRequest;
import com.example.course_service.dto.request.UploadPresignedUrlRequest;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.dto.response.UploadInitResponse;
import com.example.course_service.exception.FileUploadException;
import com.example.course_service.exception.ForbiddenException;
import com.example.course_service.model.FileType;
import com.example.course_service.model.UploadEntity;
import com.example.course_service.repository.UploadRepository;
import com.example.course_service.service.FileService;
import com.example.course_service.service.UploadService;
import com.example.course_service.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadServiceImpl implements UploadService {

    private static final String STATUS_INITIATED = "INITIATED";
    private static final String STATUS_UPLOADING = "UPLOADING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_ABORTED = "ABORTED";
    private static final String STATUS_USED = "USED";

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileService fileService;
    private final UploadRepository uploadRepository;
    private final JwtUtil jwtUtil;

    @Override
    public UploadInitResponse initiateMultipartUpload(String token, UploadInitRequest request) {
        String ownerId = jwtUtil.extractUserId(token);
        String extension = resolveExtension(request.getFileName(), request.getFileType());

        String fileKey = generateS3Key(request.getFileType(), ownerId, extension);
        String uploadId = fileService.initiateMultipartUpload(fileKey);

        Date now = new Date();
        UploadEntity entity = new UploadEntity();
        entity.setUploadId(uploadId);
        entity.setOwnerId(ownerId);
        entity.setFileKey(fileKey);
        entity.setFileType(request.getFileType().name());
        entity.setUploadStatus(STATUS_INITIATED);
        entity.setIsUsed(false);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        uploadRepository.save(entity);

        log.info("[UPLOAD INIT] uploadId: {}, fileKey: {}", uploadId, fileKey);
        return new UploadInitResponse(uploadId, fileKey, "Upload initiated");
    }

    @Override
    public String generatePresignedUrl(String token, UploadPresignedUrlRequest request) {
        String ownerId = jwtUtil.extractUserId(token);
        UploadEntity entity = getUploadEntityOrThrow(request.getUploadId(), ownerId);

        String status = entity.getUploadStatus();
        if (!STATUS_INITIATED.equals(status) && !STATUS_UPLOADING.equals(status)) {
            throw new IllegalStateException("Cannot generate URL for upload in status: " + status);
        }

        if (STATUS_INITIATED.equals(status)) {
            entity.setUploadStatus(STATUS_UPLOADING);
            entity.setUpdatedAt(new Date());
            uploadRepository.save(entity);
        }

        return fileService.generatePartUploadUrl(entity.getFileKey(), request.getUploadId(), request.getPartNumber());
    }

    @Override
    public CompleteMultipartUploadResponse completeMultipartUpload(String token, UploadCompleteRequest request) {
        String ownerId = jwtUtil.extractUserId(token);
        UploadEntity entity = getUploadEntityOrThrow(request.getUploadId(), ownerId);

        CompleteMultipartUploadResponse response =
                fileService.completeMultipartUpload(entity.getFileKey(), request.getUploadId(), request.getParts());

        entity.setUploadStatus(STATUS_COMPLETED);
        entity.setUpdatedAt(new Date());
        uploadRepository.save(entity);

        log.info("[UPLOAD COMPLETE] videoKey/fileKey: {}", response.getVideoKey());
        return response;
    }

    @Override
    public String abortMultipartUpload(String token, UploadAbortRequest request) {
        String ownerId = jwtUtil.extractUserId(token);
        UploadEntity entity = getUploadEntityOrThrow(request.getUploadId(), ownerId);

        fileService.abortMultipartUpload(entity.getFileKey(), request.getUploadId());

        entity.setUploadStatus(STATUS_ABORTED);
        entity.setUpdatedAt(new Date());
        uploadRepository.save(entity);

        return "Upload aborted";
    }

    @Override
    public void validateAndConsumeUpload(String fileKey, String ownerId, String linkedEntityId) {
        UploadEntity entity = uploadRepository.findAll().stream()
                .filter(u -> fileKey.equals(u.getFileKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Upload not found for key: " + fileKey));

        if (!Objects.equals(entity.getOwnerId(), ownerId)) {
            throw new ForbiddenException("You do not own this uploaded file.");
        }

        if (!STATUS_COMPLETED.equals(entity.getUploadStatus())) {
            throw new IllegalArgumentException("Upload must be in COMPLETED status.");
        }

        if (Boolean.TRUE.equals(entity.getIsUsed())) {
            throw new IllegalArgumentException("Upload has already been used.");
        }

        entity.setIsUsed(true);
        entity.setUploadStatus(STATUS_USED);
        entity.setLinkedEntityId(linkedEntityId);
        entity.setUpdatedAt(new Date());
        uploadRepository.save(entity);

        log.info("[UPLOAD CONSUMED] fileKey: {}, linkedTo: {}", fileKey, linkedEntityId);
    }

    @Override
    public String uploadFileDirectly(String token, MultipartFile file, FileType fileType, String linkedEntityId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String ownerId = jwtUtil.extractUserId(token);
        String extension = resolveExtension(file.getOriginalFilename(), fileType);
        String fileKey = generateS3Key(fileType, ownerId, extension);
        String contentType = file.getContentType() != null ? file.getContentType() : DEFAULT_CONTENT_TYPE;

        try {
            fileService.uploadFile(fileKey, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new FileUploadException("Failed to read file", file.getOriginalFilename(), fileKey, 0);
        }

        Date now = new Date();
        UploadEntity entity = new UploadEntity();
        entity.setUploadId("direct-" + UUID.randomUUID());
        entity.setOwnerId(ownerId);
        entity.setFileKey(fileKey);
        entity.setFileType(fileType.name());
        entity.setUploadStatus(STATUS_USED);
        entity.setIsUsed(true);
        entity.setLinkedEntityId(linkedEntityId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        uploadRepository.save(entity);

        return fileKey;
    }

    private UploadEntity getUploadEntityOrThrow(String uploadId, String ownerId) {
        UploadEntity entity = uploadRepository.findById(uploadId);
        if (entity == null) {
            throw new IllegalArgumentException("Upload ID not found: " + uploadId);
        }
        if (!Objects.equals(entity.getOwnerId(), ownerId)) {
            throw new ForbiddenException("You are not authorized to access this upload.");
        }
        return entity;
    }

    private String resolveExtension(String fileName, FileType fileType) {
        String extension = getExtension(fileName);
        return extension.isEmpty() ? getDefaultExtension(fileType) : extension;
    }

    private String generateS3Key(FileType fileType, String ownerId, String extension) {
        String uuid = UUID.randomUUID().toString();
        String folder = resolveFolder(fileType);

        return folder + "/" + ownerId + "/" + uuid + "." + extension;
    }

    private String resolveFolder(FileType fileType) {
        if (fileType == null) {
            throw new IllegalArgumentException("File type is required");
        }

        switch (fileType) {
            case DEMO_VIDEO:
                return "preview/demo-videos";
            case THUMBNAIL:
                return "preview/thumbnails";
            case COURSE_RESOURCE:
                return "resources/course-resources";
            case CERTIFICATE:
                return "resources/certificates";
            case PROFILE_VIDEO:
                return "preview/profile-videos";
            case LESSON_VIDEO:
                return "course/lesson-videos";
            default:
                throw new IllegalArgumentException("Unknown file type: " + fileType);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot == -1 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    private String getDefaultExtension(FileType type) {
        if (type == FileType.DEMO_VIDEO || type == FileType.PROFILE_VIDEO) {
            return "mp4";
        }
        if (type == FileType.THUMBNAIL) {
            return "png";
        }
        return "bin";
    }
}