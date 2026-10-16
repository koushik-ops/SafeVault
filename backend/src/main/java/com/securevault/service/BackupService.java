package com.securevault.service;

import com.securevault.crypto.AesGcmEncryptionService;
import com.securevault.crypto.EncryptionResult;
import com.securevault.crypto.KeyManagementService;
import com.securevault.dto.BackupDto;
import com.securevault.dto.BackupSummaryDto;
import com.securevault.dto.UploadResponseDto;
import com.securevault.exception.BackupNotFoundException;
import com.securevault.exception.UnauthorizedAccessException;
import com.securevault.model.Backup;
import com.securevault.model.BackupStatus;
import com.securevault.repository.BackupRepository;
import com.securevault.storage.SupabaseStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final long MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB

    private final BackupRepository backupRepository;
    private final AesGcmEncryptionService encryptionService;
    private final KeyManagementService keyManagementService;
    private final SupabaseStorageService storageService;

    @Nullable
    private final MongoTemplate atlasMongoTemplate;

    public BackupService(
            BackupRepository backupRepository,
            AesGcmEncryptionService encryptionService,
            KeyManagementService keyManagementService,
            SupabaseStorageService storageService
    ) {
        this(backupRepository, encryptionService, keyManagementService, storageService, null);
    }

    @Autowired
    public BackupService(
            BackupRepository backupRepository,
            AesGcmEncryptionService encryptionService,
            KeyManagementService keyManagementService,
            SupabaseStorageService storageService,
            @Autowired(required = false) @Qualifier("atlasMongoTemplate") @Nullable MongoTemplate atlasMongoTemplate
    ) {
        this.backupRepository = backupRepository;
        this.encryptionService = encryptionService;
        this.keyManagementService = keyManagementService;
        this.storageService = storageService;
        this.atlasMongoTemplate = atlasMongoTemplate;
    }

    /**
     * Syncs a backup document to Atlas cloud MongoDB (fire-and-forget).
     * Never blocks or fails the primary local operation.
     */
    private void syncToAtlas(Backup backup) {
        if (atlasMongoTemplate != null) {
            try {
                atlasMongoTemplate.save(backup, "backups");
                log.debug("Synced backup {} to Atlas cloud", backup.getId());
            } catch (Exception e) {
                log.warn("Atlas cloud sync failed for backup {} (non-fatal): {}", backup.getId(), e.getMessage());
            }
        }
    }

    /**
     * Uploads, encrypts with AES-256-GCM via envelope encryption, and stores in Supabase Storage.
     * Never stores or logs plaintext keys or plaintext file contents.
     */
    @Transactional
    public UploadResponseDto uploadAndEncrypt(UUID userId, MultipartFile file) throws IOException {
        validateFile(file);

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        long originalSize = file.getSize();
        byte[] plaintextBytes = file.getBytes();

        UUID backupId = UUID.randomUUID();
        String objectPath = String.format("%s.enc", originalFilename);

        // 1. Generate fresh DEK using KMS envelope encryption
        KeyManagementService.DataKey dataKey = keyManagementService.generateDataKey();
        byte[] plaintextDek = dataKey.plaintextDek();

        EncryptionResult encryptionResult;
        try {
            // 2. Encrypt plaintext in backend memory using AES-256-GCM
            encryptionResult = encryptionService.encrypt(plaintextBytes, plaintextDek);
        } finally {
            // 3. Clear plaintext DEK from memory immediately
            AesGcmEncryptionService.wipeKey(plaintextDek);
        }

        // 4. Upload encrypted bytes only to Supabase Storage
        try {
            storageService.uploadEncryptedFile(objectPath, encryptionResult.ciphertextWithTag());
        } catch (Exception e) {
            log.error("Cloud storage upload failed for backup ID: {}", backupId, e);
            throw e;
        }

        // 5. Persist backup metadata and wrapped DEK in database
        Backup backup = Backup.builder()
                .id(backupId)
                .ownerId(userId)
                .originalFilename(originalFilename)
                .objectPath(objectPath)
                .originalSizeBytes(originalSize)
                .encryptedSizeBytes((long) encryptionResult.getEncryptedSizeBytes())
                .mimeType(mimeType)
                .encryptionAlgorithm(AesGcmEncryptionService.ALGORITHM)
                .formatVersion(encryptionResult.formatVersion())
                .nonce(encryptionResult.nonce())
                .authenticationTag(encryptionResult.authenticationTag())
                .wrappedDek(dataKey.wrappedDek())
                .kmsKeyReference(dataKey.keyReference())
                .status(BackupStatus.COMPLETED)
                .encryptedData(encryptionResult.ciphertextWithTag())
                .build();

        try {
            // Deactivate any previous active backups with the same objectPath to prevent DEK mismatch
            List<Backup> previous = backupRepository.findActiveByOwnerIdAndObjectPath(userId, objectPath);
            for (Backup prev : previous) {
                prev.setStatus(BackupStatus.DELETED);
                backupRepository.save(prev);
                syncToAtlas(prev);
            }

            Backup saved = backupRepository.save(backup);
            syncToAtlas(saved);
            log.info("Successfully created encrypted backup {} for user {}", saved.getId(), userId);
            return new UploadResponseDto(
                    saved.getId(),
                    saved.getOriginalFilename(),
                    saved.getOriginalSizeBytes(),
                    saved.getEncryptedSizeBytes(),
                    saved.getEncryptionAlgorithm(),
                    saved.getCreatedAt(),
                    saved.getStatus()
            );
        } catch (Exception e) {
            log.error("Failed to persist backup metadata. Cleaning up storage object: {}", objectPath, e);
            storageService.deleteEncryptedFile(objectPath);
            throw e;
        }
    }

    /**
     * Lists backups for an authenticated user with optional filename search.
     */
    @Transactional(readOnly = true)
    public Page<BackupDto> listBackups(UUID userId, String search, Pageable pageable) {
        Page<Backup> backups;
        if (search != null && !search.trim().isEmpty()) {
            backups = backupRepository.searchByFilename(userId, search.trim(), pageable);
        } else {
            backups = backupRepository.findByOwnerIdAndStatusNotOrderByCreatedAtDesc(userId, BackupStatus.DELETED, pageable);
        }
        return backups.map(BackupDto::fromEntity);
    }

    /**
     * Gets a single backup ensuring ownership.
     */
    @Transactional(readOnly = true)
    public BackupDto getBackupById(UUID userId, UUID backupId) {
        Backup backup = getOwnedBackup(backupId, userId);
        return BackupDto.fromEntity(backup);
    }

    /**
     * Downloads raw encrypted backup file from cloud storage.
     */
    @Transactional(readOnly = true)
    public RestoredFile downloadEncrypted(UUID userId, UUID backupId) {
        Backup backup = getOwnedBackup(backupId, userId);
        byte[] encryptedBytes;
        try {
            encryptedBytes = storageService.downloadEncryptedFile(backup.getObjectPath());
        } catch (Exception ex) {
            if (backup.getEncryptedData() != null && backup.getEncryptedData().length > 0) {
                log.info("Cloud storage unavailable, retrieved encrypted file copy directly from MongoDB for {}", backupId);
                encryptedBytes = backup.getEncryptedData();
            } else {
                throw ex;
            }
        }
        return new RestoredFile(
                backup.getOriginalFilename() + ".enc",
                "application/octet-stream",
                encryptedBytes
        );
    }

    /**
     * Restores and decrypts an authorized backup.
     * Unwraps the DEK via KMS, verifies AES-GCM authentication tag, and returns original plaintext bytes.
     */
    @Transactional(readOnly = true)
    public RestoredFile restoreAndDecrypt(UUID userId, UUID backupId) {
        Backup backup = getOwnedBackup(backupId, userId);

        // 1. Download encrypted ciphertext from Supabase Storage (with MongoDB copy fallback)
        byte[] encryptedBytes;
        try {
            encryptedBytes = storageService.downloadEncryptedFile(backup.getObjectPath());
        } catch (Exception ex) {
            if (backup.getEncryptedData() != null && backup.getEncryptedData().length > 0) {
                log.info("Cloud storage unavailable, restored from MongoDB encrypted file copy for {}", backupId);
                encryptedBytes = backup.getEncryptedData();
            } else {
                throw ex;
            }
        }

        // 2. Unwrap DEK using KMS
        byte[] plaintextDek = keyManagementService.decryptDataKey(
                backup.getWrappedDek(),
                backup.getKmsKeyReference()
        );

        byte[] restoredPlaintext;
        try {
            // 3. Decrypt and verify 128-bit authentication tag
            restoredPlaintext = encryptionService.decrypt(
                    encryptedBytes,
                    backup.getNonce(),
                    plaintextDek
            );
        } finally {
            // 4. Zero out decrypted DEK memory
            AesGcmEncryptionService.wipeKey(plaintextDek);
        }

        log.info("Successfully restored and decrypted backup {} for user {}", backupId, userId);
        return new RestoredFile(
                backup.getOriginalFilename(),
                backup.getMimeType(),
                restoredPlaintext
        );
    }

    /**
     * Deletes a backup: removes from Supabase Storage and updates DB status.
     */
    @Transactional
    public void deleteBackup(UUID userId, UUID backupId) {
        Backup backup = getOwnedBackup(backupId, userId);
        // Delete cloud object
        storageService.deleteEncryptedFile(backup.getObjectPath());
        // Soft delete in database
        backup.setStatus(BackupStatus.DELETED);
        backupRepository.save(backup);
        syncToAtlas(backup);
        log.info("Deleted backup {} for user {}", backupId, userId);
    }

    /**
     * Summary metrics for user dashboard.
     */
    @Transactional(readOnly = true)
    public BackupSummaryDto getSummary(UUID userId) {
        long count = backupRepository.countByOwnerIdAndStatusNot(userId, BackupStatus.DELETED);
        long totalEncryptedSize = backupRepository.sumEncryptedSizeByOwnerId(userId);
        Optional<Backup> mostRecent = backupRepository.findFirstByOwnerIdAndStatusNotOrderByCreatedAtDesc(
                userId, BackupStatus.DELETED
        );

        return new BackupSummaryDto(
                count,
                totalEncryptedSize,
                mostRecent.map(BackupDto::fromEntity).orElse(null)
        );
    }

    private Backup getOwnedBackup(UUID backupId, UUID userId) {
        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new BackupNotFoundException("Backup not found: " + backupId));

        if (!backup.getOwnerId().equals(userId)) {
            log.warn("Access denied: user {} attempted to access backup {} owned by {}", userId, backupId, backup.getOwnerId());
            throw new UnauthorizedAccessException("You do not have permission to access this backup");
        }

        if (backup.getStatus() == BackupStatus.DELETED) {
            throw new BackupNotFoundException("Backup has been deleted: " + backupId);
        }

        return backup;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided for upload");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 25 MB limit");
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed-file";
        }
        // Strip directory paths and dangerous characters
        String clean = filename.replace("\\", "/");
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0) {
            clean = clean.substring(lastSlash + 1);
        }
        return clean.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record RestoredFile(String filename, String mimeType, byte[] data) {}
}
