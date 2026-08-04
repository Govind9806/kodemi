package com.example.admin_service.util;


import com.example.admin_service.exceptions.WeakPasswordException;

import java.util.regex.Pattern;


public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    private PasswordValidator() {

    }

    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new WeakPasswordException(
                    "Password must be at least " + MIN_LENGTH + " characters long"
            );
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException(
                    "Password must contain at least one uppercase letter"
            );
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException(
                    "Password must contain at least one lowercase letter"
            );
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException(
                    "Password must contain at least one digit"
            );
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException(
                    "Password must contain at least one special character (!@#$%^&*()_+-=[]{}...)"
            );
        }
    }
}