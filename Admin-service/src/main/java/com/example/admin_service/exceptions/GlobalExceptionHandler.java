package com.example.admin_service.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
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

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointers(NullPointerException ex) {
        log.warn("Null Value: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Invalid credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED));
    }

    @ExceptionHandler(NoActiveRequestException.class)
    public ResponseEntity<Map<String, Object>> handleNoRequest(NoActiveRequestException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND));
    }
    
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleOtpInvalidToken(InvalidTokenException ex) {
        log.warn("Invalid OTP token: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }
    
    @ExceptionHandler(FetchPendingPayoutException.class)
    public ResponseEntity<Map<String, Object>> handleFetchPendingPayout(FetchPendingPayoutException ex){
        log.warn("Fetch Failed: {}", ex.getMessage());
        return  ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(InvalidPayoutActionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidPayoutActionException ex){
        log.warn("Wrong Payout Action: {}", ex.getMessage());
        return  ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(PaymentClientException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentClientException(PaymentClientException ex){
        log.warn("Payment Exception: {}", ex.getMessage());
        return  ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(UnauthorizedPayoutAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedPayoutAccessException(UnauthorizedPayoutAccessException ex){
        log.warn("Unauthorized Payout: {}", ex.getMessage());
        return  ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED));
    }

    @ExceptionHandler(PayoutProcessingException.class)
    public ResponseEntity<Map<String, Object>> handlePayoutProcessingException(PayoutProcessingException ex){
        log.warn("Invalid Payout Process: {}", ex.getMessage());
        return  ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler({
        FeignException.class,
        CallNotPermittedException.class,
        DownstreamServiceException.class,
        ConnectException.class,
        SocketTimeoutException.class
    })
    public ResponseEntity<Map<String, Object>> handleDownstreamServiceFailures(Exception ex) {
        log.error("Downstream service failure: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponse("Downstream service is currently unavailable. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("An internal server error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
