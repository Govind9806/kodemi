package com.example.ai_service.exception;

import com.example.ai_service.exceptions.MessageServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageServiceExceptionTest {

    @Test
    void constructor_shouldSetMessage() {

        MessageServiceException exception =
                new MessageServiceException("Message service error");

        assertEquals(
                "Message service error",
                exception.getMessage()
        );

        assertInstanceOf(
                RuntimeException.class,
                exception
        );
    }
    @Test
    void messageShouldNotBeNull() {
        RuntimeException exception =
                new MessageServiceException("Test");

        assertNotNull(exception.getMessage());
    }

    @Test
    void exceptionShouldExtendRuntimeException() {
        MessageServiceException exception =
                new MessageServiceException("Test");

        assertTrue(exception instanceof RuntimeException);
    }
}
