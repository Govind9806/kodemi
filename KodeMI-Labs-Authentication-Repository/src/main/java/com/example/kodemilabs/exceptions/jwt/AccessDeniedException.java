package com.example.kodemilabs.exceptions.jwt;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}