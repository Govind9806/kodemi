package com.example.ai_service.exception;

import com.example.ai_service.exceptions.UnAuthorizedRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnAuthorizedRequestExceptionTest {

    @Test
    void constructorShouldSetMessage() {

        UnAuthorizedRequestException exception =
                new UnAuthorizedRequestException("Unauthorized");

        assertEquals(
                "Unauthorized",
                exception.getMessage()
        );

        assertTrue(
                exception instanceof RuntimeException
        );
    }
}