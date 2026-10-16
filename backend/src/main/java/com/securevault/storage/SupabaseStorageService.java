package com.securevault.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securevault.exception.StorageException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Supabase Storage REST API.
 * Uploads, downloads, and deletes encrypted backup objects in the private "encrypted-backups" bucket.
 * 
 * Uses Java 21 HttpClient with timeout and service-role key authorization.
 * Plaintext files NEVER touch Supabase Storage - only encrypted ciphertext bytes.
 */
@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.storage.bucket:encrypted-backups}")
    private String bucketName;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Uploads encrypted bytes to Supabase Storage (or local dev storage if placeholder URL).
     */
    public String uploadEncryptedFile(String objectPath, byte[] encryptedBytes) {
        if (encryptedBytes == null || encryptedBytes.length == 0) {
            throw new StorageException("Cannot upload empty or null encrypted data");
        }

        String sanitizedPath = sanitizeObjectPath(objectPath);

        // Local development storage fallback when Supabase URL is placeholder
        if (supabaseUrl.contains("placeholder") || supabaseUrl.contains("YOUR_PROJECT")) {
            try {
                Path localPath = Path.of("temp-storage", bucketName, sanitizedPath);
                Files.createDirectories(localPath.getParent());
                Files.write(localPath, encryptedBytes);
                log.info("Saved encrypted object to local dev storage: {}", localPath);
                return sanitizedPath;
            } catch (Exception e) {
                throw new StorageException("Failed to save to local dev storage", e);
            }
        }

        String endpoint = String.format("%s/storage/v1/object/%s/%s", 
                trimTrailingSlash(supabaseUrl), bucketName, sanitizedPath);

        log.debug("Uploading encrypted object of size {} bytes to {}", encryptedBytes.length, sanitizedPath);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("Content-Type", "application/octet-stream")
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(encryptedBytes))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully uploaded encrypted object to Supabase Storage: {}", sanitizedPath);
                return sanitizedPath;
            } else {
                log.error("Supabase Storage upload failed with status {}: {}", response.statusCode(), response.body());
                throw new StorageException("Failed to upload encrypted file to cloud storage. Code: " + response.statusCode());
            }
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Supabase Storage upload", e);
            throw new StorageException("Failed to upload encrypted file to Supabase Storage", e);
        }
    }

    /**
     * Downloads encrypted bytes from Supabase Storage (or local dev storage).
     */
    public byte[] downloadEncryptedFile(String objectPath) {
        String sanitizedPath = sanitizeObjectPath(objectPath);

        // Local development storage fallback
        if (supabaseUrl.contains("placeholder") || supabaseUrl.contains("YOUR_PROJECT")) {
            try {
                Path localPath = Path.of("temp-storage", bucketName, sanitizedPath);
                if (Files.exists(localPath)) {
                    return Files.readAllBytes(localPath);
                }
                throw new StorageException("Object not found in local dev storage: " + sanitizedPath);
            } catch (Exception e) {
                throw new StorageException("Failed to retrieve from local dev storage", e);
            }
        }

        String endpoint = String.format("%s/storage/v1/object/authenticated/%s/%s", 
                trimTrailingSlash(supabaseUrl), bucketName, sanitizedPath);

        log.debug("Downloading encrypted object from: {}", sanitizedPath);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully downloaded encrypted object from Supabase Storage: {}", sanitizedPath);
                return response.body();
            } else {
                log.error("Supabase Storage download failed with status {}: {}", response.statusCode(), new String(response.body()));
                throw new StorageException("Failed to download encrypted file from cloud storage. Code: " + response.statusCode());
            }
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Supabase Storage download", e);
            throw new StorageException("Failed to retrieve encrypted file from Supabase Storage", e);
        }
    }

    /**
     * Deletes an encrypted object from Supabase Storage (or local dev storage).
     */
    public void deleteEncryptedFile(String objectPath) {
        String sanitizedPath = sanitizeObjectPath(objectPath);

        if (supabaseUrl.contains("placeholder") || supabaseUrl.contains("YOUR_PROJECT")) {
            try {
                Path localPath = Path.of("temp-storage", bucketName, sanitizedPath);
                Files.deleteIfExists(localPath);
                return;
            } catch (Exception ignored) {
                return;
            }
        }

        String endpoint = String.format("%s/storage/v1/object/%s", 
                trimTrailingSlash(supabaseUrl), bucketName);

        try {
            Map<String, List<String>> body = Map.of("prefixes", List.of(sanitizedPath));
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully deleted encrypted object from Supabase Storage: {}", sanitizedPath);
            } else {
                log.warn("Supabase Storage delete returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to delete encrypted file from Supabase Storage: {}", sanitizedPath, e);
        }
    }

    private String sanitizeObjectPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Object path cannot be blank");
        }
        return path.replaceAll("\\.\\.", "").replaceAll("//+", "/").replaceAll("^/", "");
    }

    private String trimTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
