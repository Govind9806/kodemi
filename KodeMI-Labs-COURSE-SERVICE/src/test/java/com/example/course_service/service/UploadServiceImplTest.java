package com.example.course_service.service;

import com.example.course_service.dto.request.MultipartUploadPartETag;
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
import com.example.course_service.service.impl.UploadServiceImpl;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadServiceImplTest {

    private UploadServiceImpl uploadService;
    private FileService fileService;
    private UploadRepository uploadRepository;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setup() {
        fileService = mock(FileService.class);
        uploadRepository = mock(UploadRepository.class);
        jwtUtil = mock(JwtUtil.class);
        uploadService = new UploadServiceImpl(fileService, uploadRepository, jwtUtil);
    }

    // ==========================================
    // initiateMultipartUpload
    // ==========================================

    @Test
    void initiateMultipartUpload_WithExtension_Success() {
        UploadInitRequest request = new UploadInitRequest();
        request.setFileName("video.mp4");
        request.setFileType(FileType.DEMO_VIDEO);

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");
        when(fileService.initiateMultipartUpload(anyString())).thenReturn("upload123");

        UploadInitResponse response = uploadService.initiateMultipartUpload("token123", request);

        assertNotNull(response);
        assertEquals("upload123", response.getUploadId());
        assertTrue(response.getFileKey().contains("preview/demo-videos/user123/"));
        assertTrue(response.getFileKey().endsWith(".mp4"));
        verify(uploadRepository, times(1)).save(any(UploadEntity.class));
    }

    @Test
    void initiateMultipartUpload_WithoutExtension_UsesDefault() {
        UploadInitRequest request = new UploadInitRequest();
        request.setFileName("video");
        request.setFileType(FileType.THUMBNAIL);

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");
        when(fileService.initiateMultipartUpload(anyString())).thenReturn("upload123");

        UploadInitResponse response = uploadService.initiateMultipartUpload("token123", request);

        assertNotNull(response);
        assertEquals("upload123", response.getUploadId());
        assertTrue(response.getFileKey().contains("preview/thumbnails/user123/"));
        assertTrue(response.getFileKey().endsWith(".png"));
    }

    // ==========================================
    // generatePresignedUrl
    // ==========================================

    @Test
    void generatePresignedUrl_InitiatedStatus_Success() {
        UploadPresignedUrlRequest request = new UploadPresignedUrlRequest();
        request.setUploadId("upload123");
        request.setPartNumber(1);

        UploadEntity entity = new UploadEntity();
        entity.setUploadId("upload123");
        entity.setOwnerId("user123");
        entity.setFileKey("key123");
        entity.setUploadStatus("INITIATED");

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");
        when(uploadRepository.findById("upload123")).thenReturn(entity);
        when(fileService.generatePartUploadUrl("key123", "upload123", 1)).thenReturn("presigned-url");

        String url = uploadService.generatePresignedUrl("token123", request);

        assertEquals("presigned-url", url);
        assertEquals("UPLOADING", entity.getUploadStatus());
        verify(uploadRepository, times(1)).save(entity);
    }

    @Test
    void generatePresignedUrl_UploadingStatus_Success() {
        UploadPresignedUrlRequest request = new UploadPresignedUrlRequest();
        request.setUploadId("upload123");
        request.setPartNumber(2);

        UploadEntity entity = new UploadEntity();
        entity.setUploadId("upload123");
        entity.setOwnerId("user123");
        entity.setFileKey("key123");
        entity.setUploadStatus("UPLOADING");

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");
        when(uploadRepository.findById("upload123")).thenReturn(entity);
        when(fileService.generatePartUploadUrl("key123", "upload123", 2)).thenReturn("presigned-url-2");

        String url = uploadService.generatePresignedUrl("token123", request);

        assertEquals("presigned-url-2", url);
        // Should not save since status was already UPLOADING
        verify(uploadRepository, never()).save(entity);
    }

    @Test
    void generatePresignedUrl_NotFound_ThrowsException() {
        UploadPresignedUrlRequest request = new UploadPresignedUrlRequest();
        request.setUploadId("non-existent");

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");
        when(uploadRepository.findById("non-existent")).thenReturn(null);

        try {
            uploadService.generatePresignedUrl("token123", request);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Upload ID not found: non-existent", e.getMessage());
        }
    }

    @Test
    void generatePresignedUrl_Forbidden_ThrowsException() {
        UploadPresignedUrlRequest request = new UploadPresignedUrlRequest();
        request.setUploadId("upload123");

        UploadEntity entity = new UploadEntity();
        entity.setUploadId("upload123");
        entity.setOwnerId("user456"); // different owner

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");
        when(uploadRepository.findById("upload123")).thenReturn(entity);

        try {
            uploadService.generatePresignedUrl("token123", request);
            fail("Expected ForbiddenException");
        } catch (ForbiddenException e) {
            assertEquals("You are not authorized to access this upload.", e.getMessage());
        }
    }

    @Test
    void generatePresignedUrl_InvalidStatus_ThrowsException() {
        UploadPresignedUrlRequest request = new UploadPresignedUrlRequest();
        request.setUploadId("upload123");

        UploadEntity entity = new UploadEntity();
        entity.setUploadId("upload123");
        entity.setOwnerId("user123");
        entity.setUploadStatus("COMPLETED"); // invalid status for generating presigned url

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");
        when(uploadRepository.findById("upload123")).thenReturn(entity);

        try {
            uploadService.generatePresignedUrl("token123", request);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Cannot generate URL for upload in status: COMPLETED", e.getMessage());
        }
    }

    // ==========================================
    // completeMultipartUpload
    // ==========================================

    @Test
    void completeMultipartUpload_Success() {
        UploadCompleteRequest request = new UploadCompleteRequest();
        request.setUploadId("upload123");
        List<MultipartUploadPartETag> parts = new ArrayList<>();
        request.setParts(parts);

        UploadEntity entity = new UploadEntity();
        entity.setUploadId("upload123");
        entity.setOwnerId("user123");
        entity.setFileKey("key123");
        entity.setUploadStatus("UPLOADING");

        CompleteMultipartUploadResponse completeResponse = new CompleteMultipartUploadResponse("upload123", "key123", "Upload completed successfully");

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");
        when(uploadRepository.findById("upload123")).thenReturn(entity);
        when(fileService.completeMultipartUpload("key123", "upload123", parts)).thenReturn(completeResponse);

        CompleteMultipartUploadResponse result = uploadService.completeMultipartUpload("token123", request);

        assertNotNull(result);
        assertEquals("key123", result.getVideoKey());
        assertEquals("COMPLETED", entity.getUploadStatus());
        verify(uploadRepository, times(1)).save(entity);
    }

    // ==========================================
    // abortMultipartUpload
    // ==========================================

    @Test
    void abortMultipartUpload_Success() {
        UploadAbortRequest request = new UploadAbortRequest();
        request.setUploadId("upload123");

        UploadEntity entity = new UploadEntity();
        entity.setUploadId("upload123");
        entity.setOwnerId("user123");
        entity.setFileKey("key123");
        entity.setUploadStatus("UPLOADING");

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");
        when(uploadRepository.findById("upload123")).thenReturn(entity);

        String result = uploadService.abortMultipartUpload("token123", request);

        assertEquals("Upload aborted", result);
        assertEquals("ABORTED", entity.getUploadStatus());
        verify(fileService, times(1)).abortMultipartUpload("key123", "upload123");
        verify(uploadRepository, times(1)).save(entity);
    }

    // ==========================================
    // validateAndConsumeUpload
    // ==========================================

    @Test
    void validateAndConsumeUpload_Success() {
        UploadEntity entity = new UploadEntity();
        entity.setFileKey("key123");
        entity.setOwnerId("user123");
        entity.setUploadStatus("COMPLETED");
        entity.setIsUsed(false);

        when(uploadRepository.findAll()).thenReturn(Collections.singletonList(entity));

        uploadService.validateAndConsumeUpload("key123", "user123", "course789");

        assertTrue(entity.getIsUsed());
        assertEquals("USED", entity.getUploadStatus());
        assertEquals("course789", entity.getLinkedEntityId());
        verify(uploadRepository, times(1)).save(entity);
    }

    @Test
    void validateAndConsumeUpload_NotFound_ThrowsException() {
        when(uploadRepository.findAll()).thenReturn(new ArrayList<>());

        try {
            uploadService.validateAndConsumeUpload("key123", "user123", "course789");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Upload not found for key: key123", e.getMessage());
        }
    }

    @Test
    void validateAndConsumeUpload_Forbidden_ThrowsException() {
        UploadEntity entity = new UploadEntity();
        entity.setFileKey("key123");
        entity.setOwnerId("user456"); // different owner

        when(uploadRepository.findAll()).thenReturn(Collections.singletonList(entity));

        try {
            uploadService.validateAndConsumeUpload("key123", "user123", "course789");
            fail("Expected ForbiddenException");
        } catch (ForbiddenException e) {
            assertEquals("You do not own this uploaded file.", e.getMessage());
        }
    }

    @Test
    void validateAndConsumeUpload_NotCompleted_ThrowsException() {
        UploadEntity entity = new UploadEntity();
        entity.setFileKey("key123");
        entity.setOwnerId("user123");
        entity.setUploadStatus("UPLOADING"); // not completed

        when(uploadRepository.findAll()).thenReturn(Collections.singletonList(entity));

        try {
            uploadService.validateAndConsumeUpload("key123", "user123", "course789");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Upload must be in COMPLETED status.", e.getMessage());
        }
    }

    @Test
    void validateAndConsumeUpload_AlreadyUsed_ThrowsException() {
        UploadEntity entity = new UploadEntity();
        entity.setFileKey("key123");
        entity.setOwnerId("user123");
        entity.setUploadStatus("COMPLETED");
        entity.setIsUsed(true); // already used

        when(uploadRepository.findAll()).thenReturn(Collections.singletonList(entity));

        try {
            uploadService.validateAndConsumeUpload("key123", "user123", "course789");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Upload has already been used.", e.getMessage());
        }
    }

    // ==========================================
    // uploadFileDirectly
    // ==========================================

    @Test
    void uploadFileDirectly_NullFile_ReturnsNull() {
        String result = uploadService.uploadFileDirectly("token123", null, FileType.THUMBNAIL, "linkedId");
        assertNull(result);
    }

    @Test
    void uploadFileDirectly_EmptyFile_ReturnsNull() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        String result = uploadService.uploadFileDirectly("token123", file, FileType.THUMBNAIL, "linkedId");
        assertNull(result);
    }

    @Test
    void uploadFileDirectly_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("image.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(100L);
        ByteArrayInputStream bais = new ByteArrayInputStream("test-data".getBytes());
        when(file.getInputStream()).thenReturn(bais);

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");

        String fileKey = uploadService.uploadFileDirectly("token123", file, FileType.THUMBNAIL, "linkedId");

        assertNotNull(fileKey);
        assertTrue(fileKey.contains("preview/thumbnails/user123/"));
        assertTrue(fileKey.endsWith(".png"));
        verify(fileService, times(1)).uploadFile(fileKey, bais, 100L, "image/png");
        verify(uploadRepository, times(1)).save(any(UploadEntity.class));
    }

    @Test
    void uploadFileDirectly_Success_DefaultContentTypeAndExtension() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("image"); // no extension
        when(file.getContentType()).thenReturn(null); // no content type
        when(file.getSize()).thenReturn(100L);
        ByteArrayInputStream bais = new ByteArrayInputStream("test-data".getBytes());
        when(file.getInputStream()).thenReturn(bais);

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");

        String fileKey = uploadService.uploadFileDirectly("token123", file, FileType.CERTIFICATE, "linkedId");

        assertNotNull(fileKey);
        assertTrue(fileKey.contains("resources/certificates/user123/"));
        assertTrue(fileKey.endsWith(".bin"));
        verify(fileService, times(1)).uploadFile(fileKey, bais, 100L, "application/octet-stream");
    }

    @Test
    void uploadFileDirectly_IOException_ThrowsFileUploadException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("image.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(100L);
        when(file.getInputStream()).thenThrow(new IOException("Read failed"));

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");

        try {
            uploadService.uploadFileDirectly("token123", file, FileType.THUMBNAIL, "linkedId");
            fail("Expected FileUploadException");
        } catch (FileUploadException e) {
            assertEquals("Failed to read file", e.getMessage());
            assertEquals("image.png", e.getFileName());
        }
    }

    // ==========================================
    // resolveFolder edge cases
    // ==========================================

    @Test
    void resolveFolder_AllFileTypes() {
        // We will trigger all fileType cases using initiateMultipartUpload to check key generation prefixes
        when(jwtUtil.extractUserId("token123")).thenReturn("user123");

        FileType[] types = {
                FileType.DEMO_VIDEO,
                FileType.THUMBNAIL,
                FileType.COURSE_RESOURCE,
                FileType.CERTIFICATE,
                FileType.PROFILE_VIDEO,
                FileType.LESSON_VIDEO
        };

        String[] expectedFolders = {
                "preview/demo-videos",
                "preview/thumbnails",
                "resources/course-resources",
                "resources/certificates",
                "preview/profile-videos",
                "course/lesson-videos"
        };

        for (int i = 0; i < types.length; i++) {
            UploadInitRequest request = new UploadInitRequest();
            request.setFileName("file.bin");
            request.setFileType(types[i]);

            UploadInitResponse response = uploadService.initiateMultipartUpload("token123", request);
            assertTrue(response.getFileKey().startsWith(expectedFolders[i] + "/user123/"));
        }
    }

    @Test
    void resolveFolder_NullFileType_ThrowsException() {
        UploadInitRequest request = new UploadInitRequest();
        request.setFileName("file.bin");
        request.setFileType(null);

        when(jwtUtil.extractUserId("token123")).thenReturn("user123");

        try {
            uploadService.initiateMultipartUpload("token123", request);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("File type is required", e.getMessage());
        }
    }
}