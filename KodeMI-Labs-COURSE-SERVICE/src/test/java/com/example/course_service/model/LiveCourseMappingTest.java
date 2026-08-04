package com.example.course_service.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class LiveCourseMappingTest {

    @Test
    void setAndGetId_WorksCorrectly() {
        LiveCourseMapping mapping = new LiveCourseMapping();
        mapping.setId("mapping-1");
        assertEquals("mapping-1", mapping.getId());
    }

    @Test
    void setAndGetAllFields_WorksCorrectly() {
        Date now = new Date();
        LiveCourseMapping mapping = new LiveCourseMapping();
        mapping.setId("m1");
        mapping.setCourseId("course-1");
        mapping.setLiveSessionId("session-1");
        mapping.setRecordingUrl("https://s3.com/recording.mp4");
        mapping.setCreatedAt(now);
        mapping.setUpdatedAt(now);

        assertEquals("m1", mapping.getId());
        assertEquals("course-1", mapping.getCourseId());
        assertEquals("session-1", mapping.getLiveSessionId());
        assertEquals("https://s3.com/recording.mp4", mapping.getRecordingUrl());
        assertEquals(now, mapping.getCreatedAt());
        assertEquals(now, mapping.getUpdatedAt());
    }
}