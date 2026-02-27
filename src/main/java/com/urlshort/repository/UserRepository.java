package com.urlshort.repository;

import com.urlshort.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity.
 * Provides database operations for user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by email address, excluding soft-deleted records.
     * Used for authentication and user lookup.
     *
     * @param email the user's email address
     * @return Optional containing the user if found
     */
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    /**
     * Retrieves all active users belonging to a specific workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of active users in the workspace
     */
    List<User> findByWorkspaceIdAndIsDeletedFalse(Long workspaceId);

    /**
     * Retrieves all active users belonging to a specific workspace with pagination.
     *
     * @param workspaceId the workspace ID
     * @param pageable pagination parameters
     * @return page of active users in the workspace
     */
    Page<User> findByWorkspaceIdAndIsDeletedFalse(Long workspaceId, Pageable pageable);

}
