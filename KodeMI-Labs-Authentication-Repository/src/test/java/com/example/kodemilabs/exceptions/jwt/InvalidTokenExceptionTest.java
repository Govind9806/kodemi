package com.example.kodemilabs.exceptions.jwt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvalidTokenExceptionTest {

    @Test
    void constructor_shouldSetMessage() {
        String message = "Invalid token";
        InvalidTokenException exception = new InvalidTokenException(message);
        assertEquals(message, exception.getMessage());
    }
}
