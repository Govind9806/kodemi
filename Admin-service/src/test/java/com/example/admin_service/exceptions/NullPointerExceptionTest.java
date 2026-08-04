package com.example.admin_service.exceptions;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NullPointerExceptionTest {

    @Test
    void testInvalidTokenException_withNullCause() {
        InvalidTokenException ex = new InvalidTokenException("Invalid Token", null);
        assertEquals("Invalid Token", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void testInvalidTokenException_withExpiredJwtException() {
        ExpiredJwtException cause = new ExpiredJwtException(null, null, "Expired");
        InvalidTokenException ex = new InvalidTokenException("Token expired", cause);
        assertEquals("Token expired", ex.getMessage());
        assertNull(ex.getCause(), "Cause is not stored in current constructor");
    }

    @Test
    void testCustomNullPointerException() {
        com.example.admin_service.exceptions.NullPointerException ex =
                new com.example.admin_service.exceptions.NullPointerException("Null Pointer");
        assertEquals("Null Pointer", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void testInvalidCredentialsException() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid credentials");
        assertEquals("Invalid credentials", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void testNoActiveRequestException() {
        NoActiveRequestException ex = new NoActiveRequestException("No active request");
        assertEquals("No active request", ex.getMessage());
        assertNull(ex.getCause());
    }
}