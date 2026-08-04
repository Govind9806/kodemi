package com.example.user_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceExceptionTest {

    @Test
    void testExceptionWithMessage() {
        String message = "Service error occurred";

        UserServiceException exception = new UserServiceException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testExceptionThrown() {
        assertThrows(UserServiceException.class,
                () -> { throw new UserServiceException("Service failed"); });
    }

    @Test
    void testExceptionCaught() {
        try {
            throw new UserServiceException("Test exception");
        } catch (UserServiceException e) {
            assertEquals("Test exception", e.getMessage());
        }
    }

    @Test
    void testExceptionIsRuntimeException() {
        UserServiceException exception = new UserServiceException("Test");

        assertTrue(exception instanceof RuntimeException);
    }
}