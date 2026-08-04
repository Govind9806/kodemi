package com.example.user_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerNotFoundExceptionTest {

    @Test
    void testExceptionWithMessage() {
        String message = "Trainer not found with id: trainer123";
        TrainerNotFoundException exception = new TrainerNotFoundException(message);
        
        assertEquals(message, exception.getMessage());
        assertNotNull(exception);
    }

    @Test
    void testExceptionThrown() {
        assertThrows(TrainerNotFoundException.class, () -> {
            throw new TrainerNotFoundException("Trainer not found");
        });
    }

    @Test
    void testExceptionCaught() {
        try {
            throw new TrainerNotFoundException("Test trainer not found");
        } catch (TrainerNotFoundException e) {
            assertEquals("Test trainer not found", e.getMessage());
        }
    }

    @Test
    void testExceptionIsRuntimeException() {
        TrainerNotFoundException exception = new TrainerNotFoundException("Test");
        assertTrue(exception instanceof RuntimeException);
    }
}
