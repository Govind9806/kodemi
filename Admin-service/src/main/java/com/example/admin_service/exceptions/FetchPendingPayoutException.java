package com.example.admin_service.exceptions;

public class FetchPendingPayoutException extends RuntimeException {
    public FetchPendingPayoutException(String message) {
        super(message);
    }
}
