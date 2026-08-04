package com.example.kodemilabs.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LoginHistoryTest {

    @Test
    void testLoginHistoryGettersAndSetters() {
        LoginHistory loginHistory = new LoginHistory();

        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        loginHistory.setUserId("user123");
        loginHistory.setLoginAttemptTime(now);
        loginHistory.setLoginStatus("SUCCESS");
        loginHistory.setIpAddress("127.0.0.1");
        loginHistory.setUserAgent("Mozilla/5.0");

        assertEquals("user123", loginHistory.getUserId());
        assertEquals(now, loginHistory.getLoginAttemptTime());
        assertEquals("SUCCESS", loginHistory.getLoginStatus());
        assertEquals("127.0.0.1", loginHistory.getIpAddress());
        assertEquals("Mozilla/5.0", loginHistory.getUserAgent());
    }
}