package com.securevault.repository;

import com.securevault.model.Backup;
import com.securevault.model.BackupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackupRepository extends MongoRepository<Backup, UUID> {

    /**
     * Find active backups for a specific owner and objectPath.
     */
    @Query("{ 'owner_id': ?0, 'object_path': ?1, 'status': { $ne: 'DELETED' } }")
    List<Backup> findActiveByOwnerIdAndObjectPath(UUID ownerId, String objectPath);

    /**
     * Find all backups for a specific owner, ordered by creation date descending.
     */
    Page<Backup> findByOwnerIdAndStatusNotOrderByCreatedAtDesc(
        UUID ownerId, BackupStatus excludeStatus, Pageable pageable);

    /**
     * Find a backup by ID only if owned by the specified user.
     */
    Optional<Backup> findByIdAndOwnerId(UUID id, UUID ownerId);

    /**
     * Search backups by filename for a specific owner.
     */
    @Query(value = "{ 'owner_id': ?0, 'status': { $ne: 'DELETED' }, 'original_filename': { $regex: ?1, $options: 'i' } }",
           sort = "{ 'created_at': -1 }")
    Page<Backup> searchByFilename(UUID ownerId, String search, Pageable pageable);

    /**
     * Count backups for a specific owner (excluding deleted).
     */
    long countByOwnerIdAndStatusNot(UUID ownerId, BackupStatus excludeStatus);

    /**
     * Find all backups for a specific owner (excluding deleted) for calculations.
     */
    List<Backup> findByOwnerIdAndStatusNot(UUID ownerId, BackupStatus excludeStatus);

    /**
     * Sum of encrypted file sizes for a specific owner.
     */
    default long sumEncryptedSizeByOwnerId(UUID ownerId) {
        return findByOwnerIdAndStatusNot(ownerId, BackupStatus.DELETED).stream()
                .mapToLong(b -> b.getEncryptedSizeBytes() != null ? b.getEncryptedSizeBytes() : 0L)
                .sum();
    }

    /**
     * Find the most recent backup for a specific owner.
     */
    Optional<Backup> findFirstByOwnerIdAndStatusNotOrderByCreatedAtDesc(
        UUID ownerId, BackupStatus excludeStatus);
}
