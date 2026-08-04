package com.example.course_service.controller;

import com.example.course_service.dto.request.MultipartUploadInitRequest;
import com.example.course_service.dto.request.SaveUploadedContentRequest;
import com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO;
import com.example.course_service.dto.request.AbortMultipartUploadRequestDTO;
import com.example.course_service.dto.response.LessonResponseDTO;
import com.example.course_service.dto.response.MessageResponseDTO;
import com.example.course_service.dto.response.MultipartUploadInitResponse;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.service.LessonService;
import com.example.course_service.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.util.JwtUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LessonControllerTest {

    private LessonController lessonController;
    private LessonService lessonService;
    private EnrollmentClient enrollmentClient;
    private JwtUtil jwtUtil;
    private CourseService courseService;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setup() {
        lessonService = mock(LessonService.class);
        jwtUtil = mock(JwtUtil.class);
        enrollmentClient = mock(EnrollmentClient.class);
        courseService = mock(CourseService.class);
        lessonController = new LessonController(lessonService, enrollmentClient, jwtUtil, courseService);
    }

    @Test
    void createLesson_CallsService() {
        LessonEntity lesson = new LessonEntity();
        lesson.setTitle("Introduction to Java");

        when(lessonService.createLesson(lesson, TOKEN)).thenReturn(Map.of("message", "Lesson Created Successfully."));

        ResponseEntity<Map<String, Object>> response = lessonController.createLesson(TOKEN, lesson);

        verify(lessonService, times(1)).createLesson(lesson, TOKEN);
        assertEquals("Lesson Created Successfully.", response.getBody().get("message"));
    }

    @Test
    void updateLesson_CallsService() {
        LessonEntity lesson = new LessonEntity();
        lesson.setTitle("Updated Lesson");

        when(lessonService.updateLesson(TOKEN, "L101", lesson)).thenReturn("Lesson Updated Successfully.");

        ResponseEntity<MessageResponseDTO> response = lessonController.updateLesson(TOKEN, "L101", lesson);

        verify(lessonService, times(1)).updateLesson(TOKEN, "L101", lesson);
        assertEquals("Lesson Updated Successfully.", response.getBody().getMessage());
    }

    @Test
    void getLessonById_ReturnsLesson() {
        LessonResponseDTO dto = LessonResponseDTO.builder()
                .lessonId("L101").moduleId("M1").title("Introduction to Java").duration(15).orderIndex(1).build();

        when(lessonService.getLessonById("L101", TOKEN)).thenReturn(dto);

        ResponseEntity<LessonResponseDTO> response = lessonController.getLessonById("L101", TOKEN);

        verify(lessonService, times(1)).getLessonById("L101", TOKEN);
        assertNotNull(response.getBody());
        assertEquals("L101", response.getBody().getLessonId());
    }

    @Test
    void getLessonsByModule_ReturnsList() {
        LessonResponseDTO dto1 = LessonResponseDTO.builder().lessonId("L101").title("Lesson 1").build();
        LessonResponseDTO dto2 = LessonResponseDTO.builder().lessonId("L102").title("Lesson 2").build();

        when(lessonService.getLessonsByModule("M1", TOKEN)).thenReturn(Arrays.asList(dto1, dto2));

        ResponseEntity<List<LessonResponseDTO>> response = lessonController.getLessonsByModule("M1", TOKEN);

        verify(lessonService, times(1)).getLessonsByModule("M1", TOKEN);
        assertEquals(2, response.getBody().size());
    }

    @Test
    void getUploadUrl_ReturnsUrl() {
        when(lessonService.generateUploadUrl(TOKEN, "L101", "video.mp4"))
                .thenReturn(Map.of("uploadUrl", "https://s3.com/presigned"));

        ResponseEntity<Map<String, String>> response = lessonController.getUploadUrl(TOKEN, "L101", "video.mp4");

        assertEquals("https://s3.com/presigned", response.getBody().get("uploadUrl"));
    }

    @Test
    void initiateMultipartUpload_ReturnsResponse() {
        MultipartUploadInitRequest request = new MultipartUploadInitRequest();
        MultipartUploadInitResponse mockResponse = new MultipartUploadInitResponse();

        when(lessonService.initiateMultipartUpload(TOKEN, request)).thenReturn(mockResponse);

        ResponseEntity<MultipartUploadInitResponse> response = lessonController.initiateMultipartUpload(TOKEN, request);

        verify(lessonService, times(1)).initiateMultipartUpload(TOKEN, request);
        assertNotNull(response.getBody());
    }

    @Test
    void completeMultipartUpload_CallsService() {
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        com.example.course_service.dto.response.CompleteMultipartUploadResponse mockResponse =
                new com.example.course_service.dto.response.CompleteMultipartUploadResponse("upload-123", "key", "done");

        when(lessonService.completeMultipartUpload(TOKEN, request)).thenReturn(mockResponse);

        ResponseEntity<com.example.course_service.dto.response.CompleteMultipartUploadResponse> response =
                lessonController.completeMultipartUpload(TOKEN, request);

        assertNotNull(response.getBody());
        assertEquals("key", response.getBody().getVideoKey());
    }

    @Test
    void abortMultipartUpload_CallsService() {
        AbortMultipartUploadRequestDTO request = new AbortMultipartUploadRequestDTO();

        when(lessonService.abortMultipartUpload(TOKEN, request)).thenReturn("Multipart upload aborted.");

        ResponseEntity<MessageResponseDTO> response = lessonController.abortMultipartUpload(TOKEN, request);

        assertEquals("Multipart upload aborted.", response.getBody().getMessage());
    }

    @Test
    void saveContent_CallsService() {
        SaveUploadedContentRequest request = new SaveUploadedContentRequest();

        when(lessonService.saveUploadedContent(TOKEN, request)).thenReturn("Content saved.");

        ResponseEntity<MessageResponseDTO> response = lessonController.saveContent(TOKEN, request);

        assertEquals("Content saved.", response.getBody().getMessage());
    }

    @Test
    void saveLiveRecording_CallsService() {
        com.example.course_service.dto.request.LiveRecordingRequest request = new com.example.course_service.dto.request.LiveRecordingRequest();
        request.setLessonId("L101");

        when(lessonService.saveLiveRecording(request)).thenReturn("Recording saved.");

        ResponseEntity<MessageResponseDTO> response = lessonController.saveLiveRecording(TOKEN, request);

        assertEquals("Recording saved.", response.getBody().getMessage());
        verify(lessonService, times(1)).saveLiveRecording(request);
    }

    @Test
    void getDownloadUrl_ReturnsUrl() {
        LessonResponseDTO lessonDTO = LessonResponseDTO.builder().lessonId("L101").courseId("course-1").build();

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(lessonService.getLessonById("L101", TOKEN)).thenReturn(lessonDTO);
        when(enrollmentClient.checkAccess("user-1", "course-1", "RECORDED_COURSE")).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(true).build());
        when(lessonService.getDownloadUrl(TOKEN, "L101", "video.mp4")).thenReturn("https://cloudfront.com/signed-url");

        ResponseEntity<?> response = lessonController.getDownloadUrl(TOKEN, "L101", "video.mp4");

        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("https://cloudfront.com/signed-url", body.get("downloadUrl"));
    }

    @Test
    void getLessonById_Enrolled_ReturnsLesson() {
        LessonResponseDTO dto = LessonResponseDTO.builder()
                .lessonId("L101").courseId("course-1").title("Intro").build();

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(lessonService.getLessonById("L101", TOKEN)).thenReturn(dto);
        when(enrollmentClient.checkAccess("user-1", "course-1", "RECORDED_COURSE")).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(true).build());

        ResponseEntity<LessonResponseDTO> response = lessonController.getLessonById("L101", TOKEN);

        assertNotNull(response.getBody());
        assertEquals("L101", response.getBody().getLessonId());
    }

    @Test
    void getLessonsByModule_Enrolled_ReturnsList() {
        LessonResponseDTO dto = LessonResponseDTO.builder()
                .lessonId("L101").courseId("course-1").title("Intro").build();

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(lessonService.getLessonsByModule("M1", TOKEN)).thenReturn(List.of(dto));
        when(enrollmentClient.checkAccess("user-1", "course-1", "RECORDED_COURSE")).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(true).build());

        ResponseEntity<List<LessonResponseDTO>> response = lessonController.getLessonsByModule("M1", TOKEN);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getDownloadUrl_NotEnrolled_ReturnsForbidden() {
        LessonResponseDTO lessonDTO = LessonResponseDTO.builder().lessonId("L101").courseId("course-1").build();

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(lessonService.getLessonById("L101", TOKEN)).thenReturn(lessonDTO);
        when(enrollmentClient.checkAccess("user-1", "course-1", "RECORDED_COURSE")).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(false).build());

        ResponseEntity<Object> response = lessonController.getDownloadUrl(TOKEN, "L101", "video.mp4");

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void getDownloadUrl_EnrollmentCheckFails_ReturnsForbidden() {
        LessonResponseDTO lessonDTO = LessonResponseDTO.builder().lessonId("L101").courseId("course-1").build();

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(lessonService.getLessonById("L101", TOKEN)).thenReturn(lessonDTO);
        when(enrollmentClient.checkAccess("user-1", "course-1", "RECORDED_COURSE"))
                .thenThrow(new RuntimeException("Feign error"));

        ResponseEntity<Object> response = lessonController.getDownloadUrl(TOKEN, "L101", "video.mp4");

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void getDownloadUrl_InvalidToken_Returns401() {
        when(jwtUtil.extractUserId(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        ResponseEntity<Object> response = lessonController.getDownloadUrl(TOKEN, "L101", "video.mp4");

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getLessonById_InvalidToken_Returns401() {
        LessonResponseDTO dto = LessonResponseDTO.builder().lessonId("L101").courseId("course-1").build();
        when(lessonService.getLessonById("L101", TOKEN)).thenReturn(dto);
        when(jwtUtil.extractUserId(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        ResponseEntity<LessonResponseDTO> response = lessonController.getLessonById("L101", TOKEN);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getLessonsByModule_EmptyList_ReturnsOk() {
        when(lessonService.getLessonsByModule("M1", TOKEN)).thenReturn(List.of());

        ResponseEntity<List<LessonResponseDTO>> response = lessonController.getLessonsByModule("M1", TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getLessonsByModule_InvalidToken_Returns401() {
        LessonResponseDTO dto = LessonResponseDTO.builder().lessonId("L101").courseId("course-1").build();
        when(lessonService.getLessonsByModule("M1", TOKEN)).thenReturn(List.of(dto));
        when(jwtUtil.extractUserId(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        ResponseEntity<List<LessonResponseDTO>> response = lessonController.getLessonsByModule("M1", TOKEN);

        assertEquals(401, response.getStatusCode().value());
    }
}
