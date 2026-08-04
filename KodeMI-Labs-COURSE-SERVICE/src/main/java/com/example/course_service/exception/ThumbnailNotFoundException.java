package com.example.course_service.exception;

public class ThumbnailNotFoundException extends RuntimeException {
    public ThumbnailNotFoundException(String message) {
        super(message);
    }
}
