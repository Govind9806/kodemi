package com.example.admin_service.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdminNotFoundExceptionTest {
    @Test
    void testMessageConstructor() {
        AdminNotFoundException ex = new AdminNotFoundException("Not found");
        assertEquals("Not found", ex.getMessage());
    }

    @Test
    void testAdminIdMessageConstructor() {
        AdminNotFoundException ex = new AdminNotFoundException("a1", "Not found");
        assertEquals("Admin not found with adminId: a1. Not found", ex.getMessage());
    }
}
