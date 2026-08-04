package com.example.course_service.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm";

    @BeforeEach
    void setup() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKeyString", SECRET);
    }

    private String buildToken(String userId, String name, String role) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("name", name);
        claims.put("role", role);

        return "Bearer " + Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void extractUserId_ReturnsCorrectId() {
        String token = buildToken("user-123", "John Doe", "TRAINER");

        String userId = jwtUtil.extractUserId(token);

        assertEquals("user-123", userId);
    }

    @Test
    void extractName_ReturnsCorrectName() {
        String token = buildToken("user-123", "John Doe", "TRAINER");

        String name = jwtUtil.extractName(token);

        assertEquals("John Doe", name);
    }

    @Test
    void extractRole_ReturnsCorrectRole() {
        String token = buildToken("user-123", "John Doe", "ADMIN");

        String role = jwtUtil.extractRole(token);

        assertEquals("ADMIN", role);
    }
}
