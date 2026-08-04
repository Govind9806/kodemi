package com.example.ai_service.exception;

import com.example.ai_service.exceptions.MessagesNotFound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessagesNotFoundTest {

    @Test
    void constructorShouldSetMessage() {

        MessagesNotFound exception =
                new MessagesNotFound("Messages not found");

        assertEquals(
                "Messages not found",
                exception.getMessage()
        );

        assertTrue(
                exception instanceof RuntimeException
        );
    }

}