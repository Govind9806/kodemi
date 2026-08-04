package com.example.course_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionClassesTest {

    @Test
    void courseNotFoundException_HasMessage() {
        CourseNotFoundException ex = new CourseNotFoundException("Course C1 not found");
        assertEquals("Course C1 not found", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void lessonNotFoundException_HasMessage() {
        LessonNotFoundException ex = new LessonNotFoundException("Lesson not found");
        assertEquals("Lesson not found", ex.getMessage());
    }

    @Test
    void moduleNotFoundException_HasMessage() {
        ModuleNotFoundException ex = new ModuleNotFoundException("Module not found");
        assertEquals("Module not found", ex.getMessage());
    }

    @Test
    void categoryNotFoundException_HasMessage() {
        CategoryNotFoundException ex = new CategoryNotFoundException("Category not found");
        assertEquals("Category not found", ex.getMessage());
    }

    @Test
    void reviewNotFoundException_HasMessage() {
        ReviewNotFoundException ex = new ReviewNotFoundException("Review not found");
        assertEquals("Review not found", ex.getMessage());
    }

    @Test
    void forbiddenException_HasMessage() {
        ForbiddenException ex = new ForbiddenException("Access denied");
        assertEquals("Access denied", ex.getMessage());
    }

    @Test
    void nullException_HasMessage() {
        NullException ex = new NullException("Null body");
        assertEquals("Null body", ex.getMessage());
    }

    @Test
    void thumbnailNotFoundException_HasMessage() {
        ThumbnailNotFoundException ex = new ThumbnailNotFoundException("Thumbnail required");
        assertEquals("Thumbnail required", ex.getMessage());
    }

    @Test
    void tokenNotFoundException_HasMessage() {
        TokenNotFoundException ex = new TokenNotFoundException("Token required");
        assertEquals("Token required", ex.getMessage());
    }

    @Test
    void fileUploadException_WithCause_HasAllFields() {
        RuntimeException cause = new RuntimeException("IO error");
        FileUploadException ex = new FileUploadException("Upload failed", cause, "img.png", "s3/key", 2);

        assertEquals("Upload failed", ex.getMessage());
        assertEquals("img.png", ex.getFileName());
        assertEquals("s3/key", ex.getS3Key());
        assertEquals(2, ex.getRetryCount());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void fileUploadException_WithoutCause_HasAllFields() {
        FileUploadException ex = new FileUploadException("Upload failed", "img.png", "s3/key", 1);

        assertEquals("Upload failed", ex.getMessage());
        assertEquals("img.png", ex.getFileName());
        assertEquals("s3/key", ex.getS3Key());
        assertEquals(1, ex.getRetryCount());
    }
}
