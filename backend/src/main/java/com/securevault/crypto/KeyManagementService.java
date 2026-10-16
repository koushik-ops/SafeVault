package com.securevault.crypto;

/**
 * Service for envelope encryption key management.
 * In production: backed by AWS KMS.
 * In development: backed by a local KEK profile (explicitly non-production).
 */
public interface KeyManagementService {

    /**
     * Generates a new 256-bit Data Encryption Key (DEK).
     * @return DataKey containing the plaintext DEK (to be used in memory and wiped)
     *         and the wrapped/encrypted DEK (to be stored in metadata).
     */
    DataKey generateDataKey();

    /**
     * Decrypts/unwraps a wrapped Data Encryption Key (DEK).
     * @param wrappedDek The encrypted DEK bytes.
     * @param keyReference The reference/ARN of the key used to wrap the DEK.
     * @return The plaintext 256-bit DEK bytes.
     */
    byte[] decryptDataKey(byte[] wrappedDek, String keyReference);

    /**
     * Identifies the KMS provider (e.g., "AWS_KMS" or "LOCAL_DEV_KEK").
     */
    String getProviderName();

    record DataKey(byte[] plaintextDek, byte[] wrappedDek, String keyReference) {}
}
