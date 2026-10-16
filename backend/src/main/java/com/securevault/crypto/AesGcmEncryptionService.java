package com.securevault.crypto;

import com.securevault.exception.DecryptionException;
import com.securevault.exception.EncryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Service for AES-256-GCM authenticated encryption and decryption.
 * Uses Java Cryptography Architecture (JCA) with standard AES/GCM/NoPadding.
 *
 * Security properties:
 * - Confidentiality & Authenticity via AES-256-GCM.
 * - 96-bit (12-byte) fresh cryptographically random nonce per encryption operation.
 * - 128-bit (16-byte) authentication tag.
 * - Tag is verified before releasing any plaintext.
 * - Best-effort zeroing of in-memory key buffers.
 */
@Service
public class AesGcmEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AesGcmEncryptionService.class);

    public static final String ALGORITHM = "AES/GCM/NoPadding";
    public static final int NONCE_LENGTH_BYTES = 12; // 96 bits
    public static final int TAG_LENGTH_BITS = 128;   // 16 bytes
    public static final int TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8;
    public static final int FORMAT_VERSION = 1;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypts plaintext bytes using AES-256-GCM with a fresh 96-bit nonce and provided DEK.
     *
     * @param plaintext The unencrypted file bytes.
     * @param dekBytes  The 256-bit Data Encryption Key bytes.
     * @return EncryptionResult containing ciphertext (with tag appended), nonce, and extracted tag.
     */
    public EncryptionResult encrypt(byte[] plaintext, byte[] dekBytes) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }
        if (dekBytes == null || dekBytes.length != 32) {
            throw new IllegalArgumentException("DEK must be exactly 32 bytes (256 bits)");
        }

        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        secureRandom.nextBytes(nonce);

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = new SecretKeySpec(dekBytes, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            // JCA appends the 128-bit authentication tag to the end of the ciphertext
            byte[] ciphertextWithTag = cipher.doFinal(plaintext);

            // Extract the tag (last 16 bytes) for separate metadata storage
            int tagStartIndex = ciphertextWithTag.length - TAG_LENGTH_BYTES;
            byte[] authenticationTag = Arrays.copyOfRange(ciphertextWithTag, tagStartIndex, ciphertextWithTag.length);

            return new EncryptionResult(ciphertextWithTag, nonce, authenticationTag, FORMAT_VERSION);
        } catch (Exception e) {
            log.error("AES-GCM encryption failed: {}", e.getMessage());
            throw new EncryptionException("Failed to encrypt data with AES-256-GCM", e);
        }
    }

    /**
     * Decrypts AES-256-GCM ciphertext, verifying the authentication tag before returning plaintext.
     * If the ciphertext or tag has been tampered with in any way, DecryptionException is thrown.
     *
     * @param ciphertextWithTag Ciphertext including the 16-byte authentication tag.
     * @param nonce             The 96-bit nonce used during encryption.
     * @param dekBytes          The 256-bit Data Encryption Key bytes.
     * @return Decrypted plaintext bytes.
     */
    public byte[] decrypt(byte[] ciphertextWithTag, byte[] nonce, byte[] dekBytes) {
        if (ciphertextWithTag == null || ciphertextWithTag.length < TAG_LENGTH_BYTES) {
            throw new DecryptionException("Ciphertext is too short or missing authentication tag");
        }
        if (nonce == null || nonce.length != NONCE_LENGTH_BYTES) {
            throw new DecryptionException("Invalid nonce: must be 96 bits");
        }
        if (dekBytes == null || dekBytes.length != 32) {
            throw new DecryptionException("Invalid DEK: must be 256 bits");
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = new SecretKeySpec(dekBytes, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, nonce);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            return cipher.doFinal(ciphertextWithTag);
        } catch (AEADBadTagException e) {
            log.warn("AES-GCM authentication tag verification failed. Ciphertext may be corrupted or tampered with.");
            throw new DecryptionException("Unable to restore this backup because its integrity check failed.", e);
        } catch (Exception e) {
            log.error("AES-GCM decryption failed: {}", e.getMessage());
            throw new DecryptionException("Unable to restore this backup because its integrity check failed.", e);
        }
    }

    /**
     * Wipes sensitive key material from memory.
     */
    public static void wipeKey(byte[] keyBytes) {
        if (keyBytes != null) {
            Arrays.fill(keyBytes, (byte) 0);
        }
    }
}
