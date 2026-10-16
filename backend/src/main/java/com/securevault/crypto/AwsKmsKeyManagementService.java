package com.securevault.crypto;

import com.securevault.exception.EncryptionException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;
import software.amazon.awssdk.services.kms.model.KmsException;

import java.util.Arrays;

/**
 * PRODUCTION KMS IMPLEMENTATION.
 * Uses AWS KMS Customer Master Key (CMK) / KMS Key for envelope encryption.
 * Plaintext DEKs exist only in volatile memory during crypto operations and are zeroed out after use.
 * Only wrapped DEKs are stored in database records.
 */
@Service
@Profile("prod")
public class AwsKmsKeyManagementService implements KeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(AwsKmsKeyManagementService.class);

    @Value("${kms.aws.key-id}")
    private String kmsKeyId;

    @Value("${kms.aws.region:us-east-1}")
    private String awsRegion;

    private KmsClient kmsClient;

    @PostConstruct
    public void init() {
        if (kmsKeyId == null || kmsKeyId.isBlank() || kmsKeyId.contains("REPLACE")) {
            throw new IllegalStateException("AWS_KMS_KEY_ID must be configured for production profile.");
        }
        log.info("Initializing AWS KMS Client in region: {} for key: {}", awsRegion, kmsKeyId);
        this.kmsClient = KmsClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    @PreDestroy
    public void cleanup() {
        if (kmsClient != null) {
            kmsClient.close();
        }
    }

    @Override
    public DataKey generateDataKey() {
        try {
            GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
                    .keyId(kmsKeyId)
                    .keySpec(DataKeySpec.AES_256)
                    .build();

            GenerateDataKeyResponse response = kmsClient.generateDataKey(request);

            byte[] plaintextDek = response.plaintext().asByteArray();
            byte[] wrappedDek = response.ciphertextBlob().asByteArray();

            return new DataKey(plaintextDek, wrappedDek, kmsKeyId);
        } catch (KmsException e) {
            log.error("AWS KMS generateDataKey failed: code={}, message={}", e.awsErrorDetails().errorCode(), e.getMessage());
            throw new EncryptionException("Failed to generate encryption key from AWS KMS", e);
        } catch (Exception e) {
            log.error("Unexpected error during AWS KMS key generation", e);
            throw new EncryptionException("AWS KMS service unavailable", e);
        }
    }

    @Override
    public byte[] decryptDataKey(byte[] wrappedDek, String keyReference) {
        try {
            String targetKey = (keyReference != null && !keyReference.isBlank()) ? keyReference : kmsKeyId;

            DecryptRequest request = DecryptRequest.builder()
                    .keyId(targetKey)
                    .ciphertextBlob(SdkBytes.fromByteArray(wrappedDek))
                    .build();

            DecryptResponse response = kmsClient.decrypt(request);
            return response.plaintext().asByteArray();
        } catch (KmsException e) {
            log.error("AWS KMS decrypt failed: code={}, message={}", e.awsErrorDetails().errorCode(), e.getMessage());
            throw new EncryptionException("Failed to decrypt Data Encryption Key via AWS KMS", e);
        } catch (Exception e) {
            log.error("Unexpected error during AWS KMS key decryption", e);
            throw new EncryptionException("AWS KMS service unavailable", e);
        }
    }

    @Override
    public String getProviderName() {
        return "AWS_KMS";
    }
}
