package com.securevault.crypto;

import com.securevault.exception.EncryptionException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * DEVELOPMENT PROFILE ONLY.
 * Uses a locally supplied Key Encryption Key (KEK) to simulate AWS KMS envelope encryption.
 * 
 * IMPORTANT: This implementation is for local development and integration testing only.
 * It is NOT equivalent to production-grade AWS KMS and must never be used in production.
 */
@Service
@Profile("dev")
public class LocalDevKeyManagementService implements KeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(LocalDevKeyManagementService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH_BYTES = 12; // 96 bits
    private static final int DEK_LENGTH_BYTES = 32; // 256 bits

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${kms.local-kek:}")
    private String localKekBase64;

    private byte[] kekBytes;

    @PostConstruct
    public void init() {
        log.warn("================================================================================");
        log.warn("  WARNING: USING LOCAL DEVELOPMENT KEY MANAGEMENT (LocalDevKeyManagementService)  ");
        log.warn("  This is strictly for local dev/testing. It is NOT equivalent to AWS KMS.        ");
        log.warn("================================================================================");

        if (localKekBase64 == null || localKekBase64.isBlank() || localKekBase64.contains("REPLACE_WITH")) {
            // Generate an ephemeral development KEK if none is configured
            log.warn("No LOCAL_KEK configured. Generating an ephemeral in-memory KEK for this session.");
            this.kekBytes = new byte[32];
            secureRandom.nextBytes(this.kekBytes);
        } else {
            try {
                this.kekBytes = Base64.getDecoder().decode(localKekBase64.trim());
                if (this.kekBytes.length != 32) {
                    throw new IllegalArgumentException("LOCAL_KEK must be exactly 32 bytes (256 bits)");
                }
            } catch (Exception e) {
                log.error("Failed to decode LOCAL_KEK. Falling back to ephemeral key.", e);
                this.kekBytes = new byte[32];
                secureRandom.nextBytes(this.kekBytes);
            }
        }
    }

    @Override
    public DataKey generateDataKey() {
        byte[] plaintextDek = new byte[DEK_LENGTH_BYTES];
        secureRandom.nextBytes(plaintextDek);

        byte[] wrappedDek = wrapKey(plaintextDek);
        String keyRef = "local-dev-kek://default-local-key";

        return new DataKey(plaintextDek, wrappedDek, keyRef);
    }

    @Override
    public byte[] decryptDataKey(byte[] wrappedDek, String keyReference) {
        if (wrappedDek == null || wrappedDek.length < IV_LENGTH_BYTES + 16) {
            throw new EncryptionException("Invalid wrapped DEK payload");
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(wrappedDek);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(this.kekBytes, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            log.error("Failed to unwrap DEK using local KEK: {}", e.getMessage());
            throw new EncryptionException("Failed to unwrap Data Encryption Key", e);
        }
    }

    private byte[] wrapKey(byte[] plaintextKey) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(this.kekBytes, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(plaintextKey);

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return buffer.array();
        } catch (Exception e) {
            throw new EncryptionException("Failed to wrap Data Encryption Key", e);
        }
    }

    @Override
    public String getProviderName() {
        return "LOCAL_DEV_KEK";
    }
}
