package com.example.course_service.controller;

import com.example.course_service.dto.request.AbortMultipartUploadRequestDTO;
import com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO;
import com.example.course_service.dto.request.MultipartUploadInitRequest;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.dto.response.MultipartUploadInitResponse;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.service.CourseService;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseControllerAdditionalTest {

    private CourseController courseController;
    private CourseService courseService;
    private JwtUtil jwtUtil;
    private EnrollmentClient enrollmentClient;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setup() {
        courseService = mock(CourseService.class);
        jwtUtil = mock(JwtUtil.class);
        enrollmentClient = mock(EnrollmentClient.class);
        courseController = new CourseController(courseService, jwtUtil, enrollmentClient);
    }

    // ================= createCourse =================

    @Test
    void createCourse_CallsService() {
        com.example.course_service.model.CourseEntity request = new com.example.course_service.model.CourseEntity();
        org.springframework.web.multipart.MultipartFile thumbnail = mock(org.springframework.web.multipart.MultipartFile.class);
        org.springframework.web.multipart.MultipartFile demoVideo = mock(org.springframework.web.multipart.MultipartFile.class);

        when(courseService.createCourse(TOKEN, request, null, thumbnail))
                .thenReturn(Map.of("message", "Course Created Successfully", "courseId", "C101"));

        ResponseEntity<Map<String, Object>> response =
                courseController.createCourse(TOKEN, request, null, thumbnail);

        assertEquals("Course Created Successfully", response.getBody().get("message"));
        verify(courseService).createCourse(TOKEN, request, null, thumbnail);
    }

    // ================= createLiveCourse =================

    @Test
    void createLiveCourse_CallsService() {
        com.example.course_service.model.CourseEntity request = new com.example.course_service.model.CourseEntity();
        org.springframework.web.multipart.MultipartFile thumbnail = mock(org.springframework.web.multipart.MultipartFile.class);
        org.springframework.web.multipart.MultipartFile demoVideo = mock(org.springframework.web.multipart.MultipartFile.class);

        when(courseService.createLiveCourse(TOKEN, request, null, thumbnail))
                .thenReturn(Map.of("message", "Live Course Created Successfully", "courseId", "C101"));

        ResponseEntity<Map<String, Object>> response =
                courseController.createLiveCourse(TOKEN, request, null, thumbnail);

        assertEquals("Live Course Created Successfully", response.getBody().get("message"));
        verify(courseService).createLiveCourse(TOKEN, request, null, thumbnail);
    }

    // ================= updateCourse =================

    @Test
    void updateCourse_CallsService() {
        com.example.course_service.model.CourseEntity request = new com.example.course_service.model.CourseEntity();
        org.springframework.web.multipart.MultipartFile thumbnail = mock(org.springframework.web.multipart.MultipartFile.class);

        when(courseService.updateCourse(TOKEN, "C101", request, thumbnail))
                .thenReturn("Course Updated Successfully.");

        ResponseEntity<Map<String, String>> response =
                courseController.updateCourse(TOKEN, "C101", request, thumbnail);

        assertEquals("Course Updated Successfully.", response.getBody().get("message"));
        verify(courseService).updateCourse(TOKEN, "C101", request, thumbnail);
    }

    // ================= initiateDemoVideoMultipartUpload =================

    @Test
    void initiateDemoVideoMultipartUpload_ReturnsResponse() {
        MultipartUploadInitRequest request = new MultipartUploadInitRequest();
        request.setFileName("demo.mp4");
        MultipartUploadInitResponse mockResponse = new MultipartUploadInitResponse("upload-1", "key/demo.mp4", "initiated");

        when(courseService.initiateDemoVideoMultipartUpload(TOKEN, request)).thenReturn(mockResponse);

        ResponseEntity<MultipartUploadInitResponse> response =
                courseController.initiateDemoVideoMultipartUpload(TOKEN, request);

        assertNotNull(response.getBody());
        assertEquals("upload-1", response.getBody().getUploadId());
        verify(courseService).initiateDemoVideoMultipartUpload(TOKEN, request);
    }

    // ================= getDemoVideoPresignedUrlForPart =================

    @Test
    void getDemoVideoPresignedUrlForPart_ReturnsUrl() {
        when(courseService.generateDemoVideoPresignedUrl(TOKEN, "upload-1", "key/demo.mp4", 1))
                .thenReturn("https://presigned.url/part1");

        ResponseEntity<Map<String, String>> response =
                courseController.getDemoVideoPresignedUrlForPart(TOKEN, "upload-1", "key/demo.mp4", 1);

        assertEquals("https://presigned.url/part1", response.getBody().get("presignedUrl"));
        verify(courseService).generateDemoVideoPresignedUrl(TOKEN, "upload-1", "key/demo.mp4", 1);
    }

    // ================= completeDemoVideoMultipartUpload =================

    @Test
    void completeDemoVideoMultipartUpload_ReturnsResponse() {
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId("upload-1");
        request.setFileKey("key/demo.mp4");

        CompleteMultipartUploadResponse mockResponse =
                new CompleteMultipartUploadResponse("upload-1", "key/demo.mp4", "completed");

        when(courseService.completeDemoVideoMultipartUpload(TOKEN, request)).thenReturn(mockResponse);

        ResponseEntity<CompleteMultipartUploadResponse> response =
                courseController.completeDemoVideoMultipartUpload(TOKEN, request);

        assertNotNull(response.getBody());
        assertEquals("key/demo.mp4", response.getBody().getVideoKey());
        verify(courseService).completeDemoVideoMultipartUpload(TOKEN, request);
    }

    // ================= abortDemoVideoMultipartUpload =================

    @Test
    void abortDemoVideoMultipartUpload_ReturnsMessage() {
        AbortMultipartUploadRequestDTO request = new AbortMultipartUploadRequestDTO();
        request.setUploadId("upload-1");
        request.setFileKey("key/demo.mp4");

        when(courseService.abortDemoVideoMultipartUpload(TOKEN, request)).thenReturn("Upload aborted");

        ResponseEntity<Map<String, String>> response =
                courseController.abortDemoVideoMultipartUpload(TOKEN, request);

        assertEquals("Upload aborted", response.getBody().get("message"));
        verify(courseService).abortDemoVideoMultipartUpload(TOKEN, request);
    }
}
