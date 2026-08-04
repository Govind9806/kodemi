package com.example.admin_service.exceptions;

public class AdminNotFoundException extends RuntimeException {
    
    public AdminNotFoundException(String message) {
        super(message);
    }
    
    public AdminNotFoundException(String adminId, String message) {
        super("Admin not found with adminId: " + adminId + ". " + message);
    }
}
