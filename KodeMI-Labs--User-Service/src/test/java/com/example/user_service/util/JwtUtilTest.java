package com.example.user_service.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Use a 32-character secret for HS256
    private final String secret = "01234567890123456789012345678901";

    private String token;
    Clock fixedClock = Clock.fixed(
            Instant.parse("2026-01-01T00:00:00Z"),
            ZoneOffset.UTC
    );

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        jwtUtil.secretKeyString = secret;

        // Generate a test JWT token
        token = Jwts.builder()
                .claim("userId", "user123")
                .claim("name", "Test User")
                .claim("role", "ADMIN")
                .setIssuedAt(Date.from(fixedClock.instant()))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();

        // Add "Bearer " prefix to simulate real usage
        token = "Bearer " + token;
    }

    @Test
    void extractUserId_shouldReturnUserId() {
        String userId = jwtUtil.extractUserId(token);
        assertEquals("user123", userId);
    }

    @Test
    void extractName_shouldReturnName() {
        String name = jwtUtil.extractName(token);
        assertEquals("Test User", name);
    }

    @Test
    void extractRole_shouldReturnRole() {
        String role = jwtUtil.extractRole(token);
        assertEquals("ADMIN", role);
    }

    @Test
    void extractUserId_withMalformedToken_shouldThrowException() {
        String badToken = "Bearer invalid.token.value";
        assertThrows(Exception.class, () -> jwtUtil.extractUserId(badToken));
    }
}