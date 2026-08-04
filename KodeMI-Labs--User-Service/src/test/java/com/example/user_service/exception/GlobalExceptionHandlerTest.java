package com.example.user_service.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.ConnectException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleLearnerNotFound_returns404() {
        LearnerNotFoundException ex = new LearnerNotFoundException("Learner not found");
        ResponseEntity<ErrorResponse> response = handler.handleLearnerNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Learner not found"));
    }

    @Test
    void handleTrainerNotFound_returns404() {
        TrainerNotFoundException ex = new TrainerNotFoundException("Trainer not found");
        ResponseEntity<ErrorResponse> response = handler.handleTrainerNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Trainer not found", response.getBody().getMessage());
    }

    @Test
    void handleNoActiveRequest_returns404() {
        NoActiveRequestException ex = new NoActiveRequestException("No active request");
        ResponseEntity<ErrorResponse> response = handler.handleNoActiveRequest(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("No active request", response.getBody().getMessage());
    }

    @Test
    void handleFileNotFound_returns404() {
        FileNotFoundException ex = new FileNotFoundException("File not found");
        ResponseEntity<ErrorResponse> response = handler.handleFileNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("File not found", response.getBody().getMessage());
    }

    @Test
    void handleUserException_returns400() {
        UserException ex = new UserException("User error");
        ResponseEntity<ErrorResponse> response = handler.handleUserException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User error", response.getBody().getMessage());
    }

    @Test
    void handleFileException_returns422() {
        FileException ex = new FileException("File error");
        ResponseEntity<ErrorResponse> response = handler.handleFileException(ex);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("File error", response.getBody().getMessage());
    }

    @Test
    void handleUserServiceException_returns500() {
        UserServiceException ex = new UserServiceException("Service error");
        ResponseEntity<ErrorResponse> response = handler.handleUserServiceException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Service error", response.getBody().getMessage());
    }

    @Test
    void handleEncryptionException_returns500() {
        EncryptionException ex = new EncryptionException("Encryption error");
        ResponseEntity<ErrorResponse> response = handler.handleEncryptionException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("internal error occurred"));
    }

    @Test
    void handleValidationErrors_returns400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "field", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("must not be blank", response.getBody().getFieldErrors().get("field"));
    }

    @Test
    void handleConstraintViolation_returns400WithFieldErrors() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> v = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("rateTrainer.rating");
        when(v.getPropertyPath()).thenReturn(path);
        when(v.getMessage()).thenReturn("invalid value");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(v));
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid value", response.getBody().getFieldErrors().get("rating"));
    }

    @Test
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody().getMessage());
    }

    @Test
    void handleNullPointer_withMessage_returns400() {
        NullPointerException ex = new NullPointerException("Name cannot be null");
        ResponseEntity<ErrorResponse> response = handler.handleNullPointer(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Name cannot be null", response.getBody().getMessage());
    }

    @Test
    void handleNullPointer_emptyMessage_returns500() {
        NullPointerException ex = new NullPointerException();
        ResponseEntity<ErrorResponse> response = handler.handleNullPointer(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void handleExpiredJwt_returns401() {
        ExpiredJwtException ex = mock(ExpiredJwtException.class);
        ResponseEntity<ErrorResponse> response = handler.handleExpiredJwt(ex);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleMalformedJwt_returns401() {
        MalformedJwtException ex = new MalformedJwtException("bad token");
        ResponseEntity<ErrorResponse> response = handler.handleMalformedJwt(ex);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleJwtSignature_returns401() {
        SignatureException ex = new SignatureException("sig mismatch");
        ResponseEntity<ErrorResponse> response = handler.handleJwtSignature(ex);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleJwtException_returns401() {
        JwtException ex = new JwtException("jwt err");
        ResponseEntity<ErrorResponse> response = handler.handleJwtException(ex);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleAccessDenied_returns403() {
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("Access denied");
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void handleMissingHeader_returns400() {
        MissingRequestHeaderException ex = new MissingRequestHeaderException("X-Header", mock(org.springframework.core.MethodParameter.class));
        ResponseEntity<ErrorResponse> response = handler.handleMissingHeader(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("X-Header"));
    }

    @Test
    void handleMissingParam_returns400() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("paramName", "String");
        ResponseEntity<ErrorResponse> response = handler.handleMissingParam(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("paramName"));
    }

    @Test
    void handleMissingPart_returns400() {
        MissingServletRequestPartException ex = new MissingServletRequestPartException("filePart");
        ResponseEntity<ErrorResponse> response = handler.handleMissingPart(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("filePart"));
    }

    @Test
    void handleUnreadableMessage_returns400() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleUnsupportedMediaType_returns415() {
        HttpMediaTypeNotSupportedException ex = mock(HttpMediaTypeNotSupportedException.class);
        ResponseEntity<ErrorResponse> response = handler.handleUnsupportedMediaType(ex);
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
    }

    @Test
    void handleMethodNotSupported_returns405() {
        HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    void handleTypeMismatch_returns400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleNoResourceFound_returns404() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleMaxUploadSize_returns413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5000L);
        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSize(ex);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
    }

    @Test
    void handleDownstreamFailures_returns53() {
        ConnectException ex = new ConnectException("Connection refused");
        ResponseEntity<ErrorResponse> response = handler.handleDownstreamFailures(ex);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("internal error");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
