package com.example.kodemilabs.service;

import com.example.kodemilabs.exceptions.login.UserNotFoundException;
import com.example.kodemilabs.exceptions.otp.TokenExpiredException;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.UserRepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Mock private OTPService otpService;
    @Mock private UserRepo userRepo;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ================= REQUEST RESET =================
    @Test
    void requestResetPasswordOtp_success() {
        passwordResetService.requestResetPasswordOtp("test@mail.com");

        verify(otpService).generateResetPasswordOtp("test@mail.com");
    }

    @Test
    void requestResetPasswordOtp_userNotFound_shouldNotThrow() {
        doThrow(new UserNotFoundException("User not found"))
                .when(otpService).generateResetPasswordOtp(any());

        assertDoesNotThrow(() ->
                passwordResetService.requestResetPasswordOtp("test@mail.com"));
    }

    // ================= VERIFY OTP =================
    @Test
    void verifyResetPasswordOtp_success() {
        String email = "test@mail.com";

        String token = passwordResetService.verifyResetPasswordOtp(email, "123456");

        assertNotNull(token);
        assertTrue(passwordResetService.resetTokens.containsKey(token));
        assertEquals(email, passwordResetService.resetTokens.get(token));

        verify(otpService).verifyResetPasswordOtp(email, "123456");
    }

    // ================= RESET PASSWORD =================
    @Test
    void resetPassword_success() {
        String email = "test@mail.com";
        String token = "token123";

        User user = new User();
        user.setEmail(email);

        passwordResetService.resetTokens.put(token, email);

        when(userRepo.getUserByEmail(email)).thenReturn(user);

        passwordResetService.resetPassword(token, "Password@123");

        assertNotNull(user.getPasswordHash());
        assertTrue(BCrypt.checkpw("Password@123", user.getPasswordHash()));

        verify(userRepo).save(user);
        assertFalse(passwordResetService.resetTokens.containsKey(token));
    }

    @Test
    void resetPassword_invalidToken_shouldThrow() {
        assertThrows(TokenExpiredException.class,
                () -> passwordResetService.resetPassword("invalid", "Password@123"));
    }

    @Test
    void resetPassword_userNotFound_shouldThrow() {
        String token = "token123";
        passwordResetService.resetTokens.put(token, "test@mail.com");

        when(userRepo.getUserByEmail(any())).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> passwordResetService.resetPassword(token, "Password@123"));
    }

    // ================= PASSWORD VALIDATION =================
    @Test
    void resetPassword_invalidPassword_shouldThrow() {
        String token = "token123";
        passwordResetService.resetTokens.put(token, "test@mail.com");

        User user = new User();
        user.setEmail("test@mail.com");

        when(userRepo.getUserByEmail(any())).thenReturn(user);

        // invalid password (depends on your PasswordValidator rules)
        assertThrows(Exception.class,
                () -> passwordResetService.resetPassword(token, "123"));
    }

    // ================= CLEANUP =================
    @Test
    void shutdownScheduler_shouldNotThrow() {
        assertDoesNotThrow(() -> passwordResetService.shutdownScheduler());
    }
}