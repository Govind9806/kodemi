package com.example.ai_service.exceptions;

public class MessagesNotFound extends RuntimeException {
    public MessagesNotFound(String message) {
        super(message);
    }
}
