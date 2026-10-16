package com.securevault.dto;

import com.securevault.model.Backup;
import com.securevault.model.BackupStatus;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

public record BackupDto(
        UUID id,
        UUID ownerId,
        String originalFilename,
        String objectPath,
        Long originalSizeBytes,
        Long encryptedSizeBytes,
        String mimeType,
        String encryptionAlgorithm,
        Integer formatVersion,
        String nonce,
        String authenticationTag,
        String wrappedDek,
        String kmsKeyReference,
        BackupStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static BackupDto fromEntity(Backup backup) {
        return new BackupDto(
                backup.getId(),
                backup.getOwnerId(),
                backup.getOriginalFilename(),
                backup.getObjectPath(),
                backup.getOriginalSizeBytes(),
                backup.getEncryptedSizeBytes(),
                backup.getMimeType(),
                backup.getEncryptionAlgorithm(),
                backup.getFormatVersion(),
                backup.getNonce() != null ? Base64.getEncoder().encodeToString(backup.getNonce()) : null,
                backup.getAuthenticationTag() != null ? Base64.getEncoder().encodeToString(backup.getAuthenticationTag()) : null,
                backup.getWrappedDek() != null ? Base64.getEncoder().encodeToString(backup.getWrappedDek()) : null,
                backup.getKmsKeyReference(),
                backup.getStatus(),
                backup.getCreatedAt(),
                backup.getUpdatedAt()
        );
    }
}
