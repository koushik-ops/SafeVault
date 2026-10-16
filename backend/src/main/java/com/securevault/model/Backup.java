package com.securevault.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "backups")
public class Backup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "object_path", nullable = false, unique = true, length = 1000)
    private String objectPath;

    @Column(name = "original_size_bytes", nullable = false)
    private Long originalSizeBytes;

    @Column(name = "encrypted_size_bytes")
    private Long encryptedSizeBytes;

    @Column(name = "mime_type", nullable = false, length = 255)
    private String mimeType;

    @Column(name = "encryption_algorithm", nullable = false, length = 50)
    private String encryptionAlgorithm = "AES/GCM/NoPadding";

    @Column(name = "format_version", nullable = false)
    private Integer formatVersion = 1;

    @Column(name = "nonce", nullable = false)
    private byte[] nonce;

    @Column(name = "authentication_tag")
    private byte[] authenticationTag;

    @Column(name = "wrapped_dek", nullable = false)
    private byte[] wrappedDek;

    @Column(name = "kms_key_reference", nullable = false, length = 500)
    private String kmsKeyReference;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private BackupStatus status = BackupStatus.COMPLETED;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Backup() {
    }

    public Backup(UUID id, UUID ownerId, String originalFilename, String objectPath,
                  Long originalSizeBytes, Long encryptedSizeBytes, String mimeType,
                  String encryptionAlgorithm, Integer formatVersion, byte[] nonce,
                  byte[] authenticationTag, byte[] wrappedDek, String kmsKeyReference,
                  BackupStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
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
        this.createdAt = createdAt != null ? createdAt : OffsetDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
                    createdAt, updatedAt);
        }
    }
}
