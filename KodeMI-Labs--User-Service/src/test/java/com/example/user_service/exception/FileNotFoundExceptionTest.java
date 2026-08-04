package com.example.user_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileNotFoundExceptionTest {

    @Test
    void testExceptionWithMessage() {
        String message = "File not found: profile.jpg";
        FileNotFoundException exception = new FileNotFoundException(message);
        
        assertEquals(message, exception.getMessage());
        assertNotNull(exception);
    }

    @Test
    void testExceptionThrown() {
        assertThrows(FileNotFoundException.class, () -> {
            throw new FileNotFoundException("File not found");
        });
    }

    @Test
    void testExceptionCaught() {
        try {
            throw new FileNotFoundException("Test file not found");
        } catch (FileNotFoundException e) {
            assertEquals("Test file not found", e.getMessage());
        }
    }

    @Test
    void testExceptionIsRuntimeException() {
        FileNotFoundException exception = new FileNotFoundException("Test");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testEmptyFileMessage() {
        String message = "Empty File";
        FileNotFoundException exception = new FileNotFoundException(message);
        
        assertEquals(message, exception.getMessage());
    }
}
