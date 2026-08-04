package com.example.user_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearnerNotFoundExceptionTest {

    @Test
    void testExceptionWithMessage() {
        String userId = "learner123";
        LearnerNotFoundException exception = new LearnerNotFoundException(userId);
        
        assertEquals("User profile not found for userId: learner123", exception.getMessage());
        assertNotNull(exception);
    }

    @Test
    void testExceptionThrown() {
        assertThrows(LearnerNotFoundException.class, () -> {
            throw new LearnerNotFoundException("learner456");
        });
    }

    @Test
    void testExceptionCaught() {
        try {
            throw new LearnerNotFoundException("learner789");
        } catch (LearnerNotFoundException e) {
            assertEquals("User profile not found for userId: learner789", e.getMessage());
        }
    }

    @Test
    void testExceptionIsRuntimeException() {
        LearnerNotFoundException exception = new LearnerNotFoundException("test");
        assertTrue(exception instanceof RuntimeException);
    }
}
