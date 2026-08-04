package com.example.course_service.controller;

import com.example.course_service.dto.request.UploadAbortRequest;
import com.example.course_service.dto.request.UploadCompleteRequest;
import com.example.course_service.dto.request.UploadInitRequest;
import com.example.course_service.dto.request.UploadPresignedUrlRequest;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.dto.response.UploadInitResponse;
import com.example.course_service.model.FileType;
import com.example.course_service.service.UploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadControllerTest {

    private UploadService uploadService;
    private UploadController uploadController;

    @BeforeEach
    void setup() {
        uploadService = mock(UploadService.class);
        uploadController = new UploadController(uploadService);
    }

    @Test
    void initiateMultipartUpload_ReturnsResponse() {
        UploadInitRequest request = new UploadInitRequest();
        request.setFileName("video.mp4");
        request.setFileType(FileType.LESSON_VIDEO);

        UploadInitResponse expected = new UploadInitResponse("up1", "key1", "Init OK");
        when(uploadService.initiateMultipartUpload(eq("Bearer token"), any(UploadInitRequest.class))).thenReturn(expected);

        ResponseEntity<UploadInitResponse> response = uploadController.initiateMultipartUpload("Bearer token", request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("up1", response.getBody().getUploadId());
    }

    @Test
    void getPresignedUrl_ReturnsUrlMap() {
        UploadPresignedUrlRequest request = new UploadPresignedUrlRequest();
        request.setUploadId("up1");
        request.setPartNumber(1);

        when(uploadService.generatePresignedUrl(eq("Bearer token"), any(UploadPresignedUrlRequest.class))).thenReturn("https://s3.url");

        ResponseEntity<Map<String, String>> response = uploadController.getPresignedUrl("Bearer token", request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://s3.url", response.getBody().get("presignedUrl"));
    }

    @Test
    void completeMultipartUpload_ReturnsResponse() {
        UploadCompleteRequest request = new UploadCompleteRequest();
        request.setUploadId("up1");

        CompleteMultipartUploadResponse expected = new CompleteMultipartUploadResponse("up1", "key1", "Done");
        when(uploadService.completeMultipartUpload(eq("Bearer token"), any(UploadCompleteRequest.class))).thenReturn(expected);

        ResponseEntity<CompleteMultipartUploadResponse> response = uploadController.completeMultipartUpload("Bearer token", request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("up1", response.getBody().getUploadId());
    }

    @Test
    void abortMultipartUpload_ReturnsMessageMap() {
        UploadAbortRequest request = new UploadAbortRequest();
        request.setUploadId("up1");

        when(uploadService.abortMultipartUpload(eq("Bearer token"), any(UploadAbortRequest.class))).thenReturn("Aborted");

        ResponseEntity<Map<String, String>> response = uploadController.abortMultipartUpload("Bearer token", request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Aborted", response.getBody().get("message"));
    }
}
