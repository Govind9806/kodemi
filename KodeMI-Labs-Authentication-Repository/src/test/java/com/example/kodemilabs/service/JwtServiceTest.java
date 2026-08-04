package com.example.kodemilabs.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private final String secret = "thisIsASecretKeyWithAtLeast32BytesLength!!";

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        // inject secret manually (since @Value won't work in unit test)
        Field field = JwtService.class.getDeclaredField("secretKeyString");
        field.setAccessible(true);
        field.set(jwtService, secret);
    }

    @Test
    void generateToken_success() {
        String token = jwtService.generateToken(
                "user1", "test@mail.com", "LEARNER", "testUser"
        );

        assertNotNull(token);

        Claims claims = parseToken(token);

        assertEquals("test@mail.com", claims.getSubject());
        assertEquals("user1", claims.get("userId"));
        assertEquals("LEARNER", claims.get("role"));

        assertNull(claims.get("type"));

        assertEquals("USER", jwtService.getTokenType(token));
    }

    @Test
    void generateServiceToken_success() {
        String token = jwtService.generateServiceToken("course-service");

        assertNotNull(token);

        Claims claims = parseToken(token);

        assertEquals("course-service", claims.getSubject());
        assertEquals("SERVICE", claims.get("type"));
        assertEquals("ROLE_INTERNAL", claims.get("role"));

        assertTrue(jwtService.isServiceToken(token));
    }

    @Test
    void validateToken_success() {
        String token = jwtService.generateToken(
                "user1", "test@mail.com", "LEARNER", "testUser"
        );

        boolean isValid = jwtService.validateToken(token,
                org.springframework.security.core.userdetails.User
                        .withUsername("test@mail.com")
                        .password("pass")
                        .authorities("ROLE_USER")
                        .build()
        );

        assertTrue(isValid);
    }

    @Test
    void validateToken_invalidUser() {
        String token = jwtService.generateToken(
                "user1", "test@mail.com", "LEARNER", "testUser"
        );

        boolean isValid = jwtService.validateToken(token,
                org.springframework.security.core.userdetails.User
                        .withUsername("wrong@mail.com")
                        .password("pass")
                        .authorities("ROLE_USER")
                        .build()
        );

        assertFalse(isValid);
    }

    @Test
    void validateToken_withBrokenToken_returnsFalse() {
        boolean result = jwtService.validateToken(
                "totally.invalid.token",
                org.springframework.security.core.userdetails.User
                        .withUsername("test@mail.com")
                        .password("pass")
                        .authorities("ROLE_USER")
                        .build()
        );

        assertFalse(result);
    }

    @Test
    void isServiceToken_false_forUserToken() {
        String token = jwtService.generateToken(
                "user1", "test@mail.com", "LEARNER", "testUser"
        );

        assertFalse(jwtService.isServiceToken(token));
    }

    @Test
    void getTokenType_returnsUser_forRegularToken() {
        String token = jwtService.generateToken(
                "user1", "test@mail.com", "LEARNER", "testUser"
        );

        assertEquals("USER", jwtService.getTokenType(token));
    }

    @Test
    void getTokenType_returnsService_forServiceToken() {
        String token = jwtService.generateServiceToken("notification-service");
        assertEquals("SERVICE", jwtService.getTokenType(token));
    }

    @Test
    void extractUsername_returnsEmail() {
        String token = jwtService.generateToken(
                "user1", "myemail@mail.com", "LEARNER", "testUser"
        );

        assertEquals("myemail@mail.com", jwtService.extractUsername(token));
    }

    private Claims parseToken(String token) {
        return io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private java.security.Key getKey() {
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}