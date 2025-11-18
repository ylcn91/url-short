package com.urlshort.service;

import com.urlshort.dto.CreateShortLinkRequest;
import com.urlshort.dto.LinkStatsResponse;
import com.urlshort.dto.ShortLinkResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for URL shortening operations.
 * <p>
 * This interface defines the core business logic for creating, retrieving,
 * and managing shortened URLs. It implements the deterministic URL shortening
 * algorithm where the same URL in the same workspace always produces the same
 * short code.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 *   <li><b>Deterministic Behavior:</b> Same URL + workspace always returns same short code</li>
 *   <li><b>Idempotency:</b> Repeated calls with same input return same result</li>
 *   <li><b>Workspace Isolation:</b> Short codes are unique within a workspace</li>
 *   <li><b>Collision Handling:</b> Automatic retry with salt for rare hash collisions</li>
 * </ul>
 *
 * @see com.urlshort.service.impl.ShortLinkServiceImpl
 * @see com.urlshort.domain.ShortLink
 * @since 1.0
 */
public interface ShortLinkService {

    /**
     * Creates a new short link or returns an existing one if the URL already exists.
     * <p>
     * This method implements the deterministic algorithm specified in ALGORITHM_SPEC.md:
     * </p>
     * <ol>
     *   <li>Canonicalize the original URL</li>
     *   <li>Check if (workspace_id, normalized_url) already exists</li>
     *   <li>If exists, return existing short link (deterministic reuse)</li>
     *   <li>If not, generate short code using deterministic hash</li>
     *   <li>Handle collisions with retry salt (max 10 attempts)</li>
     *   <li>Save and return new short link</li>
     * </ol>
     * <p>
     * The method is idempotent: calling it multiple times with the same URL
     * in the same workspace returns the same short code without creating duplicates.
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * CreateShortLinkRequest request = CreateShortLinkRequest.builder()
     *     .originalUrl("https://example.com/very/long/path")
     *     .expiresAt(LocalDateTime.now().plusDays(30))
     *     .maxClicks(1000)
     *     .build();
     *
     * ShortLinkResponse response = service.createShortLink(workspaceId, request);
     * // Returns: {shortCode: "MaSgB7xKpQ", shortUrl: "https://short.ly/MaSgB7xKpQ", ...}
     *
     * // Calling again with same URL returns existing short code
     * ShortLinkResponse response2 = service.createShortLink(workspaceId, request);
     * // response.shortCode() equals response2.shortCode()
     * }</pre>
     *
     * @param workspaceId the ID of the workspace where the link will be created
     * @param request     the request containing URL and optional parameters
     * @return response containing the short code and link details
     * @throws com.urlshort.exception.InvalidUrlException if URL is malformed or invalid
     * @throws com.urlshort.exception.ResourceNotFoundException if workspace doesn't exist
     * @throws IllegalStateException if collision handling fails after max retries
     */
    ShortLinkResponse createShortLink(Long workspaceId, CreateShortLinkRequest request);

    /**
     * Retrieves a short link by its short code within a workspace.
     * <p>
     * This method looks up the link and verifies that it is usable
     * (active, not deleted, not expired, and not exceeding max clicks).
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * ShortLinkResponse link = service.getShortLink(workspaceId, "MaSgB7xKpQ");
     * // Returns link details if found and active
     * }</pre>
     *
     * @param workspaceId the workspace ID
     * @param shortCode   the short code to look up
     * @return the short link response
     * @throws com.urlshort.exception.ResourceNotFoundException if link not found
     * @throws com.urlshort.exception.LinkExpiredException if link has expired
     */
    ShortLinkResponse getShortLink(Long workspaceId, String shortCode);

    /**
     * Retrieves a short link by its original URL within a workspace.
     * <p>
     * This method canonicalizes the provided URL and looks up the existing
     * short link, if any. Useful for checking if a URL has already been shortened.
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * ShortLinkResponse link = service.getShortLinkByUrl(
     *     workspaceId,
     *     "https://example.com/path"
     * );
     * // Returns existing link for this URL, or throws exception if not found
     * }</pre>
     *
     * @param workspaceId the workspace ID
     * @param url         the original URL (will be canonicalized for lookup)
     * @return the short link response
     * @throws com.urlshort.exception.ResourceNotFoundException if no link exists for this URL
     * @throws com.urlshort.exception.InvalidUrlException if URL is malformed
     */
    ShortLinkResponse getShortLinkByUrl(Long workspaceId, String url);

    /**
     * Deletes a short link (soft delete).
     * <p>
     * This method marks the link as deleted without removing it from the database.
     * The link becomes inaccessible but data is preserved for analytics.
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * service.deleteShortLink(workspaceId, linkId);
     * // Link is soft-deleted and no longer accessible
     * }</pre>
     *
     * @param workspaceId the workspace ID
     * @param linkId      the ID of the link to delete
     * @throws com.urlshort.exception.ResourceNotFoundException if link not found
     */
    void deleteShortLink(Long workspaceId, Long linkId);

    /**
     * Lists all short links in a workspace with pagination.
     * <p>
     * Returns active (non-deleted) links ordered by creation date (newest first).
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
     * Page<ShortLinkResponse> links = service.listShortLinks(workspaceId, pageable);
     * // Returns first 20 links in workspace
     * }</pre>
     *
     * @param workspaceId the workspace ID
     * @param pageable    pagination parameters
     * @return page of short link responses
     */
    Page<ShortLinkResponse> listShortLinks(Long workspaceId, Pageable pageable);

    /**
     * Retrieves analytics and statistics for a short link.
     * <p>
     * Returns aggregated click data including:
     * </p>
     * <ul>
     *   <li>Total clicks</li>
     *   <li>Clicks by date (time series)</li>
     *   <li>Clicks by country (geographic distribution)</li>
     *   <li>Clicks by referrer (traffic sources)</li>
     *   <li>Clicks by device type (mobile, desktop, tablet)</li>
     * </ul>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * LinkStatsResponse stats = service.getLinkStats(workspaceId, "MaSgB7xKpQ");
     * // Returns: {totalClicks: 1523, clicksByDate: {...}, clicksByCountry: {...}, ...}
     * }</pre>
     *
     * @param workspaceId the workspace ID
     * @param shortCode   the short code to get statistics for
     * @return link statistics response
     * @throws com.urlshort.exception.ResourceNotFoundException if link not found
     */
    LinkStatsResponse getLinkStats(Long workspaceId, String shortCode);
}
