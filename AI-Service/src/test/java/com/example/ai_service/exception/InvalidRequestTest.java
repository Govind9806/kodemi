package com.example.ai_service.exception;

import com.example.ai_service.exceptions.InvalidRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidRequestTest {

    @Test
    void constructorShouldSetMessage() {

        InvalidRequest exception =
                new InvalidRequest("Invalid request");

        assertEquals(
                "Invalid request",
                exception.getMessage()
        );

        assertTrue(
                exception instanceof RuntimeException
        );
    }

}
