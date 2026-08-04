package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentClientExceptionTest {

    // ===== TEST MESSAGE =====
    @Test
    void testExceptionMessage() {
        String message = "Payment service failed";

        PaymentClientException exception =
                new PaymentClientException(message);

        assertEquals(message, exception.getMessage());
    }

    // ===== TEST THROWING EXCEPTION =====
    @Test
    void testThrowException() {
        String message = "Client error";

        PaymentClientException exception = assertThrows(
                PaymentClientException.class,
                () -> {
                    throw new PaymentClientException(message);
                }
        );

        assertEquals(message, exception.getMessage());
    }

    // ===== TEST INSTANCE TYPE =====
    @Test
    void testInstanceType() {
        PaymentClientException exception =
                new PaymentClientException("msg");

        assertTrue(exception instanceof RuntimeException);
    }

    // ===== EDGE CASE: NULL MESSAGE =====
    @Test
    void testNullMessage() {
        PaymentClientException exception =
                new PaymentClientException(null);

        assertNull(exception.getMessage());
    }

    // ===== EDGE CASE: EMPTY MESSAGE =====
    @Test
    void testEmptyMessage() {
        PaymentClientException exception =
                new PaymentClientException("");

        assertEquals("", exception.getMessage());
    }
}