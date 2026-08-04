package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoActiveRequestExceptionTest {
    @Test
    void testMessageConstructor() {
        NoActiveRequestException ex = new NoActiveRequestException("No Request");
        assertEquals("No Request", ex.getMessage());
    }
}
