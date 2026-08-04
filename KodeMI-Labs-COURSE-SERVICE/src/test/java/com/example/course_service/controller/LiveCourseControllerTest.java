package com.example.course_service.controller;

import com.example.course_service.dto.request.AttachRecordingRequest;
import com.example.course_service.dto.request.LinkLiveSessionRequest;
import com.example.course_service.dto.response.LiveCourseDetailResponseDTO;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.service.LiveCourseService;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LiveCourseControllerTest {

    private LiveCourseController liveCourseController;
    private LiveCourseService liveCourseService;
    private EnrollmentClient enrollmentClient;
    private JwtUtil jwtUtil;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setup() {
        liveCourseService = mock(LiveCourseService.class);
        enrollmentClient = mock(EnrollmentClient.class);
        jwtUtil = mock(JwtUtil.class);
        liveCourseController = new LiveCourseController(liveCourseService, enrollmentClient, jwtUtil);
    }

    @Test
    void linkLiveSession_CallsService() {
        LinkLiveSessionRequest request = new LinkLiveSessionRequest();
        request.setLiveSessionId("session-1");

        when(liveCourseService.linkLiveSession("course-1", request))
                .thenReturn("Live session linked successfully.");

        ResponseEntity<Map<String, String>> response =
                liveCourseController.linkLiveSession(TOKEN, "course-1", request);

        assertEquals("Live session linked successfully.", response.getBody().get("message"));
        verify(liveCourseService).linkLiveSession("course-1", request);
    }

    @Test
    void attachRecording_CallsService() {
        AttachRecordingRequest request = new AttachRecordingRequest();
        request.setCourseId("course-1");
        request.setLiveSessionId("session-1");
        request.setRecordingUrl("https://s3.com/recording.mp4");

        when(liveCourseService.attachRecording(request))
                .thenReturn("Recording attached successfully.");

        ResponseEntity<Map<String, String>> response =
                liveCourseController.attachRecording(request);

        assertEquals("Recording attached successfully.", response.getBody().get("message"));
    }

    @Test
    void getLiveCourseDetail_WithToken_Enrolled_ReturnsDetail() {
        LiveCourseDetailResponseDTO dto = LiveCourseDetailResponseDTO.builder()
                .courseId("course-1")
                .title("Live Java")
                .build();

        when(liveCourseService.getLiveCourseDetail("course-1")).thenReturn(dto);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(enrollmentClient.checkAccess("user-1", "course-1", "LIVE_COURSE")).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(true).build());

        ResponseEntity<LiveCourseDetailResponseDTO> response =
                liveCourseController.getLiveCourseDetail("course-1", TOKEN);

        assertNotNull(response.getBody());
        assertEquals("course-1", response.getBody().getCourseId());
    }

    @Test
    void getLiveCourseDetail_WithToken_NotEnrolled_MasksContent() {
        LiveCourseDetailResponseDTO dto = mock(LiveCourseDetailResponseDTO.class);
        when(dto.getCourseId()).thenReturn("course-1");

        when(liveCourseService.getLiveCourseDetail("course-1")).thenReturn(dto);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(enrollmentClient.checkAccess("user-1", "course-1", "LIVE_COURSE")).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(false).build());

        ResponseEntity<LiveCourseDetailResponseDTO> response =
                liveCourseController.getLiveCourseDetail("course-1", TOKEN);

        assertNotNull(response.getBody());
        verify(dto).maskVideoContent();
    }

    @Test
    void getLiveCourseDetail_NoToken_MasksContent() {
        LiveCourseDetailResponseDTO dto = mock(LiveCourseDetailResponseDTO.class);

        when(liveCourseService.getLiveCourseDetail("course-1")).thenReturn(dto);

        ResponseEntity<LiveCourseDetailResponseDTO> response =
                liveCourseController.getLiveCourseDetail("course-1", null);

        assertNotNull(response.getBody());
        verify(dto).maskVideoContent();
    }

    @Test
    void getLiveCourseDetail_InvalidToken_Returns401() {
        LiveCourseDetailResponseDTO dto = LiveCourseDetailResponseDTO.builder()
                .courseId("course-1").build();

        when(liveCourseService.getLiveCourseDetail("course-1")).thenReturn(dto);
        when(jwtUtil.extractUserId(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        ResponseEntity<LiveCourseDetailResponseDTO> response =
                liveCourseController.getLiveCourseDetail("course-1", TOKEN);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getLiveCourseDetail_EnrollmentCheckFails_MasksContent() {
        LiveCourseDetailResponseDTO dto = mock(LiveCourseDetailResponseDTO.class);
        when(dto.getCourseId()).thenReturn("course-1");

        when(liveCourseService.getLiveCourseDetail("course-1")).thenReturn(dto);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(enrollmentClient.checkAccess("user-1", "course-1", "LIVE_COURSE"))
                .thenThrow(new RuntimeException("Feign error"));

        ResponseEntity<LiveCourseDetailResponseDTO> response =
                liveCourseController.getLiveCourseDetail("course-1", TOKEN);

        assertNotNull(response.getBody());
        verify(dto).maskVideoContent();
    }

    @Test
    void getLiveCourseDetail_NullCourse_ReturnsOkWithNull() {
        when(liveCourseService.getLiveCourseDetail("course-1")).thenReturn(null);

        ResponseEntity<LiveCourseDetailResponseDTO> response =
                liveCourseController.getLiveCourseDetail("course-1", null);

        assertEquals(200, response.getStatusCode().value());
    }
}
