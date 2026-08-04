package com.example.enrollment_progress_service.exception;

public class DownstreamServiceException extends RuntimeException {
    public DownstreamServiceException(String message) {
        super(message);
    }
    public DownstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
