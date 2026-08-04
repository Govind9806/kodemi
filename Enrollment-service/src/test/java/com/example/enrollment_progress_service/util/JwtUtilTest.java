package com.example.enrollment_progress_service.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret = "mysecretkeyforjwttestingpurposeonly12345";
    private Key key;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Test
    void testExtractClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "user-123");
        claims.put("role", "LEARNER");

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject("user-123@example.com")
                .setIssuedAt(new Date())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertEquals("user-123", jwtUtil.extractUserId(token));
        assertEquals("LEARNER", jwtUtil.extractRole(token));
        assertEquals("user-123", jwtUtil.extractUserId("Bearer " + token));
    }
}
