package com.example.admin_service.util;

import com.example.admin_service.exceptions.WeakPasswordException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PasswordValidatorTest {

    @Test
    void testValidPassword() {
        assertDoesNotThrow(() -> PasswordValidator.validate("StrongPass@123"));
    }

    @Test
    void testShortPassword() {
        assertThrows(WeakPasswordException.class, () -> PasswordValidator.validate("S@1a"));
    }

    @Test
    void testNullPassword() {
        assertThrows(WeakPasswordException.class, () -> PasswordValidator.validate(null));
    }

    @Test
    void testNoUppercase() {
        assertThrows(WeakPasswordException.class, () -> PasswordValidator.validate("lowercase@123"));
    }

    @Test
    void testNoLowercase() {
        assertThrows(WeakPasswordException.class, () -> PasswordValidator.validate("UPPERCASE@123"));
    }

    @Test
    void testNoDigit() {
        assertThrows(WeakPasswordException.class, () -> PasswordValidator.validate("NoDigitsHere@"));
    }

    @Test
    void testNoSpecialChar() {
        assertThrows(WeakPasswordException.class, () -> PasswordValidator.validate("NoSpecialChar123"));
    }
}
