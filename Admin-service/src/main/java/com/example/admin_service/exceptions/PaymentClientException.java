package com.example.admin_service.exceptions;

public class PaymentClientException extends RuntimeException {
    public PaymentClientException(String message) {
        super(message);
    }
}
