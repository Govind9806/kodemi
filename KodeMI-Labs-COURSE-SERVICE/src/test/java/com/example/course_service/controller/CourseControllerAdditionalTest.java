package com.example.course_service.controller;

import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.service.CourseService;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        CourseEntity request = new CourseEntity();
        MultipartFile thumbnail = mock(MultipartFile.class);

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
        CourseEntity request = new CourseEntity();
        MultipartFile thumbnail = mock(MultipartFile.class);

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
        CourseEntity request = new CourseEntity();
        MultipartFile thumbnail = mock(MultipartFile.class);

        when(courseService.updateCourse(TOKEN, "C101", request, thumbnail))
                .thenReturn("Course Updated Successfully.");

        ResponseEntity<Map<String, String>> response =
                courseController.updateCourse(TOKEN, "C101", request, thumbnail);

        assertEquals("Course Updated Successfully.", response.getBody().get("message"));
        verify(courseService).updateCourse(TOKEN, "C101", request, thumbnail);
    }
}