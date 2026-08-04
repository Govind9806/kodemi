package com.example.ai_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private Map<String, Object> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        return response;
    }
    @ExceptionHandler(ConversationNotFound.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            ConversationNotFound ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND));
    }
    @ExceptionHandler(ConversationServiceException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            ConversationServiceException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }
    @ExceptionHandler(MessagesNotFound.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            MessagesNotFound ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND));
    }
    @ExceptionHandler(MessageServiceException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            MessageServiceException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }
    @ExceptionHandler(InvalidRequest.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            InvalidRequest ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }
    @ExceptionHandler(UnAuthorizedRequestException.class)
    public ResponseEntity<Map<String, Object>> handleUnAuthorizedRequestException(
            UnAuthorizedRequestException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED));
    }
}
