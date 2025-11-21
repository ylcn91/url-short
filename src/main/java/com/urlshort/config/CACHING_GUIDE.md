# URL Shortener Caching Configuration Guide

## Overview

This application implements a comprehensive caching strategy using Spring Cache with Redis as the primary cache provider and Caffeine as a local fallback.

## Cache Configuration

### Cache Names

Three cache stores are configured with different TTLs:

| Cache Name | Constant | TTL | Purpose |
|---|---|---|---|
| `shortlinks` | `CACHE_SHORT_LINKS` | 1 hour | Hot path optimization for URL redirects |
| `workspaces` | `CACHE_WORKSPACES` | 24 hours | Workspace metadata and configuration |
| `users` | `CACHE_USERS` | 24 hours | User profile and authentication data |

### Cache Key Patterns

Consistent key patterns are used throughout the application:

```
Short Links:    shortlink:{workspaceId}:{code}
Workspaces:    workspace:{workspaceId}
               workspace:slug:{slug}
Users:         user:{userId}
               user:email:{email}
```

**Use the `CacheKeyBuilder` utility class for generating keys.**

## Usage Examples

### 1. Basic Caching with @Cacheable

Cache method results with automatic hit/miss handling:

```java
@Service
@Slf4j
public class ShortLinkService {

    @Cacheable(
        cacheNames = CacheConfig.CACHE_SHORT_LINKS,
        key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkKey(#workspaceId, #code)",
        unless = "#result == null"
    )
    public ShortLink findByCode(Long workspaceId, String code) {
        log.debug("Fetching short link from database: {}, {}", workspaceId, code);
        return shortLinkRepository.findByWorkspaceIdAndCode(workspaceId, code)
            .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));
    }

    @Cacheable(
        cacheNames = CacheConfig.CACHE_SHORT_LINKS,
        key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#id)",
        unless = "#result == null"
    )
    public ShortLink findById(Long id) {
        log.debug("Fetching short link by ID from database: {}", id);
        return shortLinkRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));
    }
}
```

**How it works:**
- First call: Fetches from database, stores result in cache
- Subsequent calls: Returns cached value without database query
- `unless` prevents caching of null values
- On cache miss: Method is invoked and result is cached

### 2. Cache Updates with @CachePut

Update cache when the underlying data changes:

```java
@CachePut(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#result.id)"
)
public ShortLink updateShortLink(Long id, UpdateShortLinkRequest request) {
    log.debug("Updating short link: {}", id);

    ShortLink shortLink = shortLinkRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));

    shortLink.setTitle(request.getTitle());
    shortLink.setDescription(request.getDescription());

    // Always executes and updates the cache
    return shortLinkRepository.save(shortLink);
}
```

**How it works:**
- Method always executes (no cache hit)
- Result is stored in cache with the specified key
- `#result` refers to the return value
- Useful for ensuring cache consistency after updates

### 3. Cache Eviction with @CacheEvict

Remove cached values when data is deleted or invalidated:

```java
@CacheEvict(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#id)"
)
public void deleteShortLink(Long id) {
    log.debug("Deleting short link: {}", id);

    ShortLink shortLink = shortLinkRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));

    shortLinkRepository.delete(shortLink);
}
```

**Variations:**

```java
// Evict multiple related caches
@Caching(evict = {
    @CacheEvict(cacheNames = CacheConfig.CACHE_SHORT_LINKS, key = "#id"),
    @CacheEvict(cacheNames = CacheConfig.CACHE_WORKSPACES,
                key = "T(com.urlshort.config.CacheKeyBuilder).buildWorkspaceKey(#workspaceId)")
})
public void deleteShortLinkAndInvalidateWorkspace(Long id, Long workspaceId) {
    // Delete operation
}

// Evict all entries in a cache
@CacheEvict(cacheNames = CacheConfig.CACHE_SHORT_LINKS, allEntries = true)
public void invalidateAllShortLinks() {
    log.debug("Invalidating all short link cache entries");
}
```

### 4. Complex Operations with @Caching

Combine multiple cache operations:

```java
@Caching(
    cacheable = {
        @Cacheable(cacheNames = CacheConfig.CACHE_SHORT_LINKS,
                  key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#id)")
    },
    put = {
        @CachePut(cacheNames = CacheConfig.CACHE_WORKSPACES,
                 key = "T(com.urlshort.config.CacheKeyBuilder).buildWorkspaceKey(#result.workspaceId)")
    }
)
public ShortLink getShortLinkWithWorkspaceUpdate(Long id) {
    ShortLink shortLink = shortLinkRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));

    // Update workspace cache while fetching short link
    return shortLink;
}
```

### 5. Workspace Caching Example

```java
@Service
@Slf4j
public class WorkspaceService {

    @Cacheable(
        cacheNames = CacheConfig.CACHE_WORKSPACES,
        key = "T(com.urlshort.config.CacheKeyBuilder).buildWorkspaceKey(#workspaceId)",
        unless = "#result == null"
    )
    public Workspace findById(Long workspaceId) {
        log.debug("Fetching workspace from database: {}", workspaceId);
        return workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    @Cacheable(
        cacheNames = CacheConfig.CACHE_WORKSPACES,
        key = "T(com.urlshort.config.CacheKeyBuilder).buildWorkspaceBySlugKey(#slug)",
        unless = "#result == null"
    )
    public Workspace findBySlug(String slug) {
        log.debug("Fetching workspace by slug from database: {}", slug);
        return workspaceRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    @Caching(
        put = {
            @CachePut(cacheNames = CacheConfig.CACHE_WORKSPACES,
                     key = "T(com.urlshort.config.CacheKeyBuilder).buildWorkspaceKey(#result.id)"),
            @CachePut(cacheNames = CacheConfig.CACHE_WORKSPACES,
                     key = "T(com.urlshort.config.CacheKeyBuilder).buildWorkspaceBySlugKey(#result.slug)")
        }
    )
    public Workspace updateWorkspace(Long id, UpdateWorkspaceRequest request) {
        log.debug("Updating workspace: {}", id);
        Workspace workspace = workspaceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        workspace.setName(request.getName());
        workspace.setSlug(request.getSlug());

        return workspaceRepository.save(workspace);
    }

    @CacheEvict(
        cacheNames = CacheConfig.CACHE_WORKSPACES,
        allEntries = true
    )
    public void deleteWorkspace(Long id) {
        log.debug("Deleting workspace: {}", id);
        workspaceRepository.deleteById(id);
    }
}
```

### 6. User Caching Example

```java
@Service
@Slf4j
public class UserService {

    @Cacheable(
        cacheNames = CacheConfig.CACHE_USERS,
        key = "T(com.urlshort.config.CacheKeyBuilder).buildUserKey(#userId)",
        unless = "#result == null"
    )
    public User findById(Long userId) {
        log.debug("Fetching user from database: {}", userId);
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Cacheable(
        cacheNames = CacheConfig.CACHE_USERS,
        key = "T(com.urlshort.config.CacheKeyBuilder).buildUserByEmailKey(#email)",
        unless = "#result == null"
    )
    public User findByEmail(String email) {
        log.debug("Fetching user by email from database: {}", email);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @CacheEvict(
        cacheNames = CacheConfig.CACHE_USERS,
        key = "T(com.urlshort.config.CacheKeyBuilder).buildUserByEmailKey(#email)"
    )
    public void invalidateUserEmailCache(String email) {
        log.debug("Invalidating cache for user: {}", email);
    }
}
```

## Best Practices

### 1. Always Use Key Prefixes
Use the CacheKeyBuilder utility to ensure consistent key generation:
```java
key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkKey(#workspaceId, #code)"
```

### 2. Prevent Null Caching
Always include `unless = "#result == null"` to avoid caching null values:
```java
@Cacheable(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "...",
    unless = "#result == null"
)
```

### 3. Log Cache Operations
Log at DEBUG level for cache hits/misses:
```java
log.debug("Fetching from database - cache miss: {}", key);
```

### 4. Handle Cache Misses Gracefully
Always implement fallback logic:
```java
@Cacheable(...)
public ShortLink findByCode(Long workspaceId, String code) {
    return shortLinkRepository.findByWorkspaceIdAndCode(workspaceId, code)
        .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));
}
```

### 5. Use @CachePut for Updates
Ensure cache consistency after updates:
```java
@CachePut(cacheNames = CacheConfig.CACHE_SHORT_LINKS, key = "...")
public ShortLink updateShortLink(...) {
    // Update and return
}
```

### 6. Evict Related Caches
If updating affects multiple caches, evict all related entries:
```java
@Caching(
    evict = {
        @CacheEvict(cacheNames = CacheConfig.CACHE_SHORT_LINKS, ...),
        @CacheEvict(cacheNames = CacheConfig.CACHE_WORKSPACES, ...)
    }
)
```

### 7. Monitor Cache Performance
The CacheMetrics component tracks:
- Cache hits/misses per cache name
- Eviction counts
- Operation timing percentiles (50th, 95th, 99th)

Access metrics at: `http://localhost:8080/actuator/metrics/cache.hits`

### 8. Cache Invalidation Strategy

For batch operations, consider invalidating entire cache:
```java
@CacheEvict(cacheNames = CacheConfig.CACHE_SHORT_LINKS, allEntries = true)
public void importShortLinks(List<ShortLink> links) {
    shortLinkRepository.saveAll(links);
}
```

## Configuration by Profile

### Development (dev)
- Redis enabled with local connection
- Key prefix: `cache:dev:`
- Pool size: 8 active connections

### Test (test)
- Redis disabled
- Uses Caffeine cache (in-memory)
- No external dependencies needed

### Production (prod)
- Redis with high-availability configuration
- Key prefix: `cache:prod:`
- Pool size: 32 active connections
- SSL support enabled
- Connection pooling optimized for high traffic

## Environment Variables

### Development
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

### Production
```bash
REDIS_HOST=redis.example.com
REDIS_PORT=6379
REDIS_PASSWORD=your-secure-password
REDIS_SSL=true
```

## Monitoring Metrics

Available Micrometer metrics:

```
cache.hits{cache="shortlinks"}          - Number of cache hits
cache.misses{cache="shortlinks"}        - Number of cache misses
cache.evictions{cache="shortlinks"}     - Number of evictions
cache.get.time{cache="shortlinks"}      - Get operation timing
cache.put.time{cache="shortlinks"}      - Put operation timing
```

View metrics in Prometheus format:
```
http://localhost:8080/actuator/prometheus
```

## Troubleshooting

### Cache Not Working
1. Check Redis connection: `spring.redis.enabled: true`
2. Verify key format matches CacheKeyBuilder
3. Check TTL settings for expiration
4. Enable DEBUG logging: `logging.level.com.urlshort: DEBUG`

### High Cache Miss Rate
1. Check cache TTL settings
2. Verify key generation is consistent
3. Monitor for too-aggressive evictions
4. Review `unless` conditions

### Redis Connection Issues
Fallback to Caffeine cache will be used automatically if Redis is unavailable.

## References

- Spring Cache Documentation: https://spring.io/guides/gs/caching/
- Redis Configuration: https://spring.io/projects/spring-data-redis
- Caffeine Cache: https://github.com/ben-manes/caffeine
- Micrometer Metrics: https://micrometer.io/
