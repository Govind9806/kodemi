package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ===== NULL POINTER =====
    @Test
    void testHandleNullPointers_fullCoverage() {
        NullPointerException ex = new NullPointerException("Null error");

        ResponseEntity<Map<String, Object>> response =
                handler.handleNullPointers(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals(HttpStatus.NOT_FOUND.value(), body.get("status"));
        assertEquals("Null error", body.get("message"));
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), body.get("error"));
        assertNotNull(body.get("timestamp"));
    }

    // ===== INVALID CREDENTIALS =====
    @Test
    void testHandleInvalidCredentials_fullCoverage() {
        InvalidCredentialsException ex =
                new InvalidCredentialsException("Invalid credentials");

        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.get("status"));
        assertEquals("Invalid credentials", body.get("message"));
        assertEquals(HttpStatus.UNAUTHORIZED.getReasonPhrase(), body.get("error"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleInvalidCredentials_nullMessage() {
        InvalidCredentialsException ex =
                new InvalidCredentialsException(null);

        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertNull(body.get("message"));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.get("status"));
        assertEquals(HttpStatus.UNAUTHORIZED.getReasonPhrase(), body.get("error"));
        assertNotNull(body.get("timestamp"));
    }

    // ===== NO ACTIVE REQUEST =====
    @Test
    void testHandleNoRequest_fullCoverage() {
        NoActiveRequestException ex =
                new NoActiveRequestException("No active request");

        ResponseEntity<Map<String, Object>> response =
                handler.handleNoRequest(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals(HttpStatus.NOT_FOUND.value(), body.get("status"));
        assertEquals("No active request", body.get("message"));
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), body.get("error"));
        assertNotNull(body.get("timestamp"));
    }

    // ===== INVALID TOKEN =====
    @Test
    void testHandleInvalidToken_fullCoverage() {
        InvalidTokenException ex =
                new InvalidTokenException("Invalid token", null);

        ResponseEntity<Map<String, Object>> response =
                handler.handleOtpInvalidToken(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
        assertEquals("Invalid token", body.get("message"));
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), body.get("error"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleInvalidToken_nullMessage() {
        InvalidTokenException ex =
                new InvalidTokenException(null, null);

        ResponseEntity<Map<String, Object>> response =
                handler.handleOtpInvalidToken(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertNull(body.get("message"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), body.get("error"));
        assertNotNull(body.get("timestamp"));
    }
    @Test
    void testHandleFetchPendingPayout_fullCoverage() {
        FetchPendingPayoutException ex =
                new FetchPendingPayoutException("Fetch failed");

        ResponseEntity<Map<String, Object>> response =
                handler.handleFetchPendingPayout(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
        assertEquals("Fetch failed", body.get("message"));
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), body.get("error"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleFetchPendingPayout_nullMessage() {
        FetchPendingPayoutException ex =
                new FetchPendingPayoutException(null);

        ResponseEntity<Map<String, Object>> response =
                handler.handleFetchPendingPayout(ex);

        assertNull(response.getBody().get("message"));
    }

    @Test
    void testHandleInvalidPayoutAction_fullCoverage() {
        InvalidPayoutActionException ex =
                new InvalidPayoutActionException("Wrong action");

        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidCredentials(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals("Wrong action", body.get("message"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleInvalidPayoutAction_nullMessage() {
        InvalidPayoutActionException ex =
                new InvalidPayoutActionException(null);

        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidCredentials(ex);

        assertNull(response.getBody().get("message"));
    }

    @Test
    void testHandlePaymentClientException_fullCoverage() {
        PaymentClientException ex =
                new PaymentClientException("Payment failed");

        ResponseEntity<Map<String, Object>> response =
                handler.handlePaymentClientException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals("Payment failed", body.get("message"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandlePaymentClientException_nullMessage() {
        PaymentClientException ex =
                new PaymentClientException(null);

        ResponseEntity<Map<String, Object>> response =
                handler.handlePaymentClientException(ex);

        assertNull(response.getBody().get("message"));
    }

    @Test
    void testHandleUnauthorizedPayoutAccess_fullCoverage() {
        UnauthorizedPayoutAccessException ex =
                new UnauthorizedPayoutAccessException("Unauthorized");

        ResponseEntity<Map<String, Object>> response =
                handler.handleUnauthorizedPayoutAccessException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals("Unauthorized", body.get("message"));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.get("status"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleUnauthorizedPayoutAccess_nullMessage() {
        UnauthorizedPayoutAccessException ex =
                new UnauthorizedPayoutAccessException(null);

        ResponseEntity<Map<String, Object>> response =
                handler.handleUnauthorizedPayoutAccessException(ex);

        assertNull(response.getBody().get("message"));
    }

    @Test
    void testHandlePayoutProcessingException_fullCoverage() {
        PayoutProcessingException ex =
                new PayoutProcessingException("Processing failed");

        ResponseEntity<Map<String, Object>> response =
                handler.handlePayoutProcessingException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals("Processing failed", body.get("message"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandlePayoutProcessingException_nullMessage() {
        PayoutProcessingException ex =
                new PayoutProcessingException(null);

        ResponseEntity<Map<String, Object>> response =
                handler.handlePayoutProcessingException(ex);

        assertNull(response.getBody().get("message"));
    }
}