package com.example.user_service.commondto;

import com.example.user_service.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDTOTest {

    @Test
    void testBuilder() {
        UserDTO dto = UserDTO.builder()
                .userId("user123")
                .name("John Doe")
                .email("john@example.com")
                .username("johndoe")
                .isActive(true)
                .isVerified(true)
                .lastLogin(1234567890L)
                .role(Role.LEARNER)
                .build();

        assertEquals("user123", dto.getUserId());
        assertEquals("John Doe", dto.getName());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("johndoe", dto.getUsername());
        assertTrue(dto.getIsActive());
        assertTrue(dto.getIsVerified());
        assertEquals(1234567890L, dto.getLastLogin());
        assertEquals(Role.LEARNER, dto.getRole());
    }

    @Test
    void testNoArgsConstructor() {
        UserDTO dto = new UserDTO();
        assertNotNull(dto);
    }

    @Test
    void testAllArgsConstructor() {
        UserDTO dto = new UserDTO(
                "user456",
                "Jane Smith",
                "jane@example.com",
                "janesmith",
                true,
                false,
                9876543210L,
                Role.SUPER_ADMIN,
                "ACTIVE"
        );

        assertEquals("user456", dto.getUserId());
        assertEquals("Jane Smith", dto.getName());
        assertEquals("jane@example.com", dto.getEmail());
        assertEquals("janesmith", dto.getUsername());
        assertTrue(dto.getIsActive());
        assertFalse(dto.getIsVerified());
        assertEquals(9876543210L, dto.getLastLogin());
        assertEquals(Role.SUPER_ADMIN, dto.getRole());
    }

    @Test
    void testSettersAndGetters() {
        UserDTO dto = new UserDTO();
        
        dto.setUserId("user789");
        dto.setName("Test User");
        dto.setEmail("test@example.com");
        dto.setUsername("testuser");
        dto.setIsActive(false);
        dto.setIsVerified(true);
        dto.setLastLogin(5555555555L);
        dto.setRole(Role.INSTRUCTOR);

        assertEquals("user789", dto.getUserId());
        assertEquals("Test User", dto.getName());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("testuser", dto.getUsername());
        assertFalse(dto.getIsActive());
        assertTrue(dto.getIsVerified());
        assertEquals(5555555555L, dto.getLastLogin());
        assertEquals(Role.INSTRUCTOR, dto.getRole());
    }

    @Test
    void testAllRoles() {
        UserDTO learner = UserDTO.builder().role(Role.LEARNER).build();
        assertEquals(Role.LEARNER, learner.getRole());
        
        UserDTO admin = UserDTO.builder().role(Role.SUPER_ADMIN).build();
        assertEquals(Role.SUPER_ADMIN, admin.getRole());
        
        UserDTO instructor = UserDTO.builder().role(Role.INSTRUCTOR).build();
        assertEquals(Role.INSTRUCTOR, instructor.getRole());
    }

    @Test
    void testNullValues() {
        UserDTO dto = UserDTO.builder().build();
        
        assertNull(dto.getUserId());
        assertNull(dto.getName());
        assertNull(dto.getEmail());
        assertNull(dto.getUsername());
        assertNull(dto.getIsActive());
        assertNull(dto.getIsVerified());
        assertNull(dto.getLastLogin());
        assertNull(dto.getRole());
    }

    @Test
    void testBooleanFields() {
        UserDTO dto = new UserDTO();
        
        dto.setIsActive(true);
        assertTrue(dto.getIsActive());
        
        dto.setIsActive(false);
        assertFalse(dto.getIsActive());
        
        dto.setIsVerified(true);
        assertTrue(dto.getIsVerified());
        
        dto.setIsVerified(false);
        assertFalse(dto.getIsVerified());
    }
}
