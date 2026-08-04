package com.example.ai_service.exceptions;

public class UnAuthorizedRequestException extends RuntimeException {
    public UnAuthorizedRequestException(String message) {
        super(message);
    }
}
