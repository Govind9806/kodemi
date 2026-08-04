package com.example.user_service.util;

import com.example.user_service.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    // Sample 16-byte key (AES-128)
    private static final String TEST_SECRET = "1234567890abcdef";

    @BeforeEach
    void setUp() {
        encryptionUtil = new EncryptionUtil();

        // Manually inject the secret key for testing
        encryptionUtil.secretKey = TEST_SECRET;

        // Initialize keySpec (simulates @PostConstruct)
        encryptionUtil.init();
    }

    @Test
    void testEncryptDecrypt() {
        String plaintext = "Hello, World!";

        String ciphertext = EncryptionUtil.encrypt(plaintext);
        assertNotNull(ciphertext, "Ciphertext should not be null");
        assertNotEquals(plaintext, ciphertext, "Ciphertext should differ from plaintext");

        String decrypted = EncryptionUtil.decrypt(ciphertext);
        assertEquals(plaintext, decrypted, "Decrypted text should match original plaintext");
    }

    @Test
    void testEncryptWithNullOrBlank() {
        assertThrows(EncryptionException.class, () -> EncryptionUtil.encrypt(null));
        assertThrows(EncryptionException.class, () -> EncryptionUtil.encrypt(""));
        assertThrows(EncryptionException.class, () -> EncryptionUtil.encrypt("   "));
    }

    @Test
    void testDecryptWithNullOrBlank() {
        assertThrows(EncryptionException.class, () -> EncryptionUtil.decrypt(null));
        assertThrows(EncryptionException.class, () -> EncryptionUtil.decrypt(""));
        assertThrows(EncryptionException.class, () -> EncryptionUtil.decrypt("   "));
    }

    @Test
    void testDecryptWithInvalidCiphertext() {
        String invalidCiphertext = "invalid-base64";
        assertThrows(EncryptionException.class, () -> EncryptionUtil.decrypt(invalidCiphertext));
    }
}