package com.example.course_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // Error response key constants
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String DETAILS_KEY = "details";
    private static final String FILE_NAME_KEY = "fileName";
    private static final String S3_KEY = "s3Key";
    private static final String RETRY_COUNT_KEY = "retryCount";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ERROR_KEY, "ValidationError");
        errorResponse.put(MESSAGE_KEY, "Request validation failed");
        java.util.List<String> details = new java.util.ArrayList<>();
        for (org.springframework.validation.FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            details.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        errorResponse.put(DETAILS_KEY, details);
        log.warn("Validation error: {}", errorResponse.get(DETAILS_KEY));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(HttpMessageNotReadableException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ERROR_KEY, "BadRequest");
        errorResponse.put(MESSAGE_KEY, "Malformed request payload");
        log.warn("Malformed request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ERROR_KEY, "BadRequest");
        errorResponse.put(MESSAGE_KEY, e.getMessage());
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({LessonNotFoundException.class, CourseNotFoundException.class, ModuleNotFoundException.class, CategoryNotFoundException.class, ReviewNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ERROR_KEY, "NotFound");
        errorResponse.put(MESSAGE_KEY, e.getMessage());
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ERROR_KEY, "Forbidden");
        errorResponse.put(MESSAGE_KEY, e.getMessage());
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<Map<String, Object>> handleFileUploadException(FileUploadException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ERROR_KEY, "FileUploadError");
        errorResponse.put(MESSAGE_KEY, e.getMessage());
        errorResponse.put(FILE_NAME_KEY, e.getFileName());
        errorResponse.put(S3_KEY, e.getS3Key());
        errorResponse.put(RETRY_COUNT_KEY, e.getRetryCount());
        log.error("File upload failed: {}, s3Key: {}", e.getMessage(), e.getS3Key());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler({
        FeignException.class,
        CallNotPermittedException.class,
        DownstreamServiceException.class,
        ConnectException.class,
        SocketTimeoutException.class
    })
    public ResponseEntity<Map<String, Object>> handleDownstreamFailure(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ERROR_KEY, "ServiceUnavailable");
        errorResponse.put(MESSAGE_KEY, "Downstream service is currently unavailable");
        errorResponse.put(DETAILS_KEY, e.getMessage() != null ? e.getMessage() : "Downstream service invocation failed or timed out.");
        log.error("Downstream service failure: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ERROR_KEY, "InternalServerError");
        errorResponse.put(MESSAGE_KEY, "An unexpected error occurred");
        errorResponse.put(DETAILS_KEY, "An internal server error occurred. Please try again later.");
        log.error("Unhandled exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
