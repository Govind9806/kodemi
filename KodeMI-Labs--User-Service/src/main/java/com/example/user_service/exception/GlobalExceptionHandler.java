package com.example.user_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    public static  final String MISSING = "' is missing.";

    // ─────────────────────────────────────── helpers ───────────────────────────────────────

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<ErrorResponse> buildResponseWithFieldErrors(
            HttpStatus status, String message, Map<String, String> fieldErrors) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    // ═══════════════════════════════ CUSTOM / DOMAIN EXCEPTIONS ═══════════════════════════════

    @ExceptionHandler(LearnerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLearnerNotFound(LearnerNotFoundException ex) {
        log.warn("Learner not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TrainerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrainerNotFound(TrainerNotFoundException ex) {
        log.warn("Trainer not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NoActiveRequestException.class)
    public ResponseEntity<ErrorResponse> handleNoActiveRequest(NoActiveRequestException ex) {
        log.warn("No active request: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException ex) {
        log.warn("File not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException ex) {
        log.warn("User exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(FileException.class)
    public ResponseEntity<ErrorResponse> handleFileException(FileException ex) {
        log.error("File error: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<ErrorResponse> handleUserServiceException(UserServiceException ex) {
        log.error("User service exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ErrorResponse> handleEncryptionException(EncryptionException ex) {
        log.error("Encryption exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal error occurred while processing your request. Please try again later.");
    }

    // ═══════════════════════════════ VALIDATION EXCEPTIONS ═══════════════════════════════

    /** @Valid on @RequestBody — extracts per-field messages */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("Validation failed: {}", fieldErrors);
        return buildResponseWithFieldErrors(HttpStatus.BAD_REQUEST,
                "Validation failed. Please check the field errors for details.", fieldErrors);
    }

    /** @Validated on path variables / request params */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String field = v.getPropertyPath().toString();
            // strip method name prefix (e.g. "rateTrainer.rating" → "rating")
            if (field.contains(".")) {
                field = field.substring(field.lastIndexOf('.') + 1);
            }
            fieldErrors.put(field, v.getMessage());
        }
        log.warn("Constraint violation: {}", fieldErrors);
        return buildResponseWithFieldErrors(HttpStatus.BAD_REQUEST,
                "Validation failed. Please check the field errors for details.", fieldErrors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointer(NullPointerException ex) {
        // Objects.requireNonNull throws NPE with a descriptive message
        if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
            log.warn("Null value rejected: {}", ex.getMessage());
            return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        log.error("Unexpected NullPointerException", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    // ═══════════════════════════════ AUTH / JWT EXCEPTIONS ═══════════════════════════════

    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(io.jsonwebtoken.ExpiredJwtException ex) {
        log.warn("JWT token expired: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Your session has expired. Please log in again.");
    }

    @ExceptionHandler(io.jsonwebtoken.MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwt(io.jsonwebtoken.MalformedJwtException ex) {
        log.warn("Malformed JWT token: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Invalid authentication token. Please log in again.");
    }

    @ExceptionHandler(io.jsonwebtoken.security.SignatureException.class)
    public ResponseEntity<ErrorResponse> handleJwtSignature(io.jsonwebtoken.security.SignatureException ex) {
        log.warn("JWT signature mismatch: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Invalid authentication token. Please log in again.");
    }

    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(io.jsonwebtoken.JwtException ex) {
        log.warn("JWT error: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Authentication failed. Please log in again.");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.");
    }

    // ═══════════════════════════════ SPRING MVC / HTTP EXCEPTIONS ═══════════════════════════════

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Missing request header: {}", ex.getHeaderName());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Required header '" + ex.getHeaderName() + MISSING);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + MISSING);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        log.warn("Missing request part: {}", ex.getRequestPartName());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Required file part '" + ex.getRequestPartName() + MISSING);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Malformed request body. Please verify the JSON syntax and try again.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getContentType());
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type '" + ex.getContentType() + "' is not supported. Supported types: "
                        + ex.getSupportedMediaTypes());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMethod());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        String paramName = ex.getName();

        Class<?> requiredType = ex.getRequiredType();
        String expectedType = requiredType == null
                ? "unknown"
                : requiredType.getSimpleName();

        log.warn("Type mismatch for parameter '{}': expected {}", paramName, expectedType);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Parameter '" + paramName + "' must be of type " + expectedType + "."
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND,
                "The requested resource was not found.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "The uploaded file exceeds the maximum allowed size. Please upload a smaller file.");
    }

    // ═══════════════════════════════ DOWNSTREAM / FEIGN EXCEPTIONS ═══════════════════════════════

    @ExceptionHandler({
            feign.FeignException.class,
            io.github.resilience4j.circuitbreaker.CallNotPermittedException.class,
            DownstreamServiceException.class,
            java.net.ConnectException.class,
            java.net.SocketTimeoutException.class
    })
    public ResponseEntity<ErrorResponse> handleDownstreamFailures(Exception ex) {
        log.error("Downstream failure: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "A dependent service is currently unavailable. Please try again later.");
    }

    // ═══════════════════════════════ CATCH-ALL ═══════════════════════════════

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred in user-service", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }
}
