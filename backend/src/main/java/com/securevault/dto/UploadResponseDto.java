package com.securevault.dto;

import com.securevault.model.BackupStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UploadResponseDto(
        UUID id,
        String originalFilename,
        Long originalSizeBytes,
        Long encryptedSizeBytes,
        String encryptionAlgorithm,
        OffsetDateTime createdAt,
        BackupStatus status
) {}
