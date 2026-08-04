package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidCredentialsExceptionTest {

    @Test
    void testMessageConstructor() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid Credentials");

        // Check message
        assertEquals("Invalid Credentials", ex.getMessage());

        // Check that cause is null
        assertNull(ex.getCause(), "Cause should be null for this constructor");
    }
}