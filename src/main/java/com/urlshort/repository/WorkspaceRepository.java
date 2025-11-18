package com.urlshort.repository;

import com.urlshort.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Workspace entity.
 * Provides database operations for workspace management.
 */
@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    /**
     * Finds a workspace by its slug, excluding soft-deleted records.
     * Used for workspace identification in URLs.
     *
     * @param slug the unique workspace slug
     * @return Optional containing the workspace if found
     */
    Optional<Workspace> findBySlugAndIsDeletedFalse(String slug);

    /**
     * Retrieves all active workspaces (not soft-deleted).
     *
     * @return list of active workspaces
     */
    List<Workspace> findByIsDeletedFalse();
}
