package com.example.kodemilabs.exceptions.otp;

public class OtpAlreadyUsedException extends RuntimeException {

    public OtpAlreadyUsedException(String message) {
        super(message);
    }
}
