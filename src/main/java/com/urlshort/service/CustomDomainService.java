package com.urlshort.service;

import com.urlshort.dto.CustomDomainRequest;
import com.urlshort.dto.CustomDomainResponse;
import com.urlshort.dto.DomainVerificationResponse;

import java.util.List;

/**
 * Service interface for custom domain management.
 * <p>
 * Provides operations for registering, verifying, and managing custom branded
 * domains for workspaces. Domains go through a verification process using DNS
 * TXT records before they can be used for short links.
 * </p>
 */
public interface CustomDomainService {

    /**
     * Registers a new custom domain for a workspace.
     * <p>
     * Creates a domain record with PENDING status and generates a verification
     * token for DNS TXT record validation.
     * </p>
     *
     * @param workspaceId the workspace ID
     * @param request the domain registration request
     * @return the created domain with verification token
     * @throws IllegalArgumentException if domain is already registered
     */
    CustomDomainResponse registerDomain(Long workspaceId, CustomDomainRequest request);

    /**
     * Verifies domain ownership via DNS TXT record check.
     * <p>
     * Checks if the TXT record with the verification token exists at the domain.
     * Updates status to VERIFIED if successful.
     * </p>
     *
     * @param domainId the domain ID
     * @return verification result with status
     */
    DomainVerificationResponse verifyDomain(Long domainId);

    /**
     * Sets a domain as the default for a workspace.
     * <p>
     * Only one domain can be default at a time. Previous default is unset.
     * </p>
     *
     * @param domainId the domain ID to set as default
     * @return the updated domain
     */
    CustomDomainResponse setAsDefault(Long domainId);

    /**
     * Retrieves all domains for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of custom domains
     */
    List<CustomDomainResponse> getWorkspaceDomains(Long workspaceId);

    /**
     * Retrieves a domain by name.
     *
     * @param domain the domain name
     * @return the domain if found
     */
    CustomDomainResponse getDomainByName(String domain);

    /**
     * Deletes a custom domain.
     *
     * @param domainId the domain ID
     */
    void deleteDomain(Long domainId);
}
