package com.example.ai_service.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Use a 32-character secret for HS256
    private final String secret = "01234567890123456789012345678901";

    private static final Date FIXED_DATE =
            new Date(1735689600000L); // 2025-01-01 00:00:00 UTC

    private String token;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        jwtUtil.secretKeyString = secret;

        // Generate a test JWT token
        token = Jwts.builder()
                .claim("userId", "user123")
                .claim("name", "Test User")
                .claim("role", "ADMIN")
                .setIssuedAt(FIXED_DATE)
                .signWith(jwtUtil.getKey(), SignatureAlgorithm.HS256)
                .compact();

        // Add Bearer prefix to simulate real usage
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

        assertThrows(
                JwtException.class,
                () -> jwtUtil.extractUserId(badToken)
        );
    }
}