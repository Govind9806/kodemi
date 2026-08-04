package com.example.user_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileExceptionTest {

    @Test
    void testFileExceptionMessage() {
        String message = "File not found";
        FileException exception = new FileException(message);

        assertNotNull(exception, "Exception should be instantiated");
        assertEquals(message, exception.getMessage(), "Exception message should match");
    }
}