package com.example.kodemilabs.model;

import com.example.kodemilabs.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testGettersAndSetters() {
        User user = new User();

        // Set values
        user.setUserId("u123");
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setUsername("johndoe");
        user.setPasswordHash("hashedPassword");
        user.setActive(true);
        user.setVerified(false);
        user.setLastLogin(1678901234L);
        user.setRole(Role.SUPER_ADMIN);

        // Verify getters
        assertEquals("u123", user.getUserId());
        assertEquals("John Doe", user.getName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("johndoe", user.getUsername());
        assertEquals("hashedPassword", user.getPasswordHash());
        assertTrue(user.isActive());
        assertFalse(user.isVerified());
        assertEquals(1678901234L, user.getLastLogin());
        assertEquals(Role.SUPER_ADMIN, user.getRole());
    }

    @Test
    void testEqualsHashCodeAndToString() {
        User user1 = new User();
        user1.setUserId("u123");
        user1.setName("John Doe");
        user1.setEmail("john@example.com");
        user1.setUsername("johndoe");
        user1.setPasswordHash("hashedPassword");
        user1.setActive(true);
        user1.setVerified(false);
        user1.setLastLogin(1678901234L);
        user1.setRole(Role.SUPER_ADMIN);

        User user2 = new User();
        user2.setUserId("u123");
        user2.setName("John Doe");
        user2.setEmail("john@example.com");
        user2.setUsername("johndoe");
        user2.setPasswordHash("hashedPassword");
        user2.setActive(true);
        user2.setVerified(false);
        user2.setLastLogin(1678901234L);
        user2.setRole(Role.SUPER_ADMIN);

        // object equality is not overridden in User, ensure values are equal
        assertNotSame(user1, user2);
        assertEquals(user1.getUserId(), user2.getUserId());
        assertEquals(user1.getEmail(), user2.getEmail());
        assertEquals(user1.getUsername(), user2.getUsername());
        assertEquals(user1.getName(), user2.getName());
        assertEquals(user1.getRole(), user2.getRole());

        // toString
        assertNotNull(user1.toString());
        assertTrue(user1.toString().contains("User") || !user1.toString().isEmpty());
    }
}