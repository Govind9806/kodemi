package com.example.kodemilabs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void testGettersAndSetters() {
        RefreshToken token = new RefreshToken();

        // Set values
        token.setToken("abc123");
        token.setEmail("test@example.com");
        token.setExpiry(1678901234L);

        // Verify getters
        assertEquals("abc123", token.getToken());
        assertEquals("test@example.com", token.getEmail());
        assertEquals(1678901234L, token.getExpiry());
    }

    @Test
    void testConstructorAndDataAnnotation() {
        RefreshToken token = new RefreshToken();
        assertNotNull(token); // No-args constructor works

        // Lombok @Data generates equals/hashCode/toString
        RefreshToken token2 = new RefreshToken();
        token2.setToken("abc123");
        token2.setEmail("test@example.com");
        token2.setExpiry(1678901234L);

        token.setToken("abc123");
        token.setEmail("test@example.com");
        token.setExpiry(1678901234L);

        assertEquals(token, token2); // tests equals method
        assertNotNull(token.toString()); // toString() returns a non-null string
        assertEquals(token.hashCode(), token2.hashCode()); // hashCode consistency
    }
}