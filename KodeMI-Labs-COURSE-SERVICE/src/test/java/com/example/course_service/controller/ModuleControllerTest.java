package com.example.course_service.controller;

import com.example.course_service.dto.response.ModuleResponseDTO;
import com.example.course_service.model.ModuleEntity;
import com.example.course_service.service.ModuleService;
import com.example.course_service.service.CourseService;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import com.example.course_service.feign.EnrollmentClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModuleControllerTest {

    private ModuleController moduleController;
    private ModuleService moduleService;
    private JwtUtil jwtUtil;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setup() {
        moduleService = mock(ModuleService.class);
        jwtUtil = mock(JwtUtil.class);
        EnrollmentClient enrollmentClient = mock(EnrollmentClient.class);
        CourseService courseService = mock(CourseService.class);
        moduleController = new ModuleController(moduleService, jwtUtil, enrollmentClient, courseService);
    }

    @Test
    void createModule_CallsService() {
        ModuleEntity module = new ModuleEntity();
        module.setCourseId("course-1");
        module.setTitle("Introduction");

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(moduleService.createModule(module, "user-1"))
                .thenReturn(Map.of("message", "Module Created Successfully", "moduleId", "M101"));

        ResponseEntity<Map<String, Object>> response = moduleController.createModule(TOKEN, module);

        verify(moduleService, times(1)).createModule(module, "user-1");
        assertEquals("Module Created Successfully", response.getBody().get("message"));
    }

    @Test
    void updateModule_CallsService() {
        ModuleEntity module = new ModuleEntity();
        module.setTitle("Updated Title");

        // Controller passes token directly as userId to moduleService.updateModule
        when(moduleService.updateModule(TOKEN, "module-1", module))
                .thenReturn("Module Updated Successfully.");

        ResponseEntity<Map<String, String>> response = moduleController.updateModule(TOKEN, "module-1", module);

        verify(moduleService, times(1)).updateModule(TOKEN, "module-1", module);
        assertEquals("Module Updated Successfully.", response.getBody().get("message"));
    }

    @Test
    void getModulesByCourse_ReturnsList() {
        ModuleResponseDTO dto1 = ModuleResponseDTO.builder()
                .moduleId("M1").courseId("course-1").title("Intro").orderIndex(1).build();
        ModuleResponseDTO dto2 = ModuleResponseDTO.builder()
                .moduleId("M2").courseId("course-1").title("Basics").orderIndex(2).build();

        when(moduleService.getModulesByCourse("course-1")).thenReturn(Arrays.asList(dto1, dto2));

        ResponseEntity<List<ModuleResponseDTO>> response = moduleController.getModulesByCourse("course-1", TOKEN);

        verify(moduleService, times(1)).getModulesByCourse("course-1");
        assertEquals(2, response.getBody().size());
        assertEquals("M1", response.getBody().get(0).getModuleId());
        assertEquals("Basics", response.getBody().get(1).getTitle());
    }

    @Test
    void getModulesByCourse_NoToken_MasksLessons() {
        com.example.course_service.dto.response.LessonResponseDTO lesson =
                com.example.course_service.dto.response.LessonResponseDTO.builder()
                        .lessonId("L1").videoKey("videos/test.mp4").build();

        ModuleResponseDTO dto = ModuleResponseDTO.builder()
                .moduleId("M1").courseId("course-1")
                .lessons(List.of(lesson)).build();

        when(moduleService.getModulesByCourse("course-1")).thenReturn(List.of(dto));

        ResponseEntity<List<ModuleResponseDTO>> response =
                moduleController.getModulesByCourse("course-1", null);

        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody().get(0).getLessons().get(0).getVideoKey());
    }

    @Test
    void getModulesByCourse_InvalidToken_Returns401() {
        ModuleResponseDTO dto = ModuleResponseDTO.builder().moduleId("M1").build();
        when(moduleService.getModulesByCourse("course-1")).thenReturn(List.of(dto));
        when(jwtUtil.extractUserId(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        ResponseEntity<List<ModuleResponseDTO>> response =
                moduleController.getModulesByCourse("course-1", TOKEN);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getModulesByCourse_EnrollmentFails_MasksLessons() {
        com.example.course_service.dto.response.LessonResponseDTO lesson =
                com.example.course_service.dto.response.LessonResponseDTO.builder()
                        .lessonId("L1").videoKey("videos/test.mp4").build();

        ModuleResponseDTO dto = ModuleResponseDTO.builder()
                .moduleId("M1").courseId("course-1")
                .lessons(List.of(lesson)).build();

        when(moduleService.getModulesByCourse("course-1")).thenReturn(List.of(dto));
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");

        com.example.course_service.feign.EnrollmentClient enrollmentClient = mock(com.example.course_service.feign.EnrollmentClient.class);
        when(enrollmentClient.checkAccess(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Feign error"));
        CourseService courseService = mock(CourseService.class);
        moduleController = new ModuleController(moduleService, jwtUtil, enrollmentClient, courseService);

        ResponseEntity<List<ModuleResponseDTO>> response =
                moduleController.getModulesByCourse("course-1", TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody().get(0).getLessons().get(0).getVideoKey());
    }

    @Test
    void getModulesByCourse_NullLessons_NoException() {
        ModuleResponseDTO dto = ModuleResponseDTO.builder()
                .moduleId("M1").courseId("course-1").lessons(null).build();

        when(moduleService.getModulesByCourse("course-1")).thenReturn(List.of(dto));

        ResponseEntity<List<ModuleResponseDTO>> response =
                moduleController.getModulesByCourse("course-1", null);

        assertEquals(200, response.getStatusCode().value());
    }
}
