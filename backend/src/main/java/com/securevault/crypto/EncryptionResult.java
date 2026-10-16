package com.securevault.crypto;

/**
 * Result of an AES-256-GCM encryption operation.
 * 
 * @param ciphertextWithTag Full ciphertext including the 128-bit appended GCM authentication tag.
 * @param nonce 96-bit (12-byte) initialization vector / nonce.
 * @param authenticationTag Extracted 128-bit (16-byte) authentication tag for verification & metadata.
 * @param formatVersion Encrypted container format version (1).
 */
public record EncryptionResult(
        byte[] ciphertextWithTag,
        byte[] nonce,
        byte[] authenticationTag,
        int formatVersion
) {
    public int getEncryptedSizeBytes() {
        return ciphertextWithTag != null ? ciphertextWithTag.length : 0;
    }
}
