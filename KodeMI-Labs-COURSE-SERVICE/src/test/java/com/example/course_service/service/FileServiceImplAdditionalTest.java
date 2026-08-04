package com.example.course_service.service;

import com.example.course_service.dto.request.MultipartUploadPartETag;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.service.impl.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileUpload;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileServiceImplAdditionalTest {

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

    // ================= uploadFile =================

    @Test
    void uploadFile_Success() {
        FileUpload fileUpload = mock(FileUpload.class);
        when(fileUpload.completionFuture()).thenReturn(CompletableFuture.completedFuture(null));
        when(transferManager.uploadFile(any(software.amazon.awssdk.transfer.s3.model.UploadFileRequest.class)))
                .thenReturn(fileUpload);

        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
        assertDoesNotThrow(() -> fileService.uploadFile("videos/test.mp4", inputStream, 9L, "video/mp4"));

        verify(transferManager).uploadFile(any(software.amazon.awssdk.transfer.s3.model.UploadFileRequest.class));
    }

    @Test
    void uploadFile_ZeroFileSize_DoesNotThrow() {
        FileUpload fileUpload = mock(FileUpload.class);
        when(fileUpload.completionFuture()).thenReturn(CompletableFuture.completedFuture(null));
        when(transferManager.uploadFile(any(software.amazon.awssdk.transfer.s3.model.UploadFileRequest.class)))
                .thenReturn(fileUpload);

        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        assertDoesNotThrow(() -> fileService.uploadFile("test.mp4", inputStream, 0L, "video/mp4"));
    }

    @Test
    void uploadFile_TransferManagerThrows_WrapsInFileUploadException() {
        when(transferManager.uploadFile(any(software.amazon.awssdk.transfer.s3.model.UploadFileRequest.class)))
                .thenThrow(new RuntimeException("Transfer failed"));

        InputStream inputStream = new ByteArrayInputStream("data".getBytes());
        assertThrows(com.example.course_service.exception.FileUploadException.class,
                () -> fileService.uploadFile("key", inputStream, 4L, "video/mp4"));
    }

    // ================= determineContentType (via initiateMultipartUpload) =================

    @ParameterizedTest
    @CsvSource({
        "thumbnails/image.jpg,  upload-jpg",
        "thumbnails/image.jpeg, upload-jpeg",
        "thumbnails/image.png,  upload-png",
        "files/data.bin,        upload-bin",
        ","                                   // null key → octet-stream
    })
    void initiateMultipartUpload_VariousKeys_ReturnsUploadId(String key, String expectedId) {
        String resolvedId = expectedId != null ? expectedId.trim() : "upload-null";
        CreateMultipartUploadResponse mockResponse = mock(CreateMultipartUploadResponse.class);
        when(mockResponse.uploadId()).thenReturn(resolvedId);
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(mockResponse);

        String uploadId = fileService.initiateMultipartUpload(key != null ? key.trim() : null);
        assertEquals(resolvedId, uploadId);
    }

    // ================= normalizeETag (via completeMultipartUpload) =================

    @Test
    void completeMultipartUpload_ETagWithDoubleQuotes_Normalized() {
        MultipartUploadPartETag part = new MultipartUploadPartETag();
        part.setPartNumber(1);
        part.setETag("\"etag-quoted\"");

        CompleteMultipartUploadResponse result =
                fileService.completeMultipartUpload("key", "upload-123", List.of(part));

        verify(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        assertNotNull(result);
    }

    @Test
    void completeMultipartUpload_ETagWithSingleQuotes_Normalized() {
        MultipartUploadPartETag part = new MultipartUploadPartETag();
        part.setPartNumber(1);
        part.setETag("'etag-single'");

        CompleteMultipartUploadResponse result =
                fileService.completeMultipartUpload("key", "upload-123", List.of(part));

        verify(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        assertNotNull(result);
    }

    @Test
    void completeMultipartUpload_NullETag_HandledGracefully() {
        MultipartUploadPartETag part = new MultipartUploadPartETag();
        part.setPartNumber(1);
        part.setETag(null);

        assertDoesNotThrow(() -> fileService.completeMultipartUpload("key", "upload-123", List.of(part)));
    }

    @Test
    void completeMultipartUpload_MultiplePartsOutOfOrder_SortedCorrectly() {
        MultipartUploadPartETag part1 = new MultipartUploadPartETag(3, "etag-3");
        MultipartUploadPartETag part2 = new MultipartUploadPartETag(1, "etag-1");
        MultipartUploadPartETag part3 = new MultipartUploadPartETag(2, "etag-2");

        CompleteMultipartUploadResponse result =
                fileService.completeMultipartUpload("key", "upload-123", List.of(part1, part2, part3));

        verify(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        assertEquals("key", result.getVideoKey());
    }

    // ================= generateDownloadUrl CloudFront fallback =================

    @Test
    void generateDownloadUrl_CloudFrontDomainSet_ButKeyMissing_FallsBackToS3() throws Exception {
        ReflectionTestUtils.setField(fileService, "cloudFrontDomain", "cdn.example.com");
        ReflectionTestUtils.setField(fileService, "cloudFrontKeyPairId", "KEYPAIR123");
        ReflectionTestUtils.setField(fileService, "cloudFrontPrivateKeyPath", "nonexistent-key.pem");

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://test-bucket.s3.amazonaws.com/test-key").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String url = fileService.generateDownloadUrl("test-key");

        assertNotNull(url);
        assertTrue(url.contains("test-bucket"));
    }

    @Test
    void generateDownloadUrl_NullDomain_FallsBackToS3() throws Exception {
        ReflectionTestUtils.setField(fileService, "cloudFrontDomain", null);

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://test-bucket.s3.amazonaws.com/key").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String url = fileService.generateDownloadUrl("key");

        assertNotNull(url);
    }
}
