package com.securevault.controller;

import com.securevault.dto.BackupDto;
import com.securevault.dto.BackupSummaryDto;
import com.securevault.dto.UploadResponseDto;
import com.securevault.security.SecurityUtils;
import com.securevault.service.BackupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/backups")
public class BackupController {

    private final BackupService backupService;
    private final SecurityUtils securityUtils;

    public BackupController(BackupService backupService, SecurityUtils securityUtils) {
        this.backupService = backupService;
        this.securityUtils = securityUtils;
    }

    /**
     * Uploads and encrypts a file using AES-256-GCM envelope encryption.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDto> uploadBackup(@RequestParam("file") MultipartFile file) throws IOException {
        UUID userId = securityUtils.getCurrentUserId();
        UploadResponseDto response = backupService.uploadAndEncrypt(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists backups for the authenticated user with optional search and pagination.
     */
    @GetMapping
    public ResponseEntity<Page<BackupDto>> listBackups(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        UUID userId = securityUtils.getCurrentUserId();
        Sort sortOrder = sort.endsWith(",asc") 
                ? Sort.by("createdAt").ascending() 
                : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(size, 50), sortOrder);
        return ResponseEntity.ok(backupService.listBackups(userId, search, pageable));
    }

    /**
     * Returns dashboard summary metrics.
     */
    @GetMapping("/summary")
    public ResponseEntity<BackupSummaryDto> getSummary() {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(backupService.getSummary(userId));
    }

    /**
     * Gets metadata details for a specific backup.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BackupDto> getBackup(@PathVariable("id") UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(backupService.getBackupById(userId, id));
    }

    /**
     * Downloads raw encrypted backup file.
     */
    @GetMapping("/{id}/download-encrypted")
    public ResponseEntity<byte[]> downloadEncrypted(@PathVariable("id") UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        BackupService.RestoredFile file = backupService.downloadEncrypted(userId, id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file.data());
    }

    /**
     * Restores, unwraps DEK via KMS, verifies AES-GCM tag, and returns decrypted plaintext document.
     */
    @RequestMapping(value = "/{id}/restore", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<byte[]> restoreBackup(@PathVariable("id") UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        BackupService.RestoredFile file = backupService.restoreAndDecrypt(userId, id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.mimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(mediaType)
                .body(file.data());
    }

    /**
     * Deletes a backup from Supabase Storage and marks it deleted in PostgreSQL.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBackup(@PathVariable("id") UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        backupService.deleteBackup(userId, id);
        return ResponseEntity.noContent().build();
    }
}
