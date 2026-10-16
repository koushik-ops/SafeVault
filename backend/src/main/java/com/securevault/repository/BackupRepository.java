package com.securevault.repository;

import com.securevault.model.Backup;
import com.securevault.model.BackupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackupRepository extends JpaRepository<Backup, UUID> {

    /**
     * Find active backups for a specific owner and objectPath.
     */
    @Query("SELECT b FROM Backup b WHERE b.ownerId = :ownerId AND b.objectPath = :objectPath AND b.status != 'DELETED'")
    List<Backup> findActiveByOwnerIdAndObjectPath(@Param("ownerId") UUID ownerId, @Param("objectPath") String objectPath);

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
    @Query("SELECT b FROM Backup b WHERE b.ownerId = :ownerId " +
           "AND b.status != 'DELETED' " +
           "AND LOWER(b.originalFilename) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY b.createdAt DESC")
    Page<Backup> searchByFilename(@Param("ownerId") UUID ownerId,
                                   @Param("search") String search,
                                   Pageable pageable);

    /**
     * Count backups for a specific owner (excluding deleted).
     */
    long countByOwnerIdAndStatusNot(UUID ownerId, BackupStatus excludeStatus);

    /**
     * Sum of original file sizes for a specific owner.
     */
    @Query("SELECT COALESCE(SUM(b.encryptedSizeBytes), 0) FROM Backup b " +
           "WHERE b.ownerId = :ownerId AND b.status != 'DELETED'")
    long sumEncryptedSizeByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Find the most recent backup for a specific owner.
     */
    Optional<Backup> findFirstByOwnerIdAndStatusNotOrderByCreatedAtDesc(
        UUID ownerId, BackupStatus excludeStatus);
}
