package com.example.user_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptionExceptionTest {

    @Test
    void testExceptionWithMessage() {
        String message = "Error encrypting data";
        EncryptionException exception = new EncryptionException(message);
        
        assertEquals(message, exception.getMessage());
        assertNotNull(exception);
    }

    @Test
    void testExceptionThrown() {
        assertThrows(EncryptionException.class, () -> {
            throw new EncryptionException("Encryption failed");
        });
    }

    @Test
    void testExceptionCaught() {
        try {
            throw new EncryptionException("Test encryption error");
        } catch (EncryptionException e) {
            assertEquals("Test encryption error", e.getMessage());
        }
    }

    @Test
    void testExceptionIsRuntimeException() {
        EncryptionException exception = new EncryptionException("Test");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testDecryptionError() {
        String message = "Error decrypting data";
        EncryptionException exception = new EncryptionException(message);
        
        assertEquals(message, exception.getMessage());
    }
}
