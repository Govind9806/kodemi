package com.example.admin_service.exceptions;

public class InvalidPayoutActionException extends RuntimeException {
    public InvalidPayoutActionException(String message) {
        super(message);
    }
}
