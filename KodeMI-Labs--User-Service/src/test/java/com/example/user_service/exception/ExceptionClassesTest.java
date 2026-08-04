package com.example.user_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionClassesTest {

    @Test
    void learnerNotFoundException_message() {
        LearnerNotFoundException ex = new LearnerNotFoundException("Learner not found");
        assertTrue(ex.getMessage().contains("Learner not found"));
    }

    @Test
    void trainerNotFoundException_message() {
        TrainerNotFoundException ex = new TrainerNotFoundException("Trainer not found");
        assertEquals("Trainer not found", ex.getMessage());
    }

    @Test
    void fileException_message() {
        FileException ex = new FileException("File error");
        assertEquals("File error", ex.getMessage());
    }

    @Test
    void encryptionException_message() {
        EncryptionException ex = new EncryptionException("Encryption failed");
        assertEquals("Encryption failed", ex.getMessage());
    }

    @Test
    void userException_message() {
        UserException ex = new UserException("User error");
        assertEquals("User error", ex.getMessage());
    }

    @Test
    void userServiceException_message() {
        UserServiceException ex = new UserServiceException("Service error");
        assertEquals("Service error", ex.getMessage());
    }

    @Test
    void fileNotFoundException_message() {
        FileNotFoundException ex = new FileNotFoundException("File not found");
        assertEquals("File not found", ex.getMessage());
    }

    @Test
    void noActiveRequestException_message() {
        NoActiveRequestException ex = new NoActiveRequestException("No active request");
        assertEquals("No active request", ex.getMessage());
    }

    @Test
    void exceptions_areRuntimeExceptions() {
        assertInstanceOf(RuntimeException.class, new LearnerNotFoundException("x"));
        assertInstanceOf(RuntimeException.class, new TrainerNotFoundException("x"));
        assertInstanceOf(RuntimeException.class, new FileException("x"));
        assertInstanceOf(RuntimeException.class, new EncryptionException("x"));
        assertInstanceOf(RuntimeException.class, new UserException("x"));
        assertInstanceOf(RuntimeException.class, new UserServiceException("x"));
        assertInstanceOf(RuntimeException.class, new FileNotFoundException("x"));
        assertInstanceOf(RuntimeException.class, new NoActiveRequestException("x"));
    }
}
