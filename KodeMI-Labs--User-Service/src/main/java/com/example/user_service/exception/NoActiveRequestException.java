package com.example.user_service.exception;

public class NoActiveRequestException extends RuntimeException {
    public NoActiveRequestException(String message) {
        super(message);
    }
}
