package com.securevault.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BackupNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(BackupNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(
            "NOT_FOUND", ex.getMessage()
        ));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse(
            "FORBIDDEN", "You do not have access to this resource."
        ));
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<Map<String, Object>> handleEncryption(EncryptionException ex) {
        log.error("Encryption operation failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse(
            "ENCRYPTION_ERROR", "An error occurred during the encryption operation."
        ));
    }

    @ExceptionHandler(DecryptionException.class)
    public ResponseEntity<Map<String, Object>> handleDecryption(DecryptionException ex) {
        log.error("Decryption operation failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse(
            "DECRYPTION_ERROR", "Unable to restore this backup because its integrity check failed."
        ));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, Object>> handleStorage(StorageException ex) {
        log.error("Storage operation failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse(
            "STORAGE_ERROR", "An error occurred during the storage operation."
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse(
            "FILE_TOO_LARGE", "File size exceeds the maximum allowed limit of 25 MB."
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse(
            "BAD_REQUEST", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse(
            "INTERNAL_ERROR", "An unexpected error occurred. Please try again later."
        ));
    }

    private Map<String, Object> errorResponse(String code, String message) {
        return Map.of(
            "error", code,
            "message", message,
            "timestamp", Instant.now().toString()
        );
    }
}
