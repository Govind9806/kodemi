package com.example.payment_service.exception;

public class DuplicateSubscriptionCreationException extends RuntimeException {
    public DuplicateSubscriptionCreationException(String message) {
        super(message);
    }
}
