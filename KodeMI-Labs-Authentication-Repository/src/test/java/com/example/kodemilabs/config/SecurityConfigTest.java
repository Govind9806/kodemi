package com.example.kodemilabs.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SecurityConfigTest {

    private SecurityConfig buildConfig(String secret) throws Exception {
        SecurityConfig config = new SecurityConfig();
        Field field = SecurityConfig.class.getDeclaredField("jwtSecret");
        field.setAccessible(true);
        field.set(config, secret);
        return config;
    }

    @Test
    void jwtDecoder_shouldReturnDecoder() throws Exception {
        SecurityConfig config = buildConfig("thisIsATestSecretKeyWithAtLeast32Chars!!");
        JwtDecoder decoder = config.jwtDecoder();
        assertNotNull(decoder);
    }

    @Test
    void jwtAuthenticationConverter_shouldReturnConverter() throws Exception {
        SecurityConfig config = buildConfig("thisIsATestSecretKeyWithAtLeast32Chars!!");
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();
        assertNotNull(converter);
    }
}
