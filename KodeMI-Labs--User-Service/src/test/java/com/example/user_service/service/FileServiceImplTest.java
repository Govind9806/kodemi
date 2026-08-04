package com.example.user_service.service;

import com.example.user_service.exception.FileException;
import com.example.user_service.model.Learner;
import com.example.user_service.repository.LearnerRepository;
import com.example.user_service.service.impl.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class FileServiceImplTest {

    private S3Client          s3Client;
    private LearnerRepository learnerRepository;
    private S3Presigner       presigner;
    private FileServiceImpl   fileService;

    // 110MB — triggers multipart path
    private static final byte[] LARGE_FILE_BYTES = new byte[110 * 1024 * 1024];

    @BeforeEach
    void setup() throws Exception {
        s3Client          = mock(S3Client.class);
        learnerRepository = mock(LearnerRepository.class);
        presigner         = mock(S3Presigner.class);

        fileService = new FileServiceImpl(s3Client, learnerRepository, presigner);

        // Inject private @Value field
        Field bucketField = FileServiceImpl.class.getDeclaredField("bucketName");
        bucketField.setAccessible(true);
        bucketField.set(fileService, "test-bucket");
    }

    // ── uploadFile — validation ──────────────────────────────────────────

    @Test
    void uploadFile_nullFile_throwsFileException() {
        FileException ex = assertThrows(FileException.class, () ->
                fileService.uploadFile("usr001", null));
        assertEquals("File is empty", ex.getMessage());
    }

    @Test
    void uploadFile_emptyFile_throwsFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        FileException ex = assertThrows(FileException.class, () ->
                fileService.uploadFile("usr001", file));
        assertEquals("File is empty", ex.getMessage());
    }

    @Test
    void uploadFile_nullContentType_throwsFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "file.bin", null, new byte[]{1, 2, 3});

        FileException ex = assertThrows(FileException.class, () ->
                fileService.uploadFile("usr001", file));
        assertEquals("Invalid file type — Content-Type is missing", ex.getMessage());
    }

    @Test
    void uploadFile_exceedsMaxSize_throwsFileException() {
        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(2L * 1024 * 1024 * 1024);  // 2GB
        when(file.getContentType()).thenReturn("video/mp4");

        FileException ex = assertThrows(FileException.class, () ->
                fileService.uploadFile("usr001", file));
        assertEquals("File size exceeds 1 GB limit", ex.getMessage());
    }

    // ── uploadFile — single part (<100MB) ────────────────────────────────

    @Test
    void uploadFile_singlePart_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", new byte[]{1, 2, 3});

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String key = fileService.uploadFile("usr001", file);

        assertNotNull(key);
        assertTrue(key.startsWith("usr001/"));
        assertTrue(key.endsWith(".mp4"));
        verify(s3Client, times(1))
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_singlePart_noExtension_usesBin() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noextension", "text/plain", new byte[]{1, 2, 3});

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String key = fileService.uploadFile("usr001", file);

        assertTrue(key.endsWith(".bin"));
    }

    // ── uploadFile — multipart (>100MB) ──────────────────────────────────

    @Test
    void uploadFile_multipart_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.mp4", "video/mp4", LARGE_FILE_BYTES);

        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder()
                        .uploadId("upload-id-001").build());

        when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                .thenReturn(UploadPartResponse.builder().eTag("etag-1").build());

        when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenReturn(CompleteMultipartUploadResponse.builder().build());

        String key = fileService.uploadFile("usr001", file);

        assertNotNull(key);
        assertTrue(key.startsWith("usr001/"));
        verify(s3Client, times(1))
                .createMultipartUpload(any(CreateMultipartUploadRequest.class));
        verify(s3Client, atLeastOnce())
                .uploadPart(any(UploadPartRequest.class), any(RequestBody.class));
        verify(s3Client, times(1))
                .completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    }

    @Test
    void uploadFile_multipart_uploadPartFails_abortsAndThrows() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.mp4", "video/mp4", LARGE_FILE_BYTES);

        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder()
                        .uploadId("upload-id-001").build());

        when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Part upload failed").build());

        when(s3Client.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
                .thenReturn(AbortMultipartUploadResponse.builder().build());

        assertThrows(FileException.class, () ->
                fileService.uploadFile("usr001", file));

        // Must abort to avoid S3 storage charges
        verify(s3Client, times(1))
                .abortMultipartUpload(any(AbortMultipartUploadRequest.class));
    }

    @Test
    void uploadFile_multipart_createUploadFails_throwsFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.mp4", "video/mp4", LARGE_FILE_BYTES);

        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenThrow(S3Exception.builder().message("Init failed").build());

        assertThrows(FileException.class, () ->
                fileService.uploadFile("usr001", file));
    }

    // ── fileExists ───────────────────────────────────────────────────────

    @Test
    void fileExists_validKey_returnsTrue() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertTrue(fileService.fileExists("file.txt"));
    }

    @Test
    void fileExists_noSuchKey_returnsFalse() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertFalse(fileService.fileExists("file.txt"));
    }

    @Test
    void fileExists_nullKey_returnsFalse() {
        assertFalse(fileService.fileExists(null));
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void fileExists_blankKey_returnsFalse() {
        assertFalse(fileService.fileExists("   "));
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void fileExists_s3Exception_throwsFileException() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        FileException ex = assertThrows(FileException.class, () ->
                fileService.fileExists("file.txt"));
        assertTrue(ex.getMessage().contains("Error checking file existence"));
    }

    // ── deleteFile ───────────────────────────────────────────────────────

    @Test
    void deleteFile_validKey_deletesSuccessfully() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        assertDoesNotThrow(() -> fileService.deleteFile("file.txt"));
        verify(s3Client, times(1))
                .deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteFile_nullKey_skipsS3Call() {
        assertDoesNotThrow(() -> fileService.deleteFile(null));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteFile_blankKey_skipsS3Call() {
        assertDoesNotThrow(() -> fileService.deleteFile("   "));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteFile_s3Exception_throwsFileException() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        FileException ex = assertThrows(FileException.class, () ->
                fileService.deleteFile("file.txt"));
        assertTrue(ex.getMessage().contains("Failed to delete file"));
    }

    // ── getObjectAsBytes ─────────────────────────────────────────────────

    @Test
    void getObjectAsBytes_success() {
        Learner learner = new Learner();
        learner.setProfilePictureKey("file.txt");
        when(learnerRepository.findByUserId("usr001")).thenReturn(learner);

        byte[] data = "Hello".getBytes();
        ResponseInputStream<GetObjectResponse> stream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(data)
        );
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(stream);

        byte[] result = fileService.getObjectAsBytes("usr001");
        assertArrayEquals(data, result);
    }

    @Test
    void getObjectAsBytes_learnerNotFound_throwsFileException() {
        when(learnerRepository.findByUserId("usr001")).thenReturn(null);

        FileException ex = assertThrows(FileException.class, () ->
                fileService.getObjectAsBytes("usr001"));
        assertEquals("No file found for user", ex.getMessage());
    }

    @Test
    void getObjectAsBytes_noProfileKey_throwsFileException() {
        Learner learner = new Learner();   // profilePictureKey is null
        when(learnerRepository.findByUserId("usr001")).thenReturn(learner);

        FileException ex = assertThrows(FileException.class, () ->
                fileService.getObjectAsBytes("usr001"));
        assertEquals("No file found for user", ex.getMessage());
    }

    @Test
    void getObjectAsBytes_s3Throws_throwsFileException() {
        Learner learner = new Learner();
        learner.setProfilePictureKey("usr001/profile.jpg");
        when(learnerRepository.findByUserId("usr001")).thenReturn(learner);
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        FileException ex = assertThrows(FileException.class, () ->
                fileService.getObjectAsBytes("usr001"));
        assertTrue(ex.getMessage().contains("Error retrieving file from S3"));
    }

    // ── generatePresignedUrl ─────────────────────────────────────────────

    @Test
    void generatePresignedUrl_validKey_returnsUrl() throws MalformedURLException {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url())
                .thenReturn(URI.create("https://s3.amazonaws.com/test-bucket/file.mp4").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presigned);

        String url = fileService.generatePresignedUrl("file.mp4");

        assertEquals("https://s3.amazonaws.com/test-bucket/file.mp4", url);
    }

    @Test
    void generatePresignedUrl_nullKey_returnsNull() {
        assertNull(fileService.generatePresignedUrl(null));
    }

    @Test
    void generatePresignedUrl_blankKey_returnsNull() {
        assertNull(fileService.generatePresignedUrl("   "));
    }

    @Test
    void generatePresignedUrl_httpUrl_returnsOriginalUrl() {
        String url = fileService.generatePresignedUrl("https://example.com/image.jpg");
        assertEquals("https://example.com/image.jpg", url);
    }

    @Test
    void generatePresignedUrl_cloudFrontConfigured_returnsSignedUrl() {
        org.springframework.test.util.ReflectionTestUtils.setField(fileService, "cloudFrontDomain", "d1agigmrs4xhn5.cloudfront.net");
        org.springframework.test.util.ReflectionTestUtils.setField(fileService, "cloudFrontKeyPairId", "K1XWGJYU4X996D");
        org.springframework.test.util.ReflectionTestUtils.setField(fileService, "cloudFrontPrivateKeyPath", "cloudfront_private_key.pem");

        String url = fileService.generatePresignedUrl("profile.jpg");
        assertNotNull(url);
        assertTrue(url.contains("d1agigmrs4xhn5.cloudfront.net/profile.jpg"));
        assertTrue(url.contains("Key-Pair-Id=K1XWGJYU4X996D"));
    }

    @Test
    void generatePresignedUrl_cloudFrontInvalidKey_fallsBackToS3() throws MalformedURLException {
        org.springframework.test.util.ReflectionTestUtils.setField(fileService, "cloudFrontDomain", "d1agigmrs4xhn5.cloudfront.net");
        org.springframework.test.util.ReflectionTestUtils.setField(fileService, "cloudFrontKeyPairId", "K1XWGJYU4X996D");
        org.springframework.test.util.ReflectionTestUtils.setField(fileService, "cloudFrontPrivateKeyPath", "non_existent_key.pem");

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.amazonaws.com/test-bucket/profile.jpg").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = fileService.generatePresignedUrl("profile.jpg");
        assertEquals("https://s3.amazonaws.com/test-bucket/profile.jpg", url);
    }
}