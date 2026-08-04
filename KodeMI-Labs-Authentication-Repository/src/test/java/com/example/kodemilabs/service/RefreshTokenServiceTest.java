package com.example.kodemilabs.service;

import com.example.kodemilabs.enums.Role;
import com.example.kodemilabs.exceptions.otp.InvalidTokenException;
import com.example.kodemilabs.exceptions.otp.TokenExpiredException;
import com.example.kodemilabs.model.RefreshToken;
import com.example.kodemilabs.model.User;
import com.example.kodemilabs.repository.RefreshTokenRepo;
import com.example.kodemilabs.repository.UserRepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepo refreshTokenRepo;
    @Mock private JwtService jwtService;
    @Mock private UserRepo userRepo;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private RefreshToken validToken;
    private User user;

    @BeforeEach
    void setUp() {
        validToken = new RefreshToken();
        validToken.setToken("valid-token");
        validToken.setEmail("test@example.com");
        validToken.setExpiry(Instant.parse("2099-01-01T00:00:00Z").getEpochSecond());
        validToken.setInvalidated(false);

        user = new User();
        user.setUserId("user123");
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setRole(Role.LEARNER);
    }

    // ✅ SUCCESS CASE
    @Test
    void refreshAccessToken_shouldReturnNewTokens_whenValid() {
        when(refreshTokenRepo.find("valid-token")).thenReturn(validToken);
        when(userRepo.getUserByEmail("test@example.com")).thenReturn(user);
        when(jwtService.generateToken(any(), any(), any(), any()))
                .thenReturn("new-jwt");

        Map<String, String> result =
                refreshTokenService.refreshAccessToken("valid-token");

        assertNotNull(result);
        assertEquals("new-jwt", result.get("accessToken"));
        assertNotNull(result.get("refreshToken"));

        verify(refreshTokenRepo).save(validToken);
        verify(refreshTokenRepo, times(2)).save(any());
    }


    @Test
    void refreshAccessToken_shouldThrowInvalidToken_whenTokenNotFound() {
        when(refreshTokenRepo.find("invalid")).thenReturn(null);

        assertThrows(InvalidTokenException.class,
                () -> refreshTokenService.refreshAccessToken("invalid"));

        verify(refreshTokenRepo, never()).save(any());
    }

    @Test
    void refreshAccessToken_shouldThrowTokenExpired_whenExpired() {
        validToken.setExpiry(Instant.parse("2000-01-01T00:00:00Z").getEpochSecond());

        when(refreshTokenRepo.find("valid-token")).thenReturn(validToken);

        assertThrows(TokenExpiredException.class,
                () -> refreshTokenService.refreshAccessToken("valid-token"));

        verify(refreshTokenRepo, never()).save(any());
    }

    @Test
    void refreshAccessToken_shouldRevokeAllSessions_whenTokenReused() {
        validToken.setInvalidated(true);

        when(refreshTokenRepo.find("valid-token")).thenReturn(validToken);

        assertThrows(InvalidTokenException.class,
                () -> refreshTokenService.refreshAccessToken("valid-token"));

        verify(refreshTokenRepo).deleteAllByEmail("test@example.com");
    }

    @Test
    void refreshAccessToken_shouldThrowInvalidToken_whenUserNotFound() {
        when(refreshTokenRepo.find("valid-token")).thenReturn(validToken);
        when(userRepo.getUserByEmail("test@example.com")).thenReturn(null);

        assertThrows(InvalidTokenException.class,
                () -> refreshTokenService.refreshAccessToken("valid-token"));
    }

    @Test
    void refreshAccessToken_shouldUseDefaultRole_whenRoleIsNull() {
        user.setRole(null);

        when(refreshTokenRepo.find("valid-token")).thenReturn(validToken);
        when(userRepo.getUserByEmail("test@example.com")).thenReturn(user);
        when(jwtService.generateToken(any(), any(), any(), any()))
                .thenReturn("jwt");

        Map<String, String> result =
                refreshTokenService.refreshAccessToken("valid-token");

        assertEquals("jwt", result.get("accessToken"));

        verify(jwtService).generateToken(
                "user123",
                "test@example.com",
                Role.LEARNER.name(),
                "testUser"
        );
    }
}