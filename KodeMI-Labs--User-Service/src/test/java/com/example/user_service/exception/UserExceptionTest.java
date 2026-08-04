package com.example.user_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserExceptionTest {

    @Test
    void testExceptionWithMessage() {
        String message = "User error occurred";
        UserException exception = new UserException(message);
        
        assertEquals(message, exception.getMessage());
        assertNotNull(exception);
    }

    @Test
    void testExceptionThrown() {
        assertThrows(UserException.class, () -> {
            throw new UserException("User validation failed");
        });
    }

    @Test
    void testExceptionCaught() {
        try {
            throw new UserException("Test user exception");
        } catch (UserException e) {
            assertEquals("Test user exception", e.getMessage());
        }
    }

    @Test
    void testExceptionIsRuntimeException() {
        UserException exception = new UserException("Test");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testUserIdEmptyMessage() {
        String message = "UserId cannot be empty";
        UserException exception = new UserException(message);
        
        assertEquals(message, exception.getMessage());
    }
}
