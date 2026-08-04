package com.example.admin_service.dto.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserDTOTest {

    @Test
    void testGettersAndSetters() {
        UserDTO dto = new UserDTO();
        dto.setUserId("user123");
        dto.setName("John Doe");
        dto.setEmail("john@example.com");
        dto.setUsername("johnd");
        dto.setIsActive(true);
        dto.setIsVerified(false);
        dto.setLastLogin(1622540000000L);
        dto.setRole(null); // Assuming com.example.admin_service.enums.Role is used but we set null

        assertEquals("user123", dto.getUserId());
        assertEquals("John Doe", dto.getName());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("johnd", dto.getUsername());
        assertTrue(dto.getIsActive());
        assertFalse(dto.getIsVerified());
        assertEquals(1622540000000L, dto.getLastLogin());
        assertNull(dto.getRole());
    }
}
