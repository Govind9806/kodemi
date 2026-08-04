package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DuplicateValueTest {
    @Test
    void testMessageConstructor() {
        DuplicateValue ex = new DuplicateValue("Duplicate");
        assertEquals("Duplicate", ex.getMessage());
    }
}
