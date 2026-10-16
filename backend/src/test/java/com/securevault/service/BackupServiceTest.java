package com.securevault.service;

import com.securevault.crypto.AesGcmEncryptionService;
import com.securevault.crypto.KeyManagementService;
import com.securevault.dto.UploadResponseDto;
import com.securevault.exception.BackupNotFoundException;
import com.securevault.exception.UnauthorizedAccessException;
import com.securevault.model.Backup;
import com.securevault.model.BackupStatus;
import com.securevault.repository.BackupRepository;
import com.securevault.storage.SupabaseStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private BackupRepository backupRepository;

    @Mock
    private KeyManagementService keyManagementService;

    @Mock
    private SupabaseStorageService storageService;

    private AesGcmEncryptionService encryptionService;
    private BackupService backupService;

    private final UUID user1 = UUID.randomUUID();
    private final UUID user2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        encryptionService = new AesGcmEncryptionService();
        backupService = new BackupService(
                backupRepository,
                encryptionService,
                keyManagementService,
                storageService
        );
    }

    @Test
    @DisplayName("Should successfully upload, encrypt, and persist backup")
    void testUploadAndEncryptSuccess() throws IOException {
        byte[] testDek = new byte[32];
        byte[] testWrappedDek = new byte[48];
        KeyManagementService.DataKey mockDataKey = new KeyManagementService.DataKey(
                testDek, testWrappedDek, "test-kms-key-ref"
        );

        when(keyManagementService.generateDataKey()).thenReturn(mockDataKey);
        when(storageService.uploadEncryptedFile(any(), any())).thenReturn("users/test/obj.enc");
        when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "tax_return_2025.pdf", "application/pdf",
                "CONFIDENTIAL PDF CONTENT".getBytes(StandardCharsets.UTF_8)
        );

        UploadResponseDto response = backupService.uploadAndEncrypt(user1, file);

        assertNotNull(response);
        assertEquals("tax_return_2025.pdf", response.originalFilename());
        assertEquals(BackupStatus.COMPLETED, response.status());

        // Verify that Supabase was invoked with encrypted bytes
        verify(storageService, times(1)).uploadEncryptedFile(any(), any());
        verify(backupRepository, times(1)).save(any(Backup.class));
    }

    @Test
    @DisplayName("Should reject access to another user's backup with UnauthorizedAccessException")
    void testPreventCrossUserAccess() {
        UUID backupId = UUID.randomUUID();
        Backup backupOwnedByUser1 = Backup.builder()
                .id(backupId)
                .ownerId(user1)
                .originalFilename("secret.docx")
                .objectPath("users/" + user1 + "/obj.enc")
                .originalSizeBytes(100L)
                .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .status(BackupStatus.COMPLETED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(backupRepository.findById(backupId)).thenReturn(Optional.of(backupOwnedByUser1));

        // User2 attempts to access User1's backup
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            backupService.getBackupById(user2, backupId);
        });

        assertTrue(exception.getMessage().contains("permission"));
    }

    @Test
    @DisplayName("Should reject files exceeding 25 MB size limit")
    void testRejectOversizedFiles() {
        byte[] largeBytes = new byte[26 * 1024 * 1024]; // 26 MB
        MockMultipartFile file = new MockMultipartFile("file", "huge.csv", "text/csv", largeBytes);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            backupService.uploadAndEncrypt(user1, file);
        });

        assertTrue(exception.getMessage().contains("25 MB"));
    }

    @Test
    @DisplayName("Should clean up cloud storage if database persistence fails")
    void testCleanupStorageOnDatabaseFailure() throws IOException {
        byte[] testDek = new byte[32];
        KeyManagementService.DataKey mockDataKey = new KeyManagementService.DataKey(
                testDek, new byte[48], "kms-ref"
        );

        when(keyManagementService.generateDataKey()).thenReturn(mockDataKey);
        when(storageService.uploadEncryptedFile(any(), any())).thenReturn("users/test/obj.enc");
        when(backupRepository.save(any(Backup.class))).thenThrow(new RuntimeException("DB Connection Timeout"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.xlsx", "application/vnd.ms-excel",
                "EXCEL DATA".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(RuntimeException.class, () -> {
            backupService.uploadAndEncrypt(user1, file);
        });

        // Verify storage cleanup was called
        verify(storageService, times(1)).deleteEncryptedFile(any());
    }
}
