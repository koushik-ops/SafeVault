package com.securevault.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Document(collection = "SecureVault")
@CompoundIndexes({
    @CompoundIndex(name = "idx_owner_created", def = "{'ownerId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_owner_path", def = "{'ownerId': 1, 'objectPath': 1}")
})
public class Backup {

    @Id
    private UUID id;

    @Indexed
    @Field("owner_id")
    private UUID ownerId;

    @Field("original_filename")
    private String originalFilename;

    @Field("object_path")
    private String objectPath;

    @Field("original_size_bytes")
    private Long originalSizeBytes;

    @Field("encrypted_size_bytes")
    private Long encryptedSizeBytes;

    @Field("mime_type")
    private String mimeType;

    @Field("encryption_algorithm")
    private String encryptionAlgorithm = "AES/GCM/NoPadding";

    @Field("format_version")
    private Integer formatVersion = 1;

    @Field("nonce")
    private byte[] nonce;

    @Field("authentication_tag")
    private byte[] authenticationTag;

    @Field("wrapped_dek")
    private byte[] wrappedDek;

    @Field("kms_key_reference")
    private String kmsKeyReference;

    @Indexed
    @Field("status")
    private BackupStatus status = BackupStatus.COMPLETED;

    /**
     * Copy of the encrypted ciphertext stored directly in MongoDB alongside metadata.
     */
    @Field("encrypted_data")
    private byte[] encryptedData;

    @Field("created_at")
    private Instant createdAt = Instant.now();

    @Field("updated_at")
    private Instant updatedAt = Instant.now();

    public Backup() {
    }

    public Backup(UUID id, UUID ownerId, String originalFilename, String objectPath,
                  Long originalSizeBytes, Long encryptedSizeBytes, String mimeType,
                  String encryptionAlgorithm, Integer formatVersion, byte[] nonce,
                  byte[] authenticationTag, byte[] wrappedDek, String kmsKeyReference,
                  BackupStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, ownerId, originalFilename, objectPath, originalSizeBytes, encryptedSizeBytes,
             mimeType, encryptionAlgorithm, formatVersion, nonce, authenticationTag, wrappedDek,
             kmsKeyReference, status, null, createdAt, updatedAt);
    }

    public Backup(UUID id, UUID ownerId, String originalFilename, String objectPath,
                  Long originalSizeBytes, Long encryptedSizeBytes, String mimeType,
                  String encryptionAlgorithm, Integer formatVersion, byte[] nonce,
                  byte[] authenticationTag, byte[] wrappedDek, String kmsKeyReference,
                  BackupStatus status, byte[] encryptedData, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.originalFilename = originalFilename;
        this.objectPath = objectPath;
        this.originalSizeBytes = originalSizeBytes;
        this.encryptedSizeBytes = encryptedSizeBytes;
        this.mimeType = mimeType;
        this.encryptionAlgorithm = encryptionAlgorithm != null ? encryptionAlgorithm : "AES/GCM/NoPadding";
        this.formatVersion = formatVersion != null ? formatVersion : 1;
        this.nonce = nonce;
        this.authenticationTag = authenticationTag;
        this.wrappedDek = wrappedDek;
        this.kmsKeyReference = kmsKeyReference;
        this.status = status != null ? status : BackupStatus.COMPLETED;
        this.encryptedData = encryptedData;
        this.createdAt = createdAt != null ? createdAt.toInstant() : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt.toInstant() : Instant.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getObjectPath() {
        return objectPath;
    }

    public void setObjectPath(String objectPath) {
        this.objectPath = objectPath;
    }

    public Long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    public void setOriginalSizeBytes(Long originalSizeBytes) {
        this.originalSizeBytes = originalSizeBytes;
    }

    public Long getEncryptedSizeBytes() {
        return encryptedSizeBytes;
    }

    public void setEncryptedSizeBytes(Long encryptedSizeBytes) {
        this.encryptedSizeBytes = encryptedSizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public Integer getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(Integer formatVersion) {
        this.formatVersion = formatVersion;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public byte[] getAuthenticationTag() {
        return authenticationTag;
    }

    public void setAuthenticationTag(byte[] authenticationTag) {
        this.authenticationTag = authenticationTag;
    }

    public byte[] getWrappedDek() {
        return wrappedDek;
    }

    public void setWrappedDek(byte[] wrappedDek) {
        this.wrappedDek = wrappedDek;
    }

    public String getKmsKeyReference() {
        return kmsKeyReference;
    }

    public void setKmsKeyReference(String kmsKeyReference) {
        this.kmsKeyReference = kmsKeyReference;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStatus status) {
        this.status = status;
    }

    public byte[] getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(byte[] encryptedData) {
        this.encryptedData = encryptedData;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt != null ? createdAt.atOffset(ZoneOffset.UTC) : OffsetDateTime.now();
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt != null ? createdAt.toInstant() : Instant.now();
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt != null ? updatedAt.atOffset(ZoneOffset.UTC) : OffsetDateTime.now();
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt != null ? updatedAt.toInstant() : Instant.now();
    }

    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID ownerId;
        private String originalFilename;
        private String objectPath;
        private Long originalSizeBytes;
        private Long encryptedSizeBytes;
        private String mimeType;
        private String encryptionAlgorithm = "AES/GCM/NoPadding";
        private Integer formatVersion = 1;
        private byte[] nonce;
        private byte[] authenticationTag;
        private byte[] wrappedDek;
        private String kmsKeyReference;
        private BackupStatus status = BackupStatus.COMPLETED;
        private byte[] encryptedData;
        private OffsetDateTime createdAt = OffsetDateTime.now();
        private OffsetDateTime updatedAt = OffsetDateTime.now();

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder ownerId(UUID ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder originalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
            return this;
        }

        public Builder objectPath(String objectPath) {
            this.objectPath = objectPath;
            return this;
        }

        public Builder originalSizeBytes(Long originalSizeBytes) {
            this.originalSizeBytes = originalSizeBytes;
            return this;
        }

        public Builder encryptedSizeBytes(Long encryptedSizeBytes) {
            this.encryptedSizeBytes = encryptedSizeBytes;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder encryptionAlgorithm(String encryptionAlgorithm) {
            this.encryptionAlgorithm = encryptionAlgorithm;
            return this;
        }

        public Builder formatVersion(Integer formatVersion) {
            this.formatVersion = formatVersion;
            return this;
        }

        public Builder nonce(byte[] nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder authenticationTag(byte[] authenticationTag) {
            this.authenticationTag = authenticationTag;
            return this;
        }

        public Builder wrappedDek(byte[] wrappedDek) {
            this.wrappedDek = wrappedDek;
            return this;
        }

        public Builder kmsKeyReference(String kmsKeyReference) {
            this.kmsKeyReference = kmsKeyReference;
            return this;
        }

        public Builder status(BackupStatus status) {
            this.status = status;
            return this;
        }

        public Builder encryptedData(byte[] encryptedData) {
            this.encryptedData = encryptedData;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Backup build() {
            return new Backup(id, ownerId, originalFilename, objectPath, originalSizeBytes,
                    encryptedSizeBytes, mimeType, encryptionAlgorithm, formatVersion,
                    nonce, authenticationTag, wrappedDek, kmsKeyReference, status,
                    encryptedData, createdAt, updatedAt);
        }
    }
}
