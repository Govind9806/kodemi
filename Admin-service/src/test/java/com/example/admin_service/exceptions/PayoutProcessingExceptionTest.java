package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PayoutProcessingExceptionTest {

    // ===== TEST MESSAGE =====
    @Test
    void testExceptionMessage() {
        String message = "Payout processing failed";

        PayoutProcessingException exception =
                new PayoutProcessingException(message);

        assertEquals(message, exception.getMessage());
    }

    // ===== TEST THROW =====
    @Test
    void testThrowException() {
        String message = "Processing error";

        PayoutProcessingException exception = assertThrows(
                PayoutProcessingException.class,
                () -> {
                    throw new PayoutProcessingException(message);
                }
        );

        assertEquals(message, exception.getMessage());
    }

    // ===== TYPE CHECK =====
    @Test
    void testInstanceType() {
        PayoutProcessingException exception =
                new PayoutProcessingException("msg");

        assertTrue(exception instanceof RuntimeException);
    }

    // ===== NULL MESSAGE =====
    @Test
    void testNullMessage() {
        PayoutProcessingException exception =
                new PayoutProcessingException(null);

        assertNull(exception.getMessage());
    }

    // ===== EMPTY MESSAGE =====
    @Test
    void testEmptyMessage() {
        PayoutProcessingException exception =
                new PayoutProcessingException("");

        assertEquals("", exception.getMessage());
    }
}