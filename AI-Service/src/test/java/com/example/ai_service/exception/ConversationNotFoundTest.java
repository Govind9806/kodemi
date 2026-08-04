package com.example.ai_service.exception;

import com.example.ai_service.exceptions.ConversationNotFound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConversationNotFoundTest {

    @Test
    void constructorShouldSetMessage() {

        ConversationNotFound exception =
                new ConversationNotFound("Conversation not found");

        assertEquals(
                "Conversation not found",
                exception.getMessage()
        );

        assertTrue(
                exception instanceof RuntimeException
        );
    }
}
