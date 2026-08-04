package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnauthorizedPayoutAccessExceptionTest {

    // ===== TEST MESSAGE =====
    @Test
    void testExceptionMessage() {
        String message = "Unauthorized access";

        UnauthorizedPayoutAccessException exception =
                new UnauthorizedPayoutAccessException(message);

        assertEquals(message, exception.getMessage());
    }

    // ===== TEST THROW =====
    @Test
    void testThrowException() {
        String message = "Access denied";

        UnauthorizedPayoutAccessException exception = assertThrows(
                UnauthorizedPayoutAccessException.class,
                () -> {
                    throw new UnauthorizedPayoutAccessException(message);
                }
        );

        assertEquals(message, exception.getMessage());
    }

    // ===== TYPE CHECK =====
    @Test
    void testInstanceType() {
        UnauthorizedPayoutAccessException exception =
                new UnauthorizedPayoutAccessException("msg");

        assertTrue(exception instanceof RuntimeException);
    }

    // ===== NULL MESSAGE =====
    @Test
    void testNullMessage() {
        UnauthorizedPayoutAccessException exception =
                new UnauthorizedPayoutAccessException(null);

        assertNull(exception.getMessage());
    }

    // ===== EMPTY MESSAGE =====
    @Test
    void testEmptyMessage() {
        UnauthorizedPayoutAccessException exception =
                new UnauthorizedPayoutAccessException("");

        assertEquals("", exception.getMessage());
    }
}