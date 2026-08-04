package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidPayoutActionExceptionTest {

    // ===== TEST MESSAGE =====
    @Test
    void testExceptionMessage() {
        String message = "Invalid action provided";

        InvalidPayoutActionException exception =
                new InvalidPayoutActionException(message);

        assertEquals(message, exception.getMessage());
    }

    // ===== TEST THROWING EXCEPTION =====
    @Test
    void testThrowException() {
        String message = "Action not allowed";

        InvalidPayoutActionException exception = assertThrows(
                InvalidPayoutActionException.class,
                () -> {
                    throw new InvalidPayoutActionException(message);
                }
        );

        assertEquals(message, exception.getMessage());
    }

    // ===== TEST INSTANCE TYPE =====
    @Test
    void testInstanceType() {
        InvalidPayoutActionException exception =
                new InvalidPayoutActionException("msg");

        assertTrue(exception instanceof RuntimeException);
    }

    // ===== EDGE CASE: NULL MESSAGE =====
    @Test
    void testNullMessage() {
        InvalidPayoutActionException exception =
                new InvalidPayoutActionException(null);

        assertNull(exception.getMessage());
    }

    // ===== EDGE CASE: EMPTY MESSAGE =====
    @Test
    void testEmptyMessage() {
        InvalidPayoutActionException exception =
                new InvalidPayoutActionException("");

        assertEquals("", exception.getMessage());
    }
}