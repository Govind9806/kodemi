package com.example.ai_service;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ActiveProfiles("test")
class AiServiceApplicationTests {

    @Test
    void main_ClassExists() {
        assertDoesNotThrow(() -> {
            Class.forName("com.example.ai_service.AiServiceApplicationTests");
        });
	}

}
