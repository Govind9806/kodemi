package com.example.user_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NoActiveRequestExceptionTest {

    @Test
    void testNoActiveRequestExceptionMessage() {
        String message = "No active request found";
        NoActiveRequestException exception = new NoActiveRequestException(message);

        assertNotNull(exception, "Exception should be instantiated");
        assertEquals(message, exception.getMessage(), "Exception message should match");
    }
}