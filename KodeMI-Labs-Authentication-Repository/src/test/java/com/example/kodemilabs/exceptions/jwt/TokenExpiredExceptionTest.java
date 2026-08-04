package com.example.kodemilabs.exceptions.jwt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenExpiredExceptionTest {

    @Test
    void constructor_shouldSetMessage() {
        String message = "Token expired";
        TokenExpiredException exception = new TokenExpiredException(message);
        assertEquals(message, exception.getMessage());
    }
}
