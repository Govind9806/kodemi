package com.example.kodemilabs.exceptions.jwt;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}