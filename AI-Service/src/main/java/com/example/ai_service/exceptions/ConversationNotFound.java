package com.example.ai_service.exceptions;

public class ConversationNotFound extends RuntimeException {
    public ConversationNotFound(String message) {
        super(message);
    }
}
