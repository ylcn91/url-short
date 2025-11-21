package com.urlshort.repository;

import com.urlshort.domain.CustomDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CustomDomain entity.
 * Provides database operations for custom domain management and verification.
 */
@Repository
public interface CustomDomainRepository extends JpaRepository<CustomDomain, Long> {

    /**
     * Finds a custom domain by its domain name.
     * Used for domain verification and redirect resolution.
     *
     * @param domain the domain name (e.g., "go.acme.com")
     * @return Optional containing the custom domain if found
     */
    Optional<CustomDomain> findByDomain(String domain);

    /**
     * Retrieves all custom domains for a workspace.
     * Used for domain management dashboard.
     *
     * @param workspaceId the workspace ID
     * @return list of custom domains
     */
    List<CustomDomain> findByWorkspaceId(Long workspaceId);

    /**
     * Finds the default domain for a workspace.
     * Used when no specific domain is requested.
     *
     * @param workspaceId the workspace ID
     * @param isDefault true to find the default domain
     * @return Optional containing the default domain if set
     */
    Optional<CustomDomain> findByWorkspaceIdAndIsDefault(Long workspaceId, Boolean isDefault);

    /**
     * Finds a custom domain by its verification token.
     * Used during DNS verification process.
     *
     * @param verificationToken the verification token
     * @return Optional containing the domain if found
     */
    Optional<CustomDomain> findByVerificationToken(String verificationToken);

    /**
     * Counts verified domains for a workspace.
     * Used for quota management.
     *
     * @param workspaceId the workspace ID
     * @param status the domain status
     * @return count of verified domains
     */
    long countByWorkspaceIdAndStatus(Long workspaceId, String status);

    /**
     * Checks if a domain already exists.
     * Used to prevent duplicate domain registration.
     *
     * @param domain the domain name
     * @return true if domain exists
     */
    boolean existsByDomain(String domain);
}
