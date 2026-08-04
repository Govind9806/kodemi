package com.example.user_service.exception;

public class LearnerNotFoundException extends RuntimeException {

    public LearnerNotFoundException(String userId) {
        super("User profile not found for userId: " + userId);
    }
}
