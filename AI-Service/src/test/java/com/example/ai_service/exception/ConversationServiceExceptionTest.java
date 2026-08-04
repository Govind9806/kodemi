package com.example.ai_service.exception;

import com.example.ai_service.exceptions.ConversationServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConversationServiceExceptionTest {

    @Test
    void constructorShouldSetMessage() {

        ConversationServiceException exception =
                new ConversationServiceException("Service error");

        assertEquals(
                "Service error",
                exception.getMessage()
        );

        assertTrue(
                exception instanceof RuntimeException
        );
    }

}
