package com.example.admin_service.exceptions;

public class UnauthorizedPayoutAccessException extends RuntimeException {
    public UnauthorizedPayoutAccessException(String message) {
        super(message);
    }
}
