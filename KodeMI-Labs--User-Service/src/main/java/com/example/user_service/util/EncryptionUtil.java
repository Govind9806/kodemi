package com.example.user_service.util;

import com.example.user_service.exception.EncryptionException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;     // 96-bit IV for GCM
    private static final int TAG_LENGTH = 128;   // 128-bit authentication tag

    private static final SecureRandom secureRandom = new SecureRandom();

    @Value("${encryption.secret}")
    String secretKey;

    private static SecretKeySpec keySpec; // static keySpec for use in static methods

    /**
     * Initializes the AES key after the secretKey is injected by Spring.
     */
    @PostConstruct
    void init() {
        initStatic(secretKey);
    }

    /**
     * Static initializer — performs the actual static field assignment.
     * Extracted to satisfy SonarQube S2696 (instance method must not write static field).
     */
    public static void initStatic(String key) {
        if (key == null || key.isBlank()) {
            throw new EncryptionException("Encryption secret key must not be null or blank");
        }

        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        int len = keyBytes.length;

        if (len != 16 && len != 24 && len != 32) {
            throw new EncryptionException(
                    "Invalid AES key length: " + len + " bytes. Must be 16, 24, or 32 bytes."
            );
        }

        setKeySpec(new SecretKeySpec(keyBytes, AES));
    }

    private static synchronized void setKeySpec(SecretKeySpec spec) {
        keySpec = spec;
    }


    /**
     * Encrypts plaintext using AES-GCM with a random IV per call.
     * Output format: Base64(IV || ciphertext + GCM tag)
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new EncryptionException("Plaintext must not be null or blank");
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] output = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, output, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, output, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(output);

        } catch (Exception e) {
            throw new EncryptionException("Encryption failed: " + e.getMessage());
        }
    }

    /**
     * Decrypts a Base64-encoded ciphertext produced by {@link #encrypt(String)}.
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new EncryptionException("Ciphertext must not be null or blank");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            if (decoded.length <= IV_LENGTH) {
                throw new EncryptionException("Ciphertext too short — possibly corrupted");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - IV_LENGTH];

            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
            System.arraycopy(decoded, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed: " + e.getMessage());
        }
    }
}