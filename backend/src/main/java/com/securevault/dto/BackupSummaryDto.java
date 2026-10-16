package com.securevault.dto;

public record BackupSummaryDto(
        long totalBackups,
        long totalEncryptedBytes,
        BackupDto mostRecentBackup
) {}
