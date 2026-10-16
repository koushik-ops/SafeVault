package com.securevault.crypto;

import com.securevault.exception.DecryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmEncryptionServiceTest {

    private AesGcmEncryptionService encryptionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @BeforeEach
    void setUp() {
        encryptionService = new AesGcmEncryptionService();
    }

    private byte[] generateTestDek() {
        byte[] dek = new byte[32]; // 256 bits
        secureRandom.nextBytes(dek);
        return dek;
    }

    @Test
    @DisplayName("Should successfully encrypt and decrypt text file byte-for-byte")
    void testEncryptDecryptTextFileRoundTrip() {
        byte[] dek = generateTestDek();
        String originalContent = "CONFIDENTIAL FINANCIAL REPORT - Q3 NET INCOME: $4,520,000.00 USD";
        byte[] plaintext = originalContent.getBytes(StandardCharsets.UTF_8);

        EncryptionResult result = encryptionService.encrypt(plaintext, dek);

        assertNotNull(result);
        assertNotNull(result.ciphertextWithTag());
        assertNotNull(result.nonce());
        assertEquals(12, result.nonce().length, "Nonce must be exactly 96 bits (12 bytes)");
        assertEquals(16, result.authenticationTag().length, "GCM tag must be exactly 128 bits (16 bytes)");
        assertEquals(1, result.formatVersion());

        // Ciphertext should not match plaintext
        assertFalse(Arrays.equals(plaintext, result.ciphertextWithTag()));

        // Decrypt
        byte[] decrypted = encryptionService.decrypt(result.ciphertextWithTag(), result.nonce(), dek);

        // Byte-for-byte identical
        assertArrayEquals(plaintext, decrypted, "Decrypted data must be byte-for-byte identical to original");
        assertEquals(originalContent, new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should preserve binary file round-trip integrity (e.g. PDF or binary bytes)")
    void testBinaryRoundTripIntegrity() {
        byte[] dek = generateTestDek();
        byte[] binaryData = new byte[1024 * 64]; // 64 KB random binary data
        secureRandom.nextBytes(binaryData);

        EncryptionResult result = encryptionService.encrypt(binaryData, dek);
        byte[] decrypted = encryptionService.decrypt(result.ciphertextWithTag(), result.nonce(), dek);

        assertArrayEquals(binaryData, decrypted, "Binary data must match byte-for-byte");
    }

    @Test
    @DisplayName("Should generate independent nonces for identical plaintexts")
    void testIndependentNoncesPerOperation() {
        byte[] dek = generateTestDek();
        byte[] plaintext = "Identical financial record".getBytes(StandardCharsets.UTF_8);

        EncryptionResult result1 = encryptionService.encrypt(plaintext, dek);
        EncryptionResult result2 = encryptionService.encrypt(plaintext, dek);

        assertFalse(Arrays.equals(result1.nonce(), result2.nonce()), "Nonces must be unique per operation");
        assertFalse(Arrays.equals(result1.ciphertextWithTag(), result2.ciphertextWithTag()),
                "Ciphertexts must differ due to unique nonces");
    }

    @Test
    @DisplayName("Should reject modified/tampered ciphertext with DecryptionException")
    void testRejectTamperedCiphertext() {
        byte[] dek = generateTestDek();
        byte[] plaintext = "Quarterly payroll data".getBytes(StandardCharsets.UTF_8);

        EncryptionResult result = encryptionService.encrypt(plaintext, dek);
        byte[] tamperedCiphertext = result.ciphertextWithTag().clone();

        // Flip a single bit in the ciphertext
        tamperedCiphertext[0] ^= 0x01;

        DecryptionException exception = assertThrows(DecryptionException.class, () -> {
            encryptionService.decrypt(tamperedCiphertext, result.nonce(), dek);
        });

        assertTrue(exception.getMessage().contains("integrity check failed"),
                "Exception message must indicate integrity failure");
    }

    @Test
    @DisplayName("Should reject modified authentication tag with DecryptionException")
    void testRejectTamperedTag() {
        byte[] dek = generateTestDek();
        byte[] plaintext = "Balance sheet data".getBytes(StandardCharsets.UTF_8);

        EncryptionResult result = encryptionService.encrypt(plaintext, dek);
        byte[] tampered = result.ciphertextWithTag().clone();

        // Tamper with the last byte (part of the 16-byte GCM authentication tag)
        tampered[tampered.length - 1] ^= 0xFF;

        assertThrows(DecryptionException.class, () -> {
            encryptionService.decrypt(tampered, result.nonce(), dek);
        });
    }

    @Test
    @DisplayName("Should reject decryption with wrong key")
    void testRejectWrongKey() {
        byte[] correctDek = generateTestDek();
        byte[] wrongDek = generateTestDek();
        byte[] plaintext = "Audit statement".getBytes(StandardCharsets.UTF_8);

        EncryptionResult result = encryptionService.encrypt(plaintext, correctDek);

        assertThrows(DecryptionException.class, () -> {
            encryptionService.decrypt(result.ciphertextWithTag(), result.nonce(), wrongDek);
        });
    }

    @Test
    @DisplayName("Should safely wipe key material from memory")
    void testWipeKey() {
        byte[] dek = generateTestDek();
        assertFalse(allZeros(dek));

        AesGcmEncryptionService.wipeKey(dek);

        assertTrue(allZeros(dek), "Key array must be overwritten with zeros");
    }

    private boolean allZeros(byte[] array) {
        for (byte b : array) {
            if (b != 0) return false;
        }
        return true;
    }
}
