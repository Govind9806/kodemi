package com.example.admin_service.model;

import com.example.admin_service.enums.AdminRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AdminTest {

    @Test
    void testAdminGettersAndSetters() {
        Admin admin = new Admin();
        admin.setAdminId("admin123");
        admin.setPassword("pass123");
        admin.setEmail("admin@domain.com");
        admin.setUsername("adminUser");
        admin.setAdminRole(null); 
        admin.setPending(true);

        assertEquals("admin123", admin.getAdminId());
        assertEquals("pass123", admin.getPassword());
        assertEquals("admin@domain.com", admin.getEmail());
        assertEquals("adminUser", admin.getUsername());
        assertNull(admin.getAdminRole());
        assertTrue(admin.isPending());
    }

    @Test
    void testAdminAllArgsConstructor() {
        Admin admin = new Admin("a1", "p1", "e1", "u1", null, false);
        assertEquals("a1", admin.getAdminId());
        assertEquals("p1", admin.getPassword());
        assertEquals("e1", admin.getEmail());
        assertEquals("u1", admin.getUsername());
        assertFalse(admin.isPending());
    }

    @Test
    void testAdminBuilder() {
        Admin admin = Admin.builder()
                .adminId("a2")
                .password("p2")
                .email("e2.com")
                .username("u2")
                .adminRole(null)
                .pending(true)
                .build();

        assertEquals("a2", admin.getAdminId());
        assertEquals("p2", admin.getPassword());
        assertEquals("e2.com", admin.getEmail());
        assertEquals("u2", admin.getUsername());
        assertTrue(admin.isPending());
    }
}
