package com.example.admin_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void passwordEncoderBeanShouldBeCreated() {
        BCryptPasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder, "PasswordEncoder should not be null");
    }

    @Test
    void passwordEncoderShouldUseCorrectStrength() {
        BCryptPasswordEncoder encoder = securityConfig.passwordEncoder();

        // BCrypt doesn't expose strength directly,
        // but we can verify via encoded hash format
        String encoded = encoder.encode("password");

        assertTrue(encoded.startsWith("$2a$12") || encoded.startsWith("$2b$12"),
                "BCrypt strength should be 12");
    }

    @Test
    void passwordShouldBeEncodedAndMatch() {
        BCryptPasswordEncoder encoder = securityConfig.passwordEncoder();

        String rawPassword = "mySecret123";
        String encodedPassword = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword, "Encoded password should differ from raw");
        assertTrue(encoder.matches(rawPassword, encodedPassword), "Passwords should match");
    }

    @Test
    void differentEncodingsShouldNotBeEqual() {
        BCryptPasswordEncoder encoder = securityConfig.passwordEncoder();

        String password = "samePassword";

        String encoded1 = encoder.encode(password);
        String encoded2 = encoder.encode(password);

        // BCrypt uses random salt → hashes should differ
        assertNotEquals(encoded1, encoded2, "Hashes should be different due to salting");
    }

    @Test
    void incorrectPasswordShouldFailMatching() {
        BCryptPasswordEncoder encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("correctPassword");

        assertFalse(encoder.matches("wrongPassword", encoded),
                "Wrong password should not match");
    }
}