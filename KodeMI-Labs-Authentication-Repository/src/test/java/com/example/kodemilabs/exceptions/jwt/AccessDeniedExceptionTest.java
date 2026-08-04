package com.example.kodemilabs.exceptions.jwt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessDeniedExceptionTest {

    @Test
    void constructor_shouldSetMessage() {
        String message = "Access denied";
        AccessDeniedException exception = new AccessDeniedException(message);
        assertEquals(message, exception.getMessage());
    }
}
