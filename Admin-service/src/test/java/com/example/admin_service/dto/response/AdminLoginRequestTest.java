package com.example.admin_service.dto.response;

import com.example.admin_service.enums.AdminRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AdminLoginRequestTest {

    @Test
    void testGettersAndSetters() {
        AdminLoginRequest dto = new AdminLoginRequest();
        dto.setAdminId("a1");
        dto.setEmail("test@email.com");
        dto.setUsername("testuser");
        dto.setAdminRole(AdminRole.USER_ADMIN);

        assertEquals("a1", dto.getAdminId());
        assertEquals("test@email.com", dto.getEmail());
        assertEquals("testuser", dto.getUsername());
        assertEquals(AdminRole.USER_ADMIN, dto.getAdminRole());
    }
}
