package com.example.course_service.service;

import com.example.course_service.dto.request.MultipartUploadPartETag;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.service.impl.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileServiceImplTest {

    private FileServiceImpl fileService;
    private S3Client s3Client;
    private S3Presigner presigner;
    private S3TransferManager transferManager;

    @BeforeEach
    void setup() {
        s3Client = mock(S3Client.class);
        presigner = mock(S3Presigner.class);
        transferManager = mock(S3TransferManager.class);
        fileService = new FileServiceImpl(s3Client, presigner, transferManager);

        ReflectionTestUtils.setField(fileService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(fileService, "expiryMinutes", 60);
        ReflectionTestUtils.setField(fileService, "cloudFrontDomain", "");
        ReflectionTestUtils.setField(fileService, "cloudFrontKeyPairId", "");
        ReflectionTestUtils.setField(fileService, "cloudFrontPrivateKeyPath", "");
    }

    // ================= UPLOAD URL =================

    @Test
    void generateUploadUrl_Success() throws Exception {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://test-bucket.s3.amazonaws.com/test-key").toURL());
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        String url = fileService.generateUploadUrl("test-key", "video/mp4");

        assertNotNull(url);
        assertTrue(url.contains("test-bucket"));
    }

    // ================= PRESIGNED URL (S3 fallback) =================

    @Test
    void generatePresignedUrl_Success() throws Exception {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://test-bucket.s3.amazonaws.com/test-key").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String url = fileService.generatePresignedUrl("test-key");

        assertNotNull(url);
        assertTrue(url.contains("test-bucket"));
    }

    // ================= DOWNLOAD URL (CloudFront fallback to S3) =================

    @Test
    void generateDownloadUrl_FallsBackToS3_WhenCloudFrontNotConfigured() throws Exception {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://test-bucket.s3.amazonaws.com/test-key").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String url = fileService.generateDownloadUrl("test-key");

        assertNotNull(url);
        assertTrue(url.contains("test-bucket"));
    }

    // ================= MULTIPART UPLOAD =================

    @Test
    void initiateMultipartUpload_ReturnsUploadId() {
        CreateMultipartUploadResponse mockResponse = mock(CreateMultipartUploadResponse.class);
        when(mockResponse.uploadId()).thenReturn("upload-123");
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(mockResponse);

        String uploadId = fileService.initiateMultipartUpload("videos/test.mp4");

        assertEquals("upload-123", uploadId);
    }

    @Test
    void generatePartUploadUrl_Success() throws Exception {
        PresignedUploadPartRequest presignedRequest = mock(PresignedUploadPartRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://s3.amazonaws.com/part-url").toURL());
        when(presigner.presignUploadPart(any(UploadPartPresignRequest.class))).thenReturn(presignedRequest);

        String url = fileService.generatePartUploadUrl("key", "upload-123", 1);

        assertNotNull(url);
        assertTrue(url.contains("part-url"));
    }

    @Test
    void completeMultipartUpload_CallsS3() {
        MultipartUploadPartETag part = new MultipartUploadPartETag();
        part.setPartNumber(1);
        part.setETag("etag-1");

        CompleteMultipartUploadResponse result =
                fileService.completeMultipartUpload("key", "upload-123", List.of(part));

        verify(s3Client, times(1)).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        assertNotNull(result);
        assertEquals("key", result.getVideoKey());
    }

    @Test
    void completeMultipartUpload_NullParts_ThrowsException() {
        assertThrows(Exception.class,
                () -> fileService.completeMultipartUpload("key", "upload-123", null));
    }

    @Test
    void abortMultipartUpload_CallsS3() {
        fileService.abortMultipartUpload("key", "upload-123");

        verify(s3Client, times(1)).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
    }

    // ================= DELETE =================

    @Test
    void deleteFile_CallsS3() {
        fileService.deleteFile("test-key");

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    // ================= FILE EXISTS =================

    @Test
    void fileExists_ReturnsTrue_WhenFileExists() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder().build());

        assertTrue(fileService.fileExists("test-key"));
    }

    @Test
    void fileExists_ReturnsFalse_WhenNotFound() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertFalse(fileService.fileExists("test-key"));
    }

    @Test
    void fileExists_ReturnsFalse_WhenGenericException() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.create("Connection error"));

        assertFalse(fileService.fileExists("test-key"));
    }

    @Test
    void deleteFile_S3Throws_LogsError() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.create("S3 error"));

        // Should not throw - error is caught and logged
        assertDoesNotThrow(() -> fileService.deleteFile("test-key"));
    }

    // ================= BUCKET NAME =================

    @Test
    void getBucketName_ReturnsConfiguredName() {
        assertEquals("test-bucket", fileService.getBucketName());
    }
}
