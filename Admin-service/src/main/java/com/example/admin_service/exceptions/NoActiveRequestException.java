package com.example.admin_service.exceptions;

public class NoActiveRequestException extends RuntimeException {
    public NoActiveRequestException(String message) {
        super(message);
    }
}
