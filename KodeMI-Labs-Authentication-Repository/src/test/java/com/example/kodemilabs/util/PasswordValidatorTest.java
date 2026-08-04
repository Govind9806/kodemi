package com.example.kodemilabs.util;

import com.example.kodemilabs.exceptions.registration.WeakPasswordException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    @Test
    void validate_shouldPass_whenPasswordIsStrong() {
        assertDoesNotThrow(() ->
                PasswordValidator.validate("Strong@123"));
    }

    @Test
    void validate_shouldThrow_whenPasswordIsNull() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> PasswordValidator.validate(null));

        assertTrue(ex.getMessage().contains("at least"));
    }

    @Test
    void validate_shouldThrow_whenTooShort() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> PasswordValidator.validate("S@1a"));

        assertTrue(ex.getMessage().contains("at least"));
    }

    @Test
    void validate_shouldThrow_whenNoUppercase() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> PasswordValidator.validate("weak@123"));

        assertTrue(ex.getMessage().contains("uppercase"));
    }

    @Test
    void validate_shouldThrow_whenNoLowercase() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> PasswordValidator.validate("WEAK@123"));

        assertTrue(ex.getMessage().contains("lowercase"));
    }

    @Test
    void validate_shouldThrow_whenNoDigit() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> PasswordValidator.validate("Weak@abc"));

        assertTrue(ex.getMessage().contains("digit"));
    }

    @Test
    void validate_shouldThrow_whenNoSpecialChar() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> PasswordValidator.validate("Weak1234"));

        assertTrue(ex.getMessage().contains("special"));
    }

    @Test
    void validate_shouldPass_whenExactlyMinLength() {
        assertDoesNotThrow(() ->
                PasswordValidator.validate("A@1bcdef")); // exactly 8 chars
    }

    @Test
    void validate_shouldFailAtFirstRule() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> PasswordValidator.validate("short"));

        // Should fail at length, not other checks
        assertTrue(ex.getMessage().contains("at least"));
    }
}