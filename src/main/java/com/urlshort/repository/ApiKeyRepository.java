package com.urlshort.repository;

import com.urlshort.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ApiKey entity.
 * Provides database operations for API key management and authentication.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /**
     * Finds an API key by its hashed value, excluding soft-deleted records.
     * Used for API authentication - the provided key is hashed before lookup.
     *
     * Note: API keys are stored as hashed values for security.
     * Never store or compare plain-text API keys.
     *
     * @param keyHash the hashed API key
     * @return Optional containing the API key if found
     */
    Optional<ApiKey> findByKeyHashAndIsDeletedFalse(String keyHash);

    /**
     * Retrieves all active API keys for a specific workspace.
     * Used for listing and managing API keys in the workspace dashboard.
     *
     * @param workspaceId the workspace ID
     * @return list of active API keys
     */
    List<ApiKey> findByWorkspaceIdAndIsDeletedFalse(Long workspaceId);

    /**
     * Updates the last used timestamp for an API key.
     * This is a performance optimization to avoid loading the entire entity
     * just to update a single timestamp field during API authentication.
     *
     * Note: This method requires @Modifying annotation as it performs an UPDATE operation.
     * Should be called within a transactional context.
     *
     * @param id the API key ID
     * @param now the current timestamp to set
     */
    @Modifying
    @Query("UPDATE ApiKey ak SET ak.lastUsedAt = :now WHERE ak.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("now") LocalDateTime now);
}
