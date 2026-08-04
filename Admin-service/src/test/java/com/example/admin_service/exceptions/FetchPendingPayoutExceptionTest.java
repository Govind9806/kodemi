package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FetchPendingPayoutExceptionTest {

    // ===== TEST MESSAGE =====
    @Test
    void testExceptionMessage() {
        String message = "Failed to fetch payouts";

        FetchPendingPayoutException exception =
                new FetchPendingPayoutException(message);

        assertEquals(message, exception.getMessage());
    }

    // ===== TEST THROWING EXCEPTION =====
    @Test
    void testThrowException() {
        String message = "Error occurred";

        FetchPendingPayoutException exception = assertThrows(
                FetchPendingPayoutException.class,
                () -> {
                    throw new FetchPendingPayoutException(message);
                }
        );

        assertEquals(message, exception.getMessage());
    }

    // ===== TEST INSTANCE TYPE =====
    @Test
    void testInstanceType() {
        FetchPendingPayoutException exception =
                new FetchPendingPayoutException("msg");

        assertTrue(exception instanceof RuntimeException);
    }

    // ===== EDGE CASE: NULL MESSAGE =====
    @Test
    void testNullMessage() {
        FetchPendingPayoutException exception =
                new FetchPendingPayoutException(null);

        assertNull(exception.getMessage());
    }

    // ===== EDGE CASE: EMPTY MESSAGE =====
    @Test
    void testEmptyMessage() {
        FetchPendingPayoutException exception =
                new FetchPendingPayoutException("");

        assertEquals("", exception.getMessage());
    }
}