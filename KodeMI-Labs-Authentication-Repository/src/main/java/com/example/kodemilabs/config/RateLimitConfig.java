package com.example.kodemilabs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter();
    }

    public static class RateLimiter {
        private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();
        private static final int MAX_REQUESTS_PER_MINUTE = 5;
        private static final long WINDOW_SIZE_MS = TimeUnit.MINUTES.toMillis(1);

        public boolean allowRequest(String identifier) {
            long now = System.currentTimeMillis();
            java.util.Iterator<Map.Entry<String, RateLimitEntry>> iterator = requestCounts.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, RateLimitEntry> entry = iterator.next();
                if (now - entry.getValue().windowStart > WINDOW_SIZE_MS) {
                    iterator.remove();
                }
            }

            RateLimitEntry entry = requestCounts.get(identifier);
            if (entry == null) {
                RateLimitEntry newEntry = new RateLimitEntry(now, 0);
                RateLimitEntry existing = requestCounts.putIfAbsent(identifier, newEntry);
                entry = (existing != null) ? existing : newEntry;
            }

            if (now - entry.windowStart > WINDOW_SIZE_MS) {
                entry.windowStart = now;
                entry.count = 0;
            }

            if (entry.count >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }

            entry.count++;
            return true;
        }

        private static class RateLimitEntry {
            long windowStart;
            int count;

            RateLimitEntry(long windowStart, int count) {
                this.windowStart = windowStart;
                this.count = count;
            }
        }
    }
}
