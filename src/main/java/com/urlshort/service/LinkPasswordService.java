package com.urlshort.service;

import com.urlshort.dto.LinkPasswordRequest;
import com.urlshort.dto.LinkPasswordResponse;
import com.urlshort.dto.PasswordValidationRequest;
import com.urlshort.dto.PasswordValidationResponse;

/**
 * Service interface for password-protected links.
 * <p>
 * Provides operations for adding, validating, and removing password protection
 * on short links. Uses BCrypt for secure password hashing.
 * </p>
 */
public interface LinkPasswordService {

    /**
     * Adds password protection to a short link.
     * <p>
     * Hashes the password using BCrypt before storing.
     * </p>
     *
     * @param shortLinkId the short link ID
     * @param request the password request
     * @return the created password protection
     * @throws IllegalArgumentException if link already has password
     */
    LinkPasswordResponse addPassword(Long shortLinkId, LinkPasswordRequest request);

    /**
     * Validates a password for a protected link.
     * <p>
     * Checks if the link is locked due to failed attempts.
     * Compares provided password with stored hash.
     * Records failed attempts and locks if threshold exceeded.
     * </p>
     *
     * @param shortLinkId the short link ID
     * @param request the password validation request
     * @return validation result with access token if successful
     */
    PasswordValidationResponse validatePassword(Long shortLinkId, PasswordValidationRequest request);

    /**
     * Checks if a short link has password protection.
     *
     * @param shortLinkId the short link ID
     * @return true if password protected
     */
    boolean isPasswordProtected(Long shortLinkId);

    /**
     * Removes password protection from a short link.
     *
     * @param shortLinkId the short link ID
     */
    void removePassword(Long shortLinkId);

    /**
     * Resets failed attempts for a locked link.
     *
     * @param shortLinkId the short link ID
     */
    void resetFailedAttempts(Long shortLinkId);
}
