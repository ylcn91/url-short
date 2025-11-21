# System Architecture

High-level architecture and design decisions for the Linkforge URL Shortener Platform.

## Table of Contents

- [Overview](#overview)
- [System Components](#system-components)
- [Data Flow](#data-flow)
- [Database Design](#database-design)
- [Caching Strategy](#caching-strategy)
- [Analytics Pipeline](#analytics-pipeline)
- [Security Model](#security-model)
- [Scalability Considerations](#scalability-considerations)

---

## Overview

Linkforge is built as a layered monolithic application with clear separation of concerns. The architecture prioritizes:

1. **Deterministic behavior** - Same input always produces same output
2. **Workspace isolation** - Complete multi-tenancy at the data layer
3. **Performance** - Sub-50ms redirects with aggressive caching
4. **Observability** - Comprehensive analytics and monitoring
5. **Maintainability** - Standard Spring Boot patterns, minimal magic

### Architecture Style

**Layered architecture** with four distinct layers:

```
┌─────────────────────────────────────────────┐
│         Presentation Layer                  │
│  (Controllers, REST endpoints, validation)  │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│          Business Logic Layer               │
│  (Services, deterministic algorithm,        │
│   URL canonicalization, collision handling) │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│         Data Access Layer                   │
│  (JPA repositories, query methods)          │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│         Infrastructure Layer                │
│  (PostgreSQL, Redis, Kafka, monitoring)     │
└─────────────────────────────────────────────┘
```

### Why Not Hexagonal Architecture?

Hexagonal (ports and adapters) adds indirection that slows development without meaningful benefit for this problem domain. We're building a CRUD application with deterministic logic, not a complex domain model requiring multiple adapters. If requirements evolve to include complex pricing rules, multi-tenant data partitioning, or customer-specific databases, we'll reconsider.

---

## System Components

### Current Implementation (v1.0)

```
┌──────────────┐
│   Browser    │
│   / Mobile   │
└──────┬───────┘
       │
       │ HTTPS
       ▼
┌────────────────────────────────────────────┐
│         Spring Boot Application            │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │  REST Controllers                    │ │
│  │  - ShortLinkController               │ │
│  │  - AnalyticsController               │ │
│  │  - WorkspaceController               │ │
│  └────────────┬─────────────────────────┘ │
│               │                            │
│  ┌────────────▼─────────────────────────┐ │
│  │  Service Layer                       │ │
│  │  - ShortLinkService (deterministic)  │ │
│  │  - AnalyticsService                  │ │
│  │  - WorkspaceService                  │ │
│  └────────────┬─────────────────────────┘ │
│               │                            │
│  ┌────────────▼─────────────────────────┐ │
│  │  Utility Layer                       │ │
│  │  - UrlCanonicalizer                  │ │
│  │  - ShortCodeGenerator (SHA-256+Base58)│ │
│  │  - Base58Encoder                     │ │
│  └────────────┬─────────────────────────┘ │
│               │                            │
│  ┌────────────▼─────────────────────────┐ │
│  │  Repository Layer (Spring Data JPA)  │ │
│  │  - ShortLinkRepository               │ │
│  │  - ClickEventRepository              │ │
│  │  - WorkspaceRepository               │ │
│  └────────────┬─────────────────────────┘ │
│               │                            │
└───────────────┼────────────────────────────┘
                │
                │ JDBC
                ▼
      ┌──────────────────┐
      │   PostgreSQL     │
      │   - Flyway       │
      │   - Indexes      │
      │   - Constraints  │
      └──────────────────┘
```

### Planned Components (v2.0)

```
                    ┌─────────────────┐
                    │  Load Balancer  │
                    │   (nginx/ALB)   │
                    └────────┬────────┘
                             │
             ┌───────────────┴────────────────┐
             │                                │
             ▼                                ▼
    ┌────────────────┐              ┌────────────────┐
    │  Next.js       │              │  Spring Boot   │
    │  Frontend      │              │  API           │
    │  (Dashboard)   │◄─────────────┤  (Backend)     │
    └────────────────┘     REST     └────────┬───────┘
                                              │
                     ┌────────────────────────┼─────────────────────┐
                     │                        │                     │
                     ▼                        ▼                     ▼
            ┌─────────────────┐     ┌─────────────────┐   ┌──────────────┐
            │   PostgreSQL    │     │   Redis         │   │   Kafka      │
            │   (Primary)     │     │   (Cache)       │   │   (Events)   │
            └─────────────────┘     └─────────────────┘   └──────┬───────┘
                     │                                            │
                     │                                            │
                     ▼                                            ▼
            ┌─────────────────┐                          ┌──────────────┐
            │   PostgreSQL    │                          │  Analytics   │
            │   (Read Replica)│                          │  Consumer    │
            └─────────────────┘                          └──────────────┘
```

---

## Data Flow

### URL Creation Flow

```
1. Client Request
   │
   ├─► POST /api/v1/workspaces/1/links
   │   Body: {"original_url": "https://example.com/page"}
   │
   ▼
2. Controller Layer (ShortLinkController)
   │
   ├─► Validate request (JSR-303 @Valid)
   ├─► Extract workspace ID from path
   ├─► Authenticate user/API key
   │
   ▼
3. Service Layer (ShortLinkServiceImpl)
   │
   ├─► Step 1: Canonicalize URL
   │   │  UrlCanonicalizer.canonicalize(url)
   │   │  ├─► Parse URL components
   │   │  ├─► Lowercase scheme & host
   │   │  ├─► Remove default ports
   │   │  ├─► Normalize path
   │   │  ├─► Sort query parameters
   │   │  └─► Remove fragments
   │   │
   │   └─► Result: "https://example.com/page"
   │
   ├─► Step 2: Check if URL exists
   │   │  Query: SELECT * FROM short_link
   │   │         WHERE workspace_id = 1
   │   │         AND normalized_url = 'https://example.com/page'
   │   │         AND is_deleted = false
   │   │
   │   └─► If found: RETURN existing short code (deterministic reuse)
   │
   ├─► Step 3: Generate short code (if not exists)
   │   │  ShortCodeGenerator.generateShortCode(normalizedUrl, workspaceId, retrySalt=0)
   │   │  ├─► Construct hash input: normalizedUrl + "|" + workspaceId + "|" + retrySalt
   │   │  ├─► Compute SHA-256 hash (32 bytes)
   │   │  ├─► Extract first 16 bytes
   │   │  ├─► Encode to Base58 (10 characters)
   │   │  └─► Result: "MaSgB7xKpQ"
   │   │
   │   └─► Short code generated
   │
   ├─► Step 4: Check collision
   │   │  Query: SELECT * FROM short_link
   │   │         WHERE workspace_id = 1
   │   │         AND short_code = 'MaSgB7xKpQ'
   │   │         AND is_deleted = false
   │   │
   │   ├─► If not found: PROCEED to save
   │   └─► If found with different normalized_url: RETRY with retrySalt++
   │
   ├─► Step 5: Save to database
   │   │  INSERT INTO short_link (
   │   │    workspace_id, short_code, original_url, normalized_url,
   │   │    created_by, created_at, click_count, is_active
   │   │  ) VALUES (1, 'MaSgB7xKpQ', ..., 0, true)
   │   │
   │   └─► Transaction committed
   │
   └─► Step 6: Return response
       └─► {shortCode: "MaSgB7xKpQ", shortUrl: "https://short.ly/MaSgB7xKpQ", ...}
```

**Time Complexity:**
- URL canonicalization: O(n) where n = URL length
- Database lookup: O(log m) where m = records (B-tree index)
- Hash generation: O(n) constant time for SHA-256
- **Total**: ~50-150ms (including DB roundtrip)

### URL Redirect Flow

```
1. Client Request
   │
   ├─► GET /MaSgB7xKpQ
   │
   ▼
2. Controller Layer (RedirectController)
   │
   ├─► Extract short code from path
   ├─► No authentication required (public endpoint)
   │
   ▼
3. Service Layer (ShortLinkService)
   │
   ├─► Check cache (Redis) [FUTURE]
   │   │  Key: "link:1:MaSgB7xKpQ"
   │   │  TTL: 1 hour
   │   │
   │   ├─► Cache HIT: Return cached URL (< 5ms)
   │   └─► Cache MISS: Query database
   │
   ├─► Query database
   │   │  SELECT original_url, is_active, expires_at, click_count, metadata
   │   │  FROM short_link
   │   │  WHERE workspace_id = 1
   │   │  AND short_code = 'MaSgB7xKpQ'
   │   │  AND is_deleted = false
   │   │
   │   └─► Indexed lookup: ~10-30ms
   │
   ├─► Validate link status
   │   ├─► Check: is_active = true
   │   ├─► Check: expires_at > NOW() OR null
   │   ├─► Check: click_count < max_clicks OR null
   │   │
   │   └─► If invalid: RETURN 410 Gone or 404 Not Found
   │
   ├─► Log click event (async)
   │   │  Publish to Kafka topic: "click-events" [FUTURE]
   │   │  OR
   │   │  INSERT INTO click_event (
   │   │    short_link_id, clicked_at, ip_address, user_agent,
   │   │    referer, country, device_type
   │   │  ) VALUES (...)
   │   │
   │   └─► Fire-and-forget (doesn't block response)
   │
   └─► Return 302 redirect
       └─► Location: https://example.com/page
```

**Time Complexity:**
- Cache lookup (planned): ~2-5ms
- Database lookup (uncached): ~10-30ms
- Click logging (async): non-blocking
- **Total**: <30ms (p50), <65ms (p95)

### Analytics Query Flow

```
1. Client Request
   │
   ├─► GET /api/v1/workspaces/1/links/MaSgB7xKpQ/stats?from=2025-11-01&to=2025-11-18
   │
   ▼
2. Service Layer (AnalyticsService)
   │
   ├─► Validate date range
   │
   ├─► Query: Total clicks
   │   │  SELECT COUNT(*) FROM click_event
   │   │  WHERE short_link_id = (SELECT id FROM short_link WHERE short_code = 'MaSgB7xKpQ')
   │   │  AND clicked_at BETWEEN '2025-11-01' AND '2025-11-18'
   │   │
   │   └─► Result: 1523
   │
   ├─► Query: Clicks by date
   │   │  SELECT DATE(clicked_at) as date, COUNT(*) as count
   │   │  FROM click_event
   │   │  WHERE short_link_id = ...
   │   │  GROUP BY DATE(clicked_at)
   │   │  ORDER BY date
   │   │
   │   └─► Result: {"2025-11-15": 234, "2025-11-16": 312, ...}
   │
   ├─► Query: Clicks by country
   │   │  SELECT country, COUNT(*) as count
   │   │  FROM click_event
   │   │  WHERE short_link_id = ...
   │   │  GROUP BY country
   │   │  ORDER BY count DESC
   │   │  LIMIT 10
   │   │
   │   └─► Result: {"US": 678, "UK": 234, ...}
   │
   └─► Aggregate and return
       └─► {totalClicks: 1523, clicksByDate: {...}, clicksByCountry: {...}}
```

**Optimization Strategy:**
- Use TimescaleDB hypertables for time-series data
- Pre-aggregate hourly/daily stats via background job
- Cache aggregated results in Redis

---

## Database Design

### Schema Overview

See [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) for complete DDL and design decisions.

**Core Tables:**
- `workspace` - Multi-tenant workspaces (soft delete)
- `users` - User accounts per workspace (soft delete)
- `short_link` - Short URLs with deterministic codes (soft delete)
- `click_event` - Click tracking for analytics (hard delete after retention)
- `api_key` - API keys for programmatic access (hard delete after expiration)

**Key Constraints:**
```sql
-- Deterministic behavior: same URL in same workspace = same code
CREATE UNIQUE INDEX idx_short_link_workspace_normalized_url
ON short_link(workspace_id, normalized_url);

-- Workspace isolation: short code unique within workspace
CREATE UNIQUE INDEX idx_short_link_workspace_code
ON short_link(workspace_id, short_code);
```

### Indexes for Performance

| Table | Index | Purpose | Query Pattern |
|-------|-------|---------|---------------|
| `short_link` | `(workspace_id, short_code)` | **Redirect lookup** | `WHERE workspace_id = ? AND short_code = ?` |
| `short_link` | `(workspace_id, normalized_url)` | **Deterministic reuse** | `WHERE workspace_id = ? AND normalized_url = ?` |
| `click_event` | `(short_link_id, clicked_at)` | **Analytics queries** | `WHERE short_link_id = ? ORDER BY clicked_at` |
| `click_event` | `(clicked_at)` | **Time-series analysis** | `WHERE clicked_at BETWEEN ? AND ?` |

---

## Caching Strategy

### Current (v1.0): Spring Cache with JVM Heap

```java
@Cacheable(value = "shortLinks", key = "#workspaceId + ':' + #shortCode")
public ShortLinkResponse getShortLink(Long workspaceId, String shortCode) {
    // Cache hit: Return immediately (~1ms)
    // Cache miss: Query DB and populate cache
}
```

**Configuration:**
- Cache size: 10,000 entries
- Eviction: LRU (Least Recently Used)
- TTL: 1 hour

**Limitations:**
- Cache not shared across application instances
- Requires cache invalidation on link updates/deletes

### Planned (v2.0): Redis for Distributed Caching

```
┌────────────────┐     ┌────────────────┐
│  App Instance  │     │  App Instance  │
│       1        │     │       2        │
└────────┬───────┘     └────────┬───────┘
         │                      │
         └───────────┬──────────┘
                     │
                     ▼
            ┌─────────────────┐
            │     Redis       │
            │   (Shared Cache)│
            └─────────────────┘
```

**Cache Key Pattern:** `link:{workspaceId}:{shortCode}`

**Cache Operations:**
```redis
# Write-through on creation
SET link:1:MaSgB7xKpQ '{"originalUrl":"https://example.com/page",...}' EX 3600

# Read on redirect
GET link:1:MaSgB7xKpQ

# Invalidate on update/delete
DEL link:1:MaSgB7xKpQ
```

**Expected Performance:**
- Cache hit: <5ms
- Cache miss + DB query: ~30ms
- Cache hit rate: >95% (based on Zipf distribution of link popularity)

---

## Analytics Pipeline

### Current (v1.0): Direct Database Writes

```
User clicks link
    │
    ├─► 302 Redirect (immediate)
    │
    └─► INSERT INTO click_event (...) [Async]
```

**Pros:**
- Simple implementation
- No additional infrastructure

**Cons:**
- Click writes compete with redirect lookups
- Analytics queries impact transactional performance
- Limited scalability

### Planned (v2.0): Kafka Event Streaming

```
User clicks link
    │
    ├─► 302 Redirect (immediate)
    │
    └─► Publish to Kafka: "click-events" topic
            │
            └─► Consumer 1: Analytics Aggregator
            │       ├─► Aggregate hourly stats
            │       └─► Write to PostgreSQL (time-series table)
            │
            └─► Consumer 2: Data Warehouse Exporter
                    └─► Export to S3 / BigQuery / Snowflake
```

**Kafka Configuration:**
- **Topic:** `click-events`
- **Partitions:** 12 (enables 12 parallel consumers)
- **Replication Factor:** 2
- **Retention:** 30 days
- **Compression:** Snappy

**Event Schema:**
```json
{
  "eventId": "uuid",
  "timestamp": "2025-11-18T14:30:45.123Z",
  "shortLinkId": 12345,
  "originalUrl": "https://example.com/page",
  "clientIp": "192.168.1.1",
  "userAgent": "Mozilla/5.0...",
  "country": "US",
  "city": "New York",
  "referrer": "https://twitter.com",
  "deviceType": "mobile"
}
```

See [KAFKA_DECISION.md](KAFKA_DECISION.md) for detailed Kafka architecture.

---

## Security Model

### Authentication

**Two-Factor Authentication:**
1. **JWT Tokens** - For user sessions (web, mobile)
2. **API Keys** - For programmatic access (integrations, CI/CD)

### Authorization

**Role-Based Access Control (RBAC):**

| Role | Permissions |
|------|-------------|
| **Admin** | Full workspace control, manage users, delete links |
| **Member** | Create/view links, view analytics |
| **Viewer** | View links and analytics only (read-only) |

### Data Protection

1. **Workspace Isolation:** All queries filtered by `workspace_id`
2. **Row-Level Security:** PostgreSQL RLS policies (optional, not enabled in v1)
3. **API Key Hashing:** SHA-256 hash stored, never plain key
4. **IP Anonymization:** Last octet removed for GDPR compliance
5. **Password Hashing:** Bcrypt with cost factor 12

### Rate Limiting

**Per-workspace limits enforced at API Gateway:**
- Free tier: 100 requests/min
- Pro tier: 1000 requests/min
- Enterprise: Custom

**Implementation:**
```java
@RateLimiter(name = "workspaceApiLimit", fallbackMethod = "rateLimitFallback")
public ShortLinkResponse createShortLink(...) {
    // Business logic
}
```

---

## Scalability Considerations

### Current Capacity (Single Instance)

- **Links:** 100M+ per workspace (tested with 10M)
- **Redirects:** ~1000 req/sec (single instance, PostgreSQL bottleneck)
- **Link Creation:** ~100 req/sec (write-limited)
- **Database Size:** ~250-300 bytes per URL record

### Scaling Strategy

**Horizontal Scaling:**

```
┌───────────────────────────────────────┐
│         Load Balancer (ALB)           │
└─────────┬──────────────┬──────────────┘
          │              │
┌─────────▼────────┐ ┌──▼──────────────┐
│  App Instance 1  │ │  App Instance 2  │
│  (Stateless)     │ │  (Stateless)     │
└─────────┬────────┘ └──┬───────────────┘
          │              │
          └──────┬───────┘
                 │
┌────────────────▼────────────────┐
│  PostgreSQL (Primary/Replica)   │
│  + Read Replicas for Analytics  │
└─────────────────────────────────┘
```

**Vertical Scaling:**
- Start: t3.medium (2 vCPU, 4GB RAM)
- Grow to: t3.xlarge (4 vCPU, 16GB RAM)
- Peak: c5.4xlarge (16 vCPU, 32GB RAM)

**Database Scaling:**
1. **Read Replicas:** For analytics queries
2. **Connection Pooling:** HikariCP with 20-50 connections
3. **Partitioning:** `click_event` table by date range
4. **Archival:** Move old click data to S3 / data warehouse

**Caching Layer:**
- Redis cluster (3 nodes, 16GB each)
- Cache 95% of redirects
- Reduce DB load by 90%

### Performance Targets

| Metric | Current (v1.0) | Target (v2.0 with caching) |
|--------|----------------|---------------------------|
| Redirect latency (p50) | 30ms | 5ms |
| Redirect latency (p95) | 65ms | 15ms |
| Link creation (p50) | 100ms | 50ms |
| Analytics query (p95) | 500ms | 200ms |
| Throughput (redirects) | 1000 req/sec | 10,000 req/sec |

---

## Observability

### Metrics

**Application Metrics (Micrometer/Prometheus):**
```
# Business metrics
linkforge_links_created_total
linkforge_redirects_total
linkforge_cache_hits_total
linkforge_cache_misses_total

# Performance metrics
linkforge_redirect_duration_seconds{quantile="0.5"}
linkforge_redirect_duration_seconds{quantile="0.95"}
linkforge_redirect_duration_seconds{quantile="0.99"}
```

### Logging

**Structured Logging (JSON):**
```json
{
  "timestamp": "2025-11-18T14:30:45.123Z",
  "level": "INFO",
  "logger": "com.urlshort.service.ShortLinkServiceImpl",
  "message": "Created new short link",
  "context": {
    "workspaceId": 1,
    "shortCode": "MaSgB7xKpQ",
    "normalizedUrl": "https://example.com/page",
    "userId": 123
  }
}
```

### Tracing

**Distributed Tracing (Zipkin/Jaeger):**
- Trace ID propagated across service calls
- Span ID per operation
- Critical paths instrumented

### Health Checks

```bash
# Application health
GET /actuator/health

# Detailed health (requires auth)
GET /actuator/health-detailed
```

**Health indicators:**
- `db` - PostgreSQL connection
- `diskSpace` - Available disk space
- `redis` - Redis connection (v2.0)
- `kafka` - Kafka connectivity (v2.0)

---

## Technology Decisions

### Why PostgreSQL?

- **ACID guarantees** - Critical for unique constraint enforcement
- **JSONB support** - Flexible metadata without schema changes
- **Mature ecosystem** - Extensions (TimescaleDB for analytics)
- **Horizontal scaling** - Read replicas, Citus for sharding (if needed)

### Why Spring Boot?

- **Rapid development** - Batteries included
- **Production-ready** - Actuator, monitoring, health checks
- **Strong ecosystem** - Spring Data JPA, Spring Security, Spring Cache
- **Team familiarity** - Large talent pool

### Why Base58 Encoding?

- **URL-safe** - No special encoding needed
- **Human-readable** - Avoids ambiguous characters (0/O, I/l/1)
- **Compact** - 10 characters = 58^10 ≈ 4.3 × 10^17 combinations
- **Industry standard** - Used by Bitcoin, Flickr, YouTube

---

## Future Enhancements

### Planned for v2.0

- [ ] Redis caching layer
- [ ] Kafka event streaming
- [ ] Read replicas for analytics
- [ ] Next.js dashboard
- [ ] Webhook support

### Under Consideration

- [ ] Custom domains (CNAME configuration)
- [ ] QR code generation
- [ ] Link health monitoring (periodic checks)
- [ ] A/B testing (split traffic)
- [ ] Geofencing (location-based redirects)

---

**Last Updated:** 2025-11-18
**Document Version:** 1.0
