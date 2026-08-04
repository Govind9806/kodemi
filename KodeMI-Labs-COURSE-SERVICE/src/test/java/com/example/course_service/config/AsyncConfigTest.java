package com.example.course_service.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    @Test
    void uploadExecutor_BeanCreation_ReturnsNonNull() {
        AsyncConfig config = new AsyncConfig();
        ExecutorService executor = config.uploadExecutor();
        assertNotNull(executor);
        executor.shutdown();
    }
}
