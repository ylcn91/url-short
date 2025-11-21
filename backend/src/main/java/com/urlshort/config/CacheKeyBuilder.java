package com.urlshort.config;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for building consistent cache keys across the application.
 *
 * Provides methods to generate cache keys following the pattern:
 * - shortlink:{workspaceId}:{code}
 * - workspace:{workspaceId}
 * - user:{userId}
 *
 * This ensures consistency and makes cache debugging easier.
 * Keys are logged at DEBUG level for troubleshooting.
 */
@Slf4j
@UtilityClass
public class CacheKeyBuilder {

    /**
     * Build cache key for short link lookup by code.
     * Pattern: shortlink:{workspaceId}:{code}
     *
     * @param workspaceId the workspace ID
     * @param code the short link code
     * @return the cache key
     */
    public static String buildShortLinkKey(Long workspaceId, String code) {
        String key = String.format("%s%d:%s", CacheConfig.SHORT_LINK_KEY_PREFIX, workspaceId, code);
        log.debug("Generated short link cache key: {}", key);
        return key;
    }

    /**
     * Build cache key for short link lookup by ID.
     * Pattern: shortlink:id:{id}
     *
     * @param id the short link ID
     * @return the cache key
     */
    public static String buildShortLinkByIdKey(Long id) {
        String key = String.format("%sid:%d", CacheConfig.SHORT_LINK_KEY_PREFIX, id);
        log.debug("Generated short link by ID cache key: {}", key);
        return key;
    }

    /**
     * Build cache key for workspace lookup.
     * Pattern: workspace:{workspaceId}
     *
     * @param workspaceId the workspace ID
     * @return the cache key
     */
    public static String buildWorkspaceKey(Long workspaceId) {
        String key = String.format("%s%d", CacheConfig.WORKSPACE_KEY_PREFIX, workspaceId);
        log.debug("Generated workspace cache key: {}", key);
        return key;
    }

    /**
     * Build cache key for workspace lookup by slug.
     * Pattern: workspace:slug:{slug}
     *
     * @param slug the workspace slug
     * @return the cache key
     */
    public static String buildWorkspaceBySlugKey(String slug) {
        String key = String.format("%sslug:%s", CacheConfig.WORKSPACE_KEY_PREFIX, slug);
        log.debug("Generated workspace by slug cache key: {}", key);
        return key;
    }

    /**
     * Build cache key for user lookup.
     * Pattern: user:{userId}
     *
     * @param userId the user ID
     * @return the cache key
     */
    public static String buildUserKey(Long userId) {
        String key = String.format("%s%d", CacheConfig.USER_KEY_PREFIX, userId);
        log.debug("Generated user cache key: {}", key);
        return key;
    }

    /**
     * Build cache key for user lookup by email.
     * Pattern: user:email:{email}
     *
     * @param email the user email
     * @return the cache key
     */
    public static String buildUserByEmailKey(String email) {
        String key = String.format("%semail:%s", CacheConfig.USER_KEY_PREFIX, email);
        log.debug("Generated user by email cache key: {}", key);
        return key;
    }

}
