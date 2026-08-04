package com.example.user_service.util;

import com.example.user_service.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncryptionUtilExtendedTest {

    @BeforeEach
    void setUp() {
        EncryptionUtil.initStatic("1234567890abcdef"); // 16-byte AES-128 key
    }

    // ── initStatic validation ─────────────────────────────────────────────

    @Test
    void initStatic_nullKey_throwsEncryptionException() {
        assertThrows(EncryptionException.class, () -> EncryptionUtil.initStatic(null));
    }

    @Test
    void initStatic_blankKey_throwsEncryptionException() {
        assertThrows(EncryptionException.class, () -> EncryptionUtil.initStatic("   "));
    }

    @Test
    void initStatic_invalidKeyLength_throwsEncryptionException() {
        // 10 bytes — not 16, 24, or 32
        assertThrows(EncryptionException.class, () -> EncryptionUtil.initStatic("shortkey12"));
    }

    @Test
    void initStatic_24ByteKey_succeeds() {
        assertDoesNotThrow(() -> EncryptionUtil.initStatic("123456789012345678901234")); // 24 bytes
    }

    @Test
    void initStatic_32ByteKey_succeeds() {
        assertDoesNotThrow(() -> EncryptionUtil.initStatic("12345678901234567890123456789012")); // 32 bytes
    }

    // ── encrypt/decrypt round-trips ───────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
        "Hello World",
        "ABCDE1234F",
        "1234567890",
        "HDFC0001234",
        "sensitive-data-123",
        "Unicode: 你好"
    })
    void encryptDecrypt_roundTrip(String plaintext) {
        String ciphertext = EncryptionUtil.encrypt(plaintext);
        assertNotNull(ciphertext);
        assertNotEquals(plaintext, ciphertext);
        assertEquals(plaintext, EncryptionUtil.decrypt(ciphertext));
    }

    @Test
    void encrypt_producesUniqueOutputEachCall() {
        // AES-GCM uses random IV per call
        String c1 = EncryptionUtil.encrypt("same");
        String c2 = EncryptionUtil.encrypt("same");
        assertNotEquals(c1, c2);
    }

    @Test
    void decrypt_tooShortCiphertext_throwsEncryptionException() {
        // Base64 of only 5 bytes — shorter than IV_LENGTH (12)
        String tooShort = java.util.Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4, 5});
        assertThrows(EncryptionException.class, () -> EncryptionUtil.decrypt(tooShort));
    }

    @Test
    void decrypt_corruptedCiphertext_throwsEncryptionException() {
        String ciphertext = EncryptionUtil.encrypt("test");
        // Corrupt the ciphertext by appending garbage
        String corrupted = ciphertext + "AAAA";
        assertThrows(EncryptionException.class, () -> EncryptionUtil.decrypt(corrupted));
    }

    // ── init() via @PostConstruct path ────────────────────────────────────

    @Test
    void init_delegatesToInitStatic() {
        EncryptionUtil util = new EncryptionUtil();
        util.secretKey = "1234567890abcdef";
        util.init(); // should not throw

        // Verify encryption still works after init()
        String ciphertext = EncryptionUtil.encrypt("test");
        assertEquals("test", EncryptionUtil.decrypt(ciphertext));
    }
}
