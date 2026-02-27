package com.urlshort.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Interceptor that enforces rate limiting on HTTP requests.
 * Uses token bucket algorithm via Bucket4j.
 * Rate limiting strategies:
 * - Public redirect endpoints: IP-based rate limiting
 * - Management API endpoints: User/API key-based rate limiting
 * - Link creation endpoints: Stricter user-based rate limiting
 * When rate limit is exceeded:
 * - Returns HTTP 429 (Too Many Requests)
 * - Includes Retry-After header with seconds to wait
 * - Includes X-Rate-Limit-Retry-After-Seconds header
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;

    public RateLimitInterceptor(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean preHandle(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Object handler
    ) throws IOException {

        // Skip if rate limiting is disabled
        if (!rateLimitConfig.isEnabled()) {
            return true;
        }

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // Determine rate limit type based on request path
        Bucket bucket;
        String rateLimitKey;

        if (isRedirectRequest(requestUri)) {
            // Public redirect endpoints: IP-based rate limiting
            rateLimitKey = getClientIP(request);
            bucket = rateLimitConfig.resolveRedirectBucket(rateLimitKey);
            log.debug("Applying redirect rate limit for IP: {}", rateLimitKey);

        } else if (isLinkCreationRequest(requestUri, method)) {
            // Link creation: Stricter user-based rate limiting
            rateLimitKey = getUserIdentifier(request);
            bucket = rateLimitConfig.resolveCreationBucket(rateLimitKey);
            log.debug("Applying creation rate limit for user: {}", rateLimitKey);

        } else if (isManagementApiRequest(requestUri)) {
            // Management API: User-based rate limiting
            rateLimitKey = getUserIdentifier(request);
            bucket = rateLimitConfig.resolveManagementBucket(rateLimitKey);
            log.debug("Applying management rate limit for user: {}", rateLimitKey);

        } else {
            // No rate limiting for other endpoints
            return true;
        }

        // Try to consume one token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Request allowed
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            log.debug("Rate limit check passed for {}: {} remaining", rateLimitKey, probe.getRemainingTokens());
            return true;
        }

        // Rate limit exceeded
        long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
        response.addHeader("Retry-After", String.valueOf(waitForRefill));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String errorMessage = String.format(
            "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again in %d seconds.\",\"retryAfter\":%d}",
            waitForRefill, waitForRefill
        );

        response.getWriter().write(errorMessage);

        log.warn("Rate limit exceeded for {}: wait {} seconds", rateLimitKey, waitForRefill);

        return false;
    }

    /**
     * Checks if the request is a public redirect request (short code resolution).
     *
     * @param requestUri the request URI
     * @return true if this is a redirect request
     */
    private boolean isRedirectRequest(String requestUri) {
        // Redirect requests are typically GET /{shortCode}
        // They don't start with /api/v1
        return !requestUri.startsWith("/api/v1") &&
               !requestUri.startsWith("/actuator") &&
               !requestUri.startsWith("/swagger") &&
               requestUri.matches("/[A-Za-z0-9]{1,15}");
    }

    /**
     * Checks if the request is a link creation request.
     *
     * @param requestUri the request URI
     * @param method the HTTP method
     * @return true if this is a link creation request
     */
    private boolean isLinkCreationRequest(String requestUri, String method) {
        return "POST".equals(method) &&
               (requestUri.matches("/api/v1/workspaces/\\d+/links") ||
                requestUri.equals("/api/v1/links") ||
                requestUri.equals("/api/v1/links/bulk"));
    }

    /**
     * Checks if the request is a management API request.
     *
     * @param requestUri the request URI
     * @return true if this is a management API request
     */
    private boolean isManagementApiRequest(String requestUri) {
        return requestUri.startsWith("/api/v1");
    }

    /**
     * Extracts the client IP address from the request.
     * Considers X-Forwarded-For header for proxy scenarios.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Extracts a user identifier from the authenticated principal.
     * Falls back to IP address for unauthenticated requests.
     *
     * @param request the HTTP request
     * @return user identifier or IP address
     */
    private String getUserIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {
            // Use authenticated user's name (typically user ID or email)
            return authentication.getName();
        }

        // Fallback to IP for unauthenticated requests
        return "ip_" + getClientIP(request);
    }
}
