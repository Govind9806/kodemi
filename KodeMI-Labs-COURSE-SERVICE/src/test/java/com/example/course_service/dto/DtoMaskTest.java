package com.example.course_service.dto;

import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.LessonResponseDTO;
import com.example.course_service.dto.response.LiveCourseDetailResponseDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoMaskTest {

    @Test
    void lessonResponseDTO_maskContent_ClearsFields() {
        LessonResponseDTO dto = new LessonResponseDTO();
        dto.setVideoKey("videos/lesson.mp4");

        dto.maskContent();

        assertNull(dto.getVideoKey());
        assertNull(dto.getContentKey());
    }

    @Test
    void courseResponseDTO_maskVideoContent_MasksLessons() {
        LessonResponseDTO lesson = new LessonResponseDTO();
        lesson.setVideoKey("videos/lesson.mp4");

        CourseResponseDTO course = CourseResponseDTO.builder()
                .courseId("C1")
                .lessons(List.of(lesson))
                .build();

        course.maskVideoContent();

        assertNull(course.getLessons().get(0).getVideoKey());
    }

    @Test
    void courseResponseDTO_maskVideoContent_MasksLessonsInsideModules() {
        LessonResponseDTO lesson = new LessonResponseDTO();
        lesson.setVideoKey("videos/lesson.mp4");
        lesson.setContentKey(new ArrayList<>());

        com.example.course_service.dto.response.ModuleResponseDTO module = com.example.course_service.dto.response.ModuleResponseDTO.builder()
                .moduleId("M1")
                .lessons(List.of(lesson))
                .build();

        CourseResponseDTO course = CourseResponseDTO.builder()
                .courseId("C1")
                .modules(List.of(module))
                .build();

        course.maskVideoContent();

        assertNull(course.getModules().get(0).getLessons().get(0).getVideoKey());
        assertNull(course.getModules().get(0).getLessons().get(0).getContentKey());
    }

    @Test
    void courseResponseDTO_maskVideoContent_NullLessons_NoException() {
        CourseResponseDTO course = CourseResponseDTO.builder()
                .courseId("C1")
                .lessons(null)
                .build();

        assertDoesNotThrow(course::maskVideoContent);
    }

    @Test
    void courseResponseDTO_maskVideoContent_EmptyLessons_NoException() {
        CourseResponseDTO course = CourseResponseDTO.builder()
                .courseId("C1")
                .lessons(new ArrayList<>())
                .build();

        assertDoesNotThrow(course::maskVideoContent);
        assertTrue(course.getLessons().isEmpty());
    }

    @Test
    void liveCourseDetailResponseDTO_maskVideoContent_ClearsFields() {
        LiveCourseDetailResponseDTO dto = LiveCourseDetailResponseDTO.builder()
                .courseId("C1")
                .joinLink("https://zoom.us/join/123")
                .recordingUrl("https://s3.com/recording.mp4")
                .liveSessionId("session-1")
                .build();

        dto.maskVideoContent();

        assertNull(dto.getJoinLink());
        assertNull(dto.getRecordingUrl());
        assertNull(dto.getLiveSessionId());
    }
}
