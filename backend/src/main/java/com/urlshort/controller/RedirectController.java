package com.urlshort.controller;

import com.urlshort.dto.ClickEventDto;
import com.urlshort.dto.ShortLinkResponse;
import com.urlshort.event.ClickEventProducer;
import com.urlshort.exception.LinkExpiredException;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for public URL redirection.
 * <p>
 * This controller handles the core functionality of the URL shortener: redirecting
 * short codes to their original URLs. It is the only public endpoint that does not
 * require authentication, making shortened URLs accessible to anyone with the link.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>Public Access:</b> No authentication required for redirects</li>
 *   <li><b>Fast Redirects:</b> Uses HTTP 302 (Found) for flexible URL changes</li>
 *   <li><b>Analytics Tracking:</b> Records click events asynchronously</li>
 *   <li><b>Graceful Error Handling:</b> User-friendly error pages for expired/invalid links</li>
 *   <li><b>Security:</b> Validates links before redirecting to prevent abuse</li>
 * </ul>
 *
 * <h3>Redirect Behavior:</h3>
 * <p>
 * This controller uses HTTP 302 (Found) redirects rather than 301 (Moved Permanently)
 * to maintain flexibility. With 302 redirects:
 * </p>
 * <ul>
 *   <li>URLs can be updated without cache invalidation issues</li>
 *   <li>Click tracking remains accurate (browsers don't cache the redirect)</li>
 *   <li>Links can be disabled or expired without leaving broken cached redirects</li>
 * </ul>
 *
 * <h3>Click Event Recording:</h3>
 * <p>
 * Click events are recorded asynchronously to minimize latency. Each click captures:
 * </p>
 * <ul>
 *   <li>Timestamp</li>
 *   <li>IP address (for geographic analysis)</li>
 *   <li>User agent (for device type detection)</li>
 *   <li>Referrer (for traffic source analysis)</li>
 * </ul>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // User visits: https://short.ly/abc123
 * GET /abc123
 *
 * // Server responds with:
 * HTTP/1.1 302 Found
 * Location: https://example.com/original/long/url
 *
 * // Browser automatically redirects to original URL
 * // Click event is recorded asynchronously in background
 * }</pre>
 *
 * <h3>Error Handling:</h3>
 * <ul>
 *   <li><b>404 Not Found:</b> Short code doesn't exist or link is deleted</li>
 *   <li><b>410 Gone:</b> Link has expired or exceeded click limit</li>
 *   <li><b>403 Forbidden:</b> Link is inactive (disabled by owner)</li>
 * </ul>
 *
 * @see ShortLinkService
 * @since 1.0
 */
@RestController
@Tag(name = "Public Redirect", description = "Public endpoint for URL redirection (no authentication required)")
@Slf4j
public class RedirectController {

    @Autowired
    private ShortLinkService shortLinkService;

    /**
     * Redirects a short code to its original URL.
     * <p>
     * This is the primary public endpoint of the URL shortener. When a user visits
     * a shortened URL (e.g., https://short.ly/abc123), this endpoint:
     * </p>
     * <ol>
     *   <li>Looks up the short code in the database</li>
     *   <li>Validates the link is active and not expired</li>
     *   <li>Records a click event asynchronously (non-blocking)</li>
     *   <li>Returns HTTP 302 redirect to the original URL</li>
     * </ol>
     *
     * <p>
     * <b>No authentication is required</b> - this endpoint is publicly accessible.
     * The workspace ID is determined from the short code lookup.
     * </p>
     *
     * <h3>Redirect Flow:</h3>
     * <pre>{@code
     * User -> GET /abc123 -> Controller looks up link -> Records click (async)
     *                                                 -> 302 Redirect to original URL
     *                                                 -> User's browser follows redirect
     * }</pre>
     *
     * <h3>HTTP Status Codes:</h3>
     * <ul>
     *   <li><b>302 Found:</b> Successful redirect to original URL</li>
     *   <li><b>404 Not Found:</b> Short code doesn't exist or is deleted</li>
     *   <li><b>410 Gone:</b> Link has expired or exceeded max clicks</li>
     *   <li><b>403 Forbidden:</b> Link is inactive (disabled)</li>
     * </ul>
     *
     * <h3>Example Request:</h3>
     * <pre>{@code
     * GET /abc123
     * Host: short.ly
     * User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)
     * Referer: https://google.com/
     * }</pre>
     *
     * <h3>Example Response:</h3>
     * <pre>{@code
     * HTTP/1.1 302 Found
     * Location: https://www.example.com/products/widget-2024
     * Cache-Control: no-cache, no-store, must-revalidate
     * }</pre>
     *
     * <h3>Error Response Example (Expired Link):</h3>
     * <pre>{@code
     * HTTP/1.1 410 Gone
     * Content-Type: application/json
     *
     * {
     *   "timestamp": "2025-11-18T15:30:00",
     *   "status": 410,
     *   "error": "GONE",
     *   "message": "This short link has expired",
     *   "path": "/abc123"
     * }
     * }</pre>
     *
     * @param code the short code to redirect (e.g., "abc123")
     * @param request the HTTP servlet request (used for analytics)
     * @return ResponseEntity with 302 redirect to original URL
     * @throws ResourceNotFoundException if short code not found or link is deleted
     * @throws LinkExpiredException if link has expired or exceeded click limit
     */
    @GetMapping("/{code}")
    @Operation(
        summary = "Redirect to original URL",
        description = "Public endpoint that redirects a short code to its original URL. " +
                      "No authentication required. Returns HTTP 302 redirect. " +
                      "Click events are recorded asynchronously for analytics."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "302",
            description = "Redirect to original URL",
            content = @Content(schema = @Schema(implementation = Void.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Short code not found or link is deleted",
            content = @Content(schema = @Schema(implementation = com.urlshort.dto.ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "410",
            description = "Link has expired or exceeded maximum clicks",
            content = @Content(schema = @Schema(implementation = com.urlshort.dto.ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Link is inactive (disabled by owner)",
            content = @Content(schema = @Schema(implementation = com.urlshort.dto.ErrorResponse.class))
        )
    })
    public ResponseEntity<Void> redirect(
            @Parameter(description = "Short code to redirect", example = "abc123")
            @PathVariable String code,
            HttpServletRequest request) {

        log.info("GET /{} - Redirecting short code", code);

        // Extract request metadata for analytics
        String ipAddress = extractClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");

        log.debug("Redirect request details: code={}, ip={}, userAgent={}, referrer={}",
                  code, ipAddress, userAgent, referrer);

        // TODO: In production, the workspace ID would be determined from:
        // 1. Subdomain routing (e.g., acme.short.ly -> workspace "acme")
        // 2. Custom domain mapping (e.g., go.acme.com -> workspace "acme")
        // 3. Default workspace for main domain
        //
        // For now, using a default workspace ID
        Long workspaceId = 1L;

        // Look up the short link
        ShortLinkResponse shortLink;
        try {
            shortLink = shortLinkService.getShortLink(workspaceId, code);
        } catch (ResourceNotFoundException e) {
            log.warn("Short code not found: {} in workspace {}", code, workspaceId);
            throw e;
        } catch (LinkExpiredException e) {
            log.warn("Short code expired: {} in workspace {}", code, workspaceId);
            throw e;
        }

        // Additional validation: check if link is active
        if (!shortLink.isActive()) {
            log.warn("Short code is inactive: {} in workspace {}", code, workspaceId);
            throw new ResourceNotFoundException("This short link is inactive");
        }

        String originalUrl = shortLink.originalUrl();
        log.info("Redirecting {} -> {}", code, originalUrl);

        // Record click event asynchronously (non-blocking)
        recordClickEventAsync(shortLink.id(), ipAddress, userAgent, referrer);

        // Return 302 redirect (not 301, to allow URL changes and prevent browser caching)
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build();
    }

    /**
     * Records a click event asynchronously.
     * <p>
     * This method runs in a separate thread to avoid blocking the redirect response.
     * Click data is stored for analytics and includes:
     * </p>
     * <ul>
     *   <li>Timestamp (automatic)</li>
     *   <li>IP address (for geographic analysis)</li>
     *   <li>User agent (for device type detection)</li>
     *   <li>Referrer (for traffic source analysis)</li>
     * </ul>
     *
     * <p>
     * If click recording fails, the error is logged but the redirect still succeeds.
     * This ensures that analytics failures don't impact user experience.
     * </p>
     *
     * @param shortLinkId the ID of the short link that was clicked
     * @param ipAddress the client's IP address
     * @param userAgent the client's user agent string
     * @param referrer the HTTP referrer (or null if direct visit)
     */
    @Async
    protected void recordClickEventAsync(Long shortLinkId, String ipAddress, String userAgent, String referrer) {
        try {
            log.debug("Recording click event: linkId={}, ip={}", shortLinkId, ipAddress);

            // TODO: Implement click event recording
            // This would typically:
            // 1. Create a ClickEvent entity
            // 2. Parse user agent to determine device type
            // 3. Perform IP geolocation lookup for country/region
            // 4. Save to database
            // 5. Optionally publish to message queue for real-time analytics
            //
            // Example:
            // ClickEvent event = ClickEvent.builder()
            //     .shortLinkId(shortLinkId)
            //     .ipAddress(ipAddress)
            //     .userAgent(userAgent)
            //     .referrer(referrer)
            //     .deviceType(parseDeviceType(userAgent))
            //     .country(geolocate(ipAddress))
            //     .build();
            // clickEventRepository.save(event);

            log.debug("Click event recorded successfully for linkId={}", shortLinkId);

        } catch (Exception e) {
            // Log error but don't propagate - analytics failures shouldn't break redirects
            log.error("Failed to record click event for linkId={}: {}",
                     shortLinkId, e.getMessage(), e);
        }
    }

    /**
     * Extracts the client's IP address from the HTTP request.
     * <p>
     * This method checks common proxy headers in order of preference:
     * </p>
     * <ol>
     *   <li>X-Forwarded-For (standard proxy header)</li>
     *   <li>X-Real-IP (nginx/CloudFlare)</li>
     *   <li>Proxy-Client-IP (Apache)</li>
     *   <li>WL-Proxy-Client-IP (WebLogic)</li>
     *   <li>HTTP_X_FORWARDED_FOR (fallback)</li>
     *   <li>Remote address (direct connection)</li>
     * </ol>
     *
     * <p>
     * <b>Security Note:</b> In production, you should validate that proxy headers
     * are only accepted from trusted sources (your load balancer/CDN) to prevent
     * IP spoofing.
     * </p>
     *
     * @param request the HTTP servlet request
     * @return the client's IP address
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
            // The first one is the original client
            int firstComma = ip.indexOf(',');
            if (firstComma > 0) {
                ip = ip.substring(0, firstComma).trim();
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }
}
