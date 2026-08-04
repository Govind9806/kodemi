package com.example.kodemilabs.service;

import com.example.kodemilabs.dto.request.UserDTO;
import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserService userService;

    // ✅ getUserByEmail - SUCCESS
    @Test
    void getUserByEmail_shouldReturnDTO_whenUserExists() {
        User user = new User();
        user.setUserId("u1");
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setName("Test");
        user.setRole(Role.LEARNER);
        user.setVerified(true);
        user.setActive(true);
        user.setLastLogin(123L);

        when(userRepo.getUserByEmail("test@example.com")).thenReturn(user);

        UserDTO result = userService.getUserByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("u1", result.getUserId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("testUser", result.getUsername());
        assertEquals(Role.LEARNER, result.getRole());
        assertTrue(result.getIsVerified());
        assertTrue(result.getIsActive());
    }

    // ❌ getUserByEmail - USER NOT FOUND → RETURNS NULL (important behavior)
    @Test
    void getUserByEmail_shouldReturnNull_whenUserNotFound() {
        when(userRepo.getUserByEmail("test@example.com")).thenReturn(null);

        UserDTO result = userService.getUserByEmail("test@example.com");

        assertNull(result);
    }

    // ✅ updateUserRole - SUCCESS
    @Test
    void updateUserRole_shouldUpdateRole_whenUserExists() {
        User user = new User();
        user.setUserId("u1");
        user.setRole(Role.LEARNER);

        when(userRepo.getUserById("u1")).thenReturn(user);

        userService.updateUserRole("u1", "TRAINER");

        assertEquals(Role.TRAINER, user.getRole());
        verify(userRepo).save(user);
    }

    // ❌ updateUserRole - USER NOT FOUND
    @Test
    void updateUserRole_shouldThrow_whenUserNotFound() {
        when(userRepo.getUserById("u1")).thenReturn(null);

        try {
            userService.updateUserRole("u1", "TRAINER");
            fail("Expected UserNotFoundException");
        } catch (UserNotFoundException e) {
            // expected
        }

        verify(userRepo, never()).save(any());
    }

    // ❌ updateUserRole - INVALID ROLE STRING
    @Test
    void updateUserRole_shouldThrow_whenInvalidRole() {
        User user = new User();
        when(userRepo.getUserById("u1")).thenReturn(user);

        try {
            userService.updateUserRole("u1", "INVALID_ROLE");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    // ✅ getUserById - SUCCESS
    @Test
    void getUserById_shouldReturnDTO_whenUserExists() {
        User user = new User();
        user.setUserId("u1");
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setName("Test");
        user.setRole(Role.TRAINER);
        user.setVerified(true);
        user.setActive(false);
        user.setLastLogin(456L);

        when(userRepo.getUserById("u1")).thenReturn(user);

        UserDTO result = userService.getUserById("u1");

        assertNotNull(result);
        assertEquals("u1", result.getUserId());
        assertEquals(Role.TRAINER, result.getRole());
        assertFalse(result.getIsActive());
    }

    // ✅ getUsersByIds - mixed found/not found
    @Test
    void getUsersByIds_shouldReturnOnlyFoundUsers() {
        User user = new User();
        user.setUserId("u1");
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setName("Test");
        user.setRole(Role.LEARNER);
        user.setVerified(true);
        user.setActive(true);

        when(userRepo.findByIds(java.util.List.of("u1", "missing"))).thenReturn(java.util.List.of(user));

        var result = userService.getUsersByIds(java.util.List.of("u1", "missing"));

        assertEquals(1, result.size());
        assertEquals("u1", result.get(0).getUserId());
    }

    // ✅ getUsersByIds - empty list
    @Test
    void getUsersByIds_emptyList_returnsEmpty() {
        var result = userService.getUsersByIds(java.util.List.of());
        assertTrue(result.isEmpty());
    }
}