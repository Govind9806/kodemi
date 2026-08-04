package com.example.ai_service.exceptions;

public class ConversationServiceException extends RuntimeException {
    public ConversationServiceException(String message) {
        super(message);
    }
}
