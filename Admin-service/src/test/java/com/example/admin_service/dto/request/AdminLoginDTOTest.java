package com.example.admin_service.dto.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AdminLoginDTOTest {

    @Test
    void testGettersAndSetters() {
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setEmail("admin@domain.com");
        dto.setPassword("pass123");

        assertEquals("admin@domain.com", dto.getEmail());
        assertEquals("pass123", dto.getPassword());
    }
}
