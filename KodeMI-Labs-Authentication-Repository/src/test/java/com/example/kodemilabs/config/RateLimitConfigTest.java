package com.example.kodemilabs.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitConfigTest {

    @Test
    void testRateLimiterBeanAndBehavior() {
        RateLimitConfig config = new RateLimitConfig();
        RateLimitConfig.RateLimiter limiter = config.rateLimiter();

        assertNotNull(limiter);

        String id = "test-ip-1";

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allowRequest(id), "Request " + (i + 1) + " should be allowed");
        }

        assertFalse(limiter.allowRequest(id), "6th request should be rate-limited");
    }
}
