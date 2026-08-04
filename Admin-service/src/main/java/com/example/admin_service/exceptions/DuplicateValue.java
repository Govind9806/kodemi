package com.example.admin_service.exceptions;

public class DuplicateValue extends RuntimeException {
    public DuplicateValue(String message) {
        super(message);
    }
}
