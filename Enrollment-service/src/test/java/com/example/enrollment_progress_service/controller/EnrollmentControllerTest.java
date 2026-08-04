package com.example.enrollment_progress_service.controller;

import com.example.enrollment_progress_service.dto.request.EnrollmentRequest;
import com.example.enrollment_progress_service.dto.request.UnenrollRequest;
import com.example.enrollment_progress_service.dto.request.BatchEnrollmentStatsRequest;
import com.example.enrollment_progress_service.dto.response.AccessCheckResponse;
import com.example.enrollment_progress_service.dto.response.EnrollmentResponse;
import com.example.enrollment_progress_service.dto.response.EnrollmentStatusResponse;
import com.example.enrollment_progress_service.dto.response.CourseEnrollmentStatsResponse;
import com.example.enrollment_progress_service.enums.EnrollmentStatus;
import com.example.enrollment_progress_service.enums.EnrollmentTargetType;
import com.example.enrollment_progress_service.model.EnrollmentEntity;
import com.example.enrollment_progress_service.service.EnrollmentService;
import com.example.enrollment_progress_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnrollmentControllerTest {

    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private EnrollmentController enrollmentController;

    private static final String TOKEN = "Bearer token123";
    private static final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
    }

    @Test
    void enroll_success() {
        EnrollmentRequest request = new EnrollmentRequest();
        EnrollmentResponse expected = new EnrollmentResponse();
        when(enrollmentService.enroll(USER_ID, request, TOKEN)).thenReturn(expected);

        ResponseEntity<EnrollmentResponse> response = enrollmentController.enroll(TOKEN, request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void unenroll_success() {
        UnenrollRequest request = new UnenrollRequest();
        EnrollmentResponse expected = new EnrollmentResponse();
        when(enrollmentService.unenroll(USER_ID, request)).thenReturn(expected);

        ResponseEntity<EnrollmentResponse> response = enrollmentController.unenroll(TOKEN, request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getMyEnrollments_success() {
        List<EnrollmentEntity> expected = List.of(new EnrollmentEntity());
        when(enrollmentService.getMyEnrollments(USER_ID)).thenReturn(expected);

        ResponseEntity<List<EnrollmentEntity>> response = enrollmentController.getMyEnrollments(TOKEN);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getStatus_success() {
        EnrollmentStatusResponse expected = new EnrollmentStatusResponse();
        when(enrollmentService.getStatus(USER_ID, "target-1", "RECORDED_COURSE")).thenReturn(expected);

        ResponseEntity<EnrollmentStatusResponse> response = enrollmentController.getStatus(TOKEN, "target-1", "RECORDED_COURSE");
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void checkAccess_success() {
        AccessCheckResponse expected = new AccessCheckResponse();
        when(enrollmentService.checkAccess(USER_ID, "target-1", "RECORDED_COURSE")).thenReturn(expected);

        ResponseEntity<AccessCheckResponse> response = enrollmentController.checkAccess(USER_ID, "target-1", "RECORDED_COURSE");
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getLegacyCourseStatus_success() {
        EnrollmentStatusResponse expected = new EnrollmentStatusResponse();
        when(enrollmentService.getStatus(USER_ID, "course-1", "RECORDED_COURSE")).thenReturn(expected);

        ResponseEntity<EnrollmentStatusResponse> response = enrollmentController.getLegacyCourseStatus(TOKEN, "course-1", "RECORDED_COURSE");
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getLegacyConferenceStatus_success() {
        EnrollmentStatusResponse expected = new EnrollmentStatusResponse();
        when(enrollmentService.getStatus(USER_ID, "conf-1", "CONFERENCE")).thenReturn(expected);

        ResponseEntity<EnrollmentStatusResponse> response = enrollmentController.getLegacyConferenceStatus(TOKEN, "conf-1");
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getEnrolledLearners_success() {
        List<String> expected = List.of("user1", "user2");
        when(enrollmentService.getEnrolledLearners("course-1")).thenReturn(expected);

        ResponseEntity<List<String>> response = enrollmentController.getEnrolledLearners("course-1");
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getCourseStats_success() {
        CourseEnrollmentStatsResponse expected = new CourseEnrollmentStatsResponse();
        when(enrollmentService.getEnrollmentStats("course-1", "RECORDED_COURSE")).thenReturn(expected);

        ResponseEntity<CourseEnrollmentStatsResponse> response = enrollmentController.getCourseStats("course-1", "RECORDED_COURSE");
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getBatchStats_success() {
        BatchEnrollmentStatsRequest request = new BatchEnrollmentStatsRequest();
        java.util.Map<String, CourseEnrollmentStatsResponse> expected = java.util.Map.of("course-1", new CourseEnrollmentStatsResponse());
        when(enrollmentService.getBatchEnrollmentStats(request)).thenReturn(expected);

        ResponseEntity<java.util.Map<String, CourseEnrollmentStatsResponse>> response = enrollmentController.getBatchStats(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getInternalCourseStats_success() {
        CourseEnrollmentStatsResponse expected = new CourseEnrollmentStatsResponse();
        when(enrollmentService.getEnrollmentStats("course-1", "RECORDED_COURSE")).thenReturn(expected);

        ResponseEntity<CourseEnrollmentStatsResponse> response = enrollmentController.getInternalCourseStats("course-1", "RECORDED_COURSE");
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getInternalBatchStats_success() {
        BatchEnrollmentStatsRequest request = new BatchEnrollmentStatsRequest();
        java.util.Map<String, CourseEnrollmentStatsResponse> expected = java.util.Map.of("course-1", new CourseEnrollmentStatsResponse());
        when(enrollmentService.getBatchEnrollmentStats(request)).thenReturn(expected);

        ResponseEntity<java.util.Map<String, CourseEnrollmentStatsResponse>> response = enrollmentController.getInternalBatchStats(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }
}
