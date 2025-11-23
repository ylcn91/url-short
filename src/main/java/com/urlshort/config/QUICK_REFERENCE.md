# Cache Configuration - Quick Reference Guide

## Cache Names & Keys

```java
// Import these constants
import com.urlshort.config.CacheConfig;
import com.urlshort.config.CacheKeyBuilder;

// Cache Names
CacheConfig.CACHE_SHORT_LINKS  // shortlinks (1 hour)
CacheConfig.CACHE_WORKSPACES   // workspaces (24 hours)
CacheConfig.CACHE_USERS        // users (24 hours)
```

## Common Patterns

### Pattern 1: Simple Get with Cache
```java
@Cacheable(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkKey(#workspaceId, #code)",
    unless = "#result == null"
)
public ShortLink findByCode(Long workspaceId, String code) {
    return repository.findByWorkspaceIdAndCode(workspaceId, code).orElse(null);
}
```

### Pattern 2: Get by ID with Cache
```java
@Cacheable(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#id)",
    unless = "#result == null"
)
public ShortLink findById(Long id) {
    return repository.findById(id).orElse(null);
}
```

### Pattern 3: Update and Refresh Cache
```java
@CachePut(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#result.id)"
)
public ShortLink update(Long id, UpdateRequest request) {
    ShortLink entity = repository.findById(id).orElseThrow();
    // Update properties
    return repository.save(entity);
}
```

### Pattern 4: Delete and Evict Cache
```java
@CacheEvict(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#id)"
)
public void delete(Long id) {
    repository.deleteById(id);
}
```

### Pattern 5: Multiple Cache Operations
```java
@Caching(
    cacheable = {
        @Cacheable(
            cacheNames = CacheConfig.CACHE_SHORT_LINKS,
            key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#id)",
            unless = "#result == null"
        )
    },
    put = {
        @CachePut(
            cacheNames = CacheConfig.CACHE_WORKSPACES,
            key = "T(com.urlshort.config.CacheKeyBuilder).buildWorkspaceKey(#result.workspaceId)"
        )
    }
)
public ShortLink findByIdWithWorkspaceUpdate(Long id) {
    return repository.findById(id).orElse(null);
}
```

### Pattern 6: Workspace Caching
```java
@Cacheable(
    cacheNames = CacheConfig.CACHE_WORKSPACES,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildWorkspaceKey(#id)",
    unless = "#result == null"
)
public Workspace findWorkspaceById(Long id) {
    return workspaceRepository.findById(id).orElse(null);
}
```

### Pattern 7: User Caching
```java
@Cacheable(
    cacheNames = CacheConfig.CACHE_USERS,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildUserByEmailKey(#email)",
    unless = "#result == null"
)
public User findByEmail(String email) {
    return userRepository.findByEmail(email).orElse(null);
}
```

### Pattern 8: Batch Operations - Invalidate All
```java
@CacheEvict(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    allEntries = true
)
public void importShortLinks(List<ShortLink> links) {
    repository.saveAll(links);
}
```

## Checklist for Implementation

- [ ] Import CacheConfig and CacheKeyBuilder constants
- [ ] Use CacheKeyBuilder for all key generation
- [ ] Add `unless = "#result == null"` to @Cacheable
- [ ] Use @CachePut for update operations
- [ ] Use @CacheEvict for delete operations
- [ ] Add DEBUG level logging for cache operations
- [ ] Test cache hits and misses
- [ ] Verify metrics in /actuator/prometheus
- [ ] Document cache strategy in service class

## Logging Template

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class YourService {

    @Cacheable(
        cacheNames = CacheConfig.CACHE_SHORT_LINKS,
        key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#id)",
        unless = "#result == null"
    )
    public ShortLink findById(Long id) {
        log.debug("Cache miss for short link: {}", id);
        return repository.findById(id).orElse(null);
    }
}
```

## Testing Cache Configuration

```yaml
# application-test.yml
spring:
  redis:
    enabled: false
  cache:
    type: caffeine
```

## Environment Variables

```bash
# Development
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Production
export REDIS_HOST=redis.example.com
export REDIS_PORT=6379
export REDIS_PASSWORD=your-password
export REDIS_SSL=true
```

## Verification Commands

```bash
# Check Redis connection
curl http://localhost:8080/actuator/health

# View cache metrics
curl http://localhost:8080/actuator/metrics/cache.hits

# View all metrics
curl http://localhost:8080/actuator/prometheus | grep cache
```

## Common Mistakes to Avoid

❌ **Don't:** Hardcode cache keys
```java
@Cacheable(cacheNames = "shortlinks", key = "'shortlink:' + #workspaceId + ':' + #code")
```

✓ **Do:** Use CacheKeyBuilder
```java
@Cacheable(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkKey(#workspaceId, #code)"
)
```

---

❌ **Don't:** Cache null values
```java
@Cacheable(cacheNames = CacheConfig.CACHE_SHORT_LINKS, key = "...")
public ShortLink find(...) { ... }
```

✓ **Do:** Exclude null values
```java
@Cacheable(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "...",
    unless = "#result == null"
)
public ShortLink find(...) { ... }
```

---

❌ **Don't:** Forget to evict on delete
```java
public void delete(Long id) {
    repository.deleteById(id);  // Cache not invalidated!
}
```

✓ **Do:** Evict cache on delete
```java
@CacheEvict(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#id)"
)
public void delete(Long id) {
    repository.deleteById(id);
}
```

---

❌ **Don't:** Use @CacheEvict on update
```java
@CacheEvict(cacheNames = CacheConfig.CACHE_SHORT_LINKS, key = "...")
public ShortLink update(...) { ... }
```

✓ **Do:** Use @CachePut on update
```java
@CachePut(
    cacheNames = CacheConfig.CACHE_SHORT_LINKS,
    key = "T(com.urlshort.config.CacheKeyBuilder).buildShortLinkByIdKey(#result.id)"
)
public ShortLink update(...) { ... }
```

## Key Generation Examples

```java
// Short Link by Code
CacheKeyBuilder.buildShortLinkKey(1L, "abc123")
// Result: "shortlink:1:abc123"

// Short Link by ID
CacheKeyBuilder.buildShortLinkByIdKey(42L)
// Result: "shortlink:id:42"

// Workspace by ID
CacheKeyBuilder.buildWorkspaceKey(1L)
// Result: "workspace:1"

// Workspace by Slug
CacheKeyBuilder.buildWorkspaceBySlugKey("my-workspace")
// Result: "workspace:slug:my-workspace"

// User by ID
CacheKeyBuilder.buildUserKey(1L)
// Result: "user:1"

// User by Email
CacheKeyBuilder.buildUserByEmailKey("user@example.com")
// Result: "user:email:user@example.com"
```

## TTL Reference

| Cache | TTL | Seconds |
|-------|-----|---------|
| shortlinks | 1 hour | 3600 |
| workspaces | 24 hours | 86400 |
| users | 24 hours | 86400 |

## Performance Impact

Expected improvements with caching enabled:
- Redirect endpoint: **5-10x faster** (no DB query)
- Workspace lookups: **2-5x faster**
- User lookups: **2-5x faster**
- Reduced database load by **30-50%** (varies by usage patterns)
