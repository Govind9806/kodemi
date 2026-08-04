package com.example.admin_service.exceptions;

public class PayoutProcessingException extends RuntimeException {
    public PayoutProcessingException(String message) {
        super(message);
    }
}
