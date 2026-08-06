package com.example.course_service.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm";
    private static final String USER_ID = "user-123";
    private static final String USER_NAME = "John Doe";
    private static final String ROLE_TRAINER = "TRAINER";

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
                .claims(claims)
                .subject(userId)
                .issuedAt(Date.from(Instant.ofEpochMilli(1000000000000L)))
                .expiration(Date.from(Instant.ofEpochMilli(2000000000000L)))
                .signWith(key)
                .compact();
    }

    @Test
    void extractUserId_ReturnsCorrectId() {
        String token = buildToken(USER_ID, USER_NAME, ROLE_TRAINER);

        String userId = jwtUtil.extractUserId(token);

        assertEquals(USER_ID, userId);
    }

    @Test
    void extractName_ReturnsCorrectName() {
        String token = buildToken(USER_ID, USER_NAME, ROLE_TRAINER);

        String name = jwtUtil.extractName(token);

        assertEquals(USER_NAME, name);
    }

    @Test
    void extractRole_ReturnsCorrectRole() {
        String token = buildToken(USER_ID, USER_NAME, "ADMIN");

        String role = jwtUtil.extractRole(token);

        assertEquals("ADMIN", role);
    }
}