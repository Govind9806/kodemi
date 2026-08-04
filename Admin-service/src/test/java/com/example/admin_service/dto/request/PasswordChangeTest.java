package com.example.admin_service.dto.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordChangeTest {

    @Test
    void testGettersAndSetters() {
        PasswordChange dto = new PasswordChange();
        dto.setPassword("newPass123");

        assertEquals("newPass123", dto.getPassword());
    }
}
