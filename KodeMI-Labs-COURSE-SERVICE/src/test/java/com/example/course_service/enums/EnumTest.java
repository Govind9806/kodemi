package com.example.course_service.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumTest {

    @Test
    void courseType_Values() {
        assertEquals(CourseType.RECORDED, CourseType.valueOf("RECORDED"));
        assertEquals(CourseType.LIVE, CourseType.valueOf("LIVE"));
        assertEquals(2, CourseType.values().length);
    }

    @Test
    void lessonType_Values() {
        assertEquals(LessonType.VIDEO, LessonType.valueOf("VIDEO"));
        assertEquals(LessonType.TEXT, LessonType.valueOf("TEXT"));
        assertEquals(LessonType.QUIZ, LessonType.valueOf("QUIZ"));
        assertEquals(LessonType.LIVE, LessonType.valueOf("LIVE"));
        assertEquals(7, LessonType.values().length);
    }

    @Test
    void moderationStatus_Values() {
        assertEquals(ModerationStatus.APPROVED, ModerationStatus.valueOf("APPROVED"));
        assertEquals(ModerationStatus.PENDING, ModerationStatus.valueOf("PENDING"));
        assertEquals(ModerationStatus.HIDDEN, ModerationStatus.valueOf("HIDDEN"));
        assertEquals(ModerationStatus.REJECTED, ModerationStatus.valueOf("REJECTED"));
        assertEquals(4, ModerationStatus.values().length);
    }

    @Test
    void role_Values() {
        assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
        assertEquals(Role.TRAINER, Role.valueOf("TRAINER"));
        assertEquals(Role.STUDENT, Role.valueOf("STUDENT"));
        assertEquals(3, Role.values().length);
    }
}
