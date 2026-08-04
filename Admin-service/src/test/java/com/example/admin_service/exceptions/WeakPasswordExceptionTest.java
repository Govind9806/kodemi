package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WeakPasswordExceptionTest {
    @Test
    void testMessageConstructor() {
        WeakPasswordException ex = new WeakPasswordException("Weak Password");
        assertEquals("Weak Password", ex.getMessage());
    }
}
