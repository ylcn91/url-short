# Database Schema Design - URL Shortener Platform

## Overview

This document describes the PostgreSQL database schema for a multi-tenant URL shortener platform. The schema supports:
- Multi-tenant architecture (workspace-based isolation)
- Deterministic URL shortening (same URL + workspace = same short code)
- Analytics and click tracking
- API-based programmatic access
- User management per workspace

## Conceptual Data Model

### Entity Relationships

```
workspace (1) ──── (N) users
    │
    ├──── (N) short_link
    │          │
    │          └──── (N) click_event
    │
    └──── (N) api_key

users (1) ──── (N) short_link [created_by]
users (1) ──── (N) api_key [created_by]
```

### Key Relationships

1. **workspace → users**: One-to-Many
   - A workspace can have multiple users
   - Each user belongs to exactly one workspace

2. **workspace → short_link**: One-to-Many
   - A workspace can create multiple short links
   - Each short link belongs to exactly one workspace
   - Enforces tenant isolation

3. **workspace → api_key**: One-to-Many
   - A workspace can have multiple API keys
   - Each API key is scoped to one workspace

4. **users → short_link**: One-to-Many (created_by)
   - A user can create multiple short links
   - Each short link tracks its creator

5. **short_link → click_event**: One-to-Many
   - A short link can have many click events
   - Each click event belongs to one short link

6. **users → api_key**: One-to-Many (created_by)
   - A user can create multiple API keys
   - Each API key tracks its creator

## Design Decisions

### 1. Soft Delete vs Hard Delete Strategy

#### **Soft Delete** (is_deleted flag) for:
- **workspace**: Preserve tenant data for compliance, audit, and potential reactivation
- **users**: Maintain referential integrity with created content, comply with audit requirements
- **short_link**: Preserve analytics history, prevent broken links from being reused with different targets

**Rationale**: These entities have significant referential integrity requirements and historical value. Soft deletion prevents cascade deletion issues and maintains data lineage for compliance and auditing.

#### **Hard Delete** (physical removal) for:
- **click_event**: High-volume time-series data that can grow unboundedly
- **api_key**: After expiration, no historical value; security-sensitive data should be removed

**Rationale**: Click events can generate millions of records and should be managed through partitioning, archival, and eventual deletion. API keys should be permanently removed after expiration for security hygiene.

**Implementation Note**: For click_event, use PostgreSQL table partitioning by date range and archive/drop old partitions as part of data retention policy.

### 2. Deterministic URL Shortening

The schema supports deterministic shortening through:
- **normalized_url** column: Stores a normalized form of the original URL
- **UNIQUE constraint** on `(workspace_id, normalized_url)`: Ensures same URL in same workspace returns same short code
- **Normalization rules** (application layer):
  - Convert to lowercase
  - Remove trailing slashes
  - Sort query parameters alphabetically
  - Remove tracking parameters (utm_*, fbclid, etc.)
  - Normalize protocol (http/https)

### 3. Indexing Strategy

#### **Primary Indexes** (for core functionality):
- `short_link(workspace_id, short_code)`: Main lookup path for URL redirection
- `short_link(workspace_id, normalized_url)`: Deterministic lookup
- `click_event(short_link_id, clicked_at)`: Analytics queries

#### **Performance Indexes**:
- `short_link(workspace_id, created_at)`: List links by workspace with time-based sorting
- `short_link(created_by)`: User's link history
- `api_key(workspace_id, key_hash)`: API authentication
- `users(workspace_id, email)`: User lookup and authentication

#### **Analytics Indexes**:
- `click_event(clicked_at)`: Time-series analysis
- `click_event(country)`, `click_event(device_type)`: Demographic analysis

### 4. Data Types

- **IDs**: `BIGSERIAL` - Supports high scale (9 quintillion values)
- **Timestamps**: `TIMESTAMPTZ` - Timezone-aware for global users
- **short_code**: `VARCHAR(20)` - Flexible length for various encoding schemes
- **URLs**: `TEXT` - No artificial length limits on URLs
- **JSON**: `JSONB` - For flexible structured data (API scopes, metadata)

### 5. Multi-Tenant Isolation

All tenant-scoped tables include `workspace_id` as part of their primary access patterns:
- Enforced through foreign key constraints
- Application-level row-level security (RLS) policies can be added
- Query patterns always filter by workspace_id first

## Entity Definitions

### 1. workspace

**Purpose**: Represents a tenant/organization in the multi-tenant system.

**Columns**:
- `id`: Primary key, auto-incrementing
- `name`: Display name of the workspace
- `slug`: URL-safe identifier (for subdomains, URLs)
- `created_at`: Creation timestamp
- `updated_at`: Last modification timestamp
- `is_deleted`: Soft delete flag
- `settings`: JSONB for workspace-level configuration

**Constraints**:
- `slug` must be unique across all workspaces (including deleted)
- `slug` must be lowercase alphanumeric with hyphens

**Indexes**:
- Primary key on `id`
- Unique index on `slug`
- Partial index on `is_deleted = false` for active workspace queries

---

### 2. users

**Purpose**: User accounts within workspaces.

**Columns**:
- `id`: Primary key
- `workspace_id`: Foreign key to workspace
- `email`: User's email address
- `password_hash`: Bcrypt/Argon2 password hash
- `full_name`: User's display name
- `role`: Enum (admin, member, viewer)
- `created_at`: Account creation timestamp
- `updated_at`: Last modification timestamp
- `last_login_at`: Last successful login
- `is_deleted`: Soft delete flag

**Constraints**:
- `(workspace_id, email)` must be unique (users can have same email in different workspaces)
- `email` must be valid email format

**Indexes**:
- Primary key on `id`
- Unique index on `(workspace_id, email)`
- Index on `workspace_id` for listing workspace members

---

### 3. short_link

**Purpose**: Core entity for shortened URLs with deterministic reuse support.

**Columns**:
- `id`: Primary key
- `workspace_id`: Foreign key to workspace
- `short_code`: The short identifier (e.g., "abc123")
- `original_url`: The full original URL
- `normalized_url`: Normalized form for deterministic matching
- `title`: Optional display title
- `description`: Optional description
- `created_by`: Foreign key to users
- `created_at`: Creation timestamp
- `updated_at`: Last modification timestamp
- `expires_at`: Optional expiration timestamp
- `is_active`: Whether link is currently active
- `click_count`: Denormalized click counter for performance
- `is_deleted`: Soft delete flag
- `metadata`: JSONB for custom tags, categories, etc.

**Constraints**:
- **UNIQUE** `(workspace_id, short_code)`: Each short code is unique within a workspace
- **UNIQUE** `(workspace_id, normalized_url)`: Deterministic reuse - same URL gets same code
- `short_code` must be alphanumeric (base62 or similar)
- `click_count` must be non-negative

**Indexes**:
- Primary key on `id`
- **Unique index** on `(workspace_id, short_code)` - **Main lookup path for redirects**
- **Unique index** on `(workspace_id, normalized_url)` - **Deterministic lookup**
- Index on `(workspace_id, created_at DESC)` - List links chronologically
- Index on `created_by` - User's link history
- Partial index on `(workspace_id, expires_at)` WHERE `expires_at IS NOT NULL` - Expiration cleanup

**Design Notes**:
- The unique constraint on `(workspace_id, normalized_url)` enables deterministic behavior
- When a user requests to shorten a URL, the system first normalizes it and checks if it exists
- If it exists, return the existing short_code; otherwise, generate a new one
- `click_count` is denormalized from click_event for performance (updated via trigger or application logic)

---

### 4. click_event

**Purpose**: Analytics and tracking data for each click on a short link.

**Columns**:
- `id`: Primary key
- `short_link_id`: Foreign key to short_link
- `clicked_at`: Timestamp of the click
- `ip_address`: Client IP address (anonymized if needed for GDPR)
- `user_agent`: Browser user agent string
- `referer`: HTTP referer header
- `country`: ISO country code (derived from IP)
- `city`: City name (derived from IP)
- `device_type`: Enum (desktop, mobile, tablet, bot)
- `browser`: Browser name and version
- `os`: Operating system

**Constraints**:
- `clicked_at` must not be in the future

**Indexes**:
- Primary key on `id`
- Index on `(short_link_id, clicked_at DESC)` - Link-specific analytics
- Index on `clicked_at` - Time-series queries
- Index on `(short_link_id, country)` - Geographic analytics
- Index on `(short_link_id, device_type)` - Device analytics

**Partitioning Strategy** (recommended):
```sql
-- Partition by month for efficient archival and querying
CREATE TABLE click_event (
    ...
) PARTITION BY RANGE (clicked_at);

CREATE TABLE click_event_2024_11 PARTITION OF click_event
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

-- Create new partitions monthly and drop old ones based on retention policy
```

**Data Retention**:
- Recommend 13-month retention policy (12 months + current month)
- Archive older data to cold storage (S3, data warehouse) before dropping
- Consider pre-aggregating statistics before archival

---

### 5. api_key

**Purpose**: API keys for programmatic access to the platform.

**Columns**:
- `id`: Primary key
- `workspace_id`: Foreign key to workspace
- `key_hash`: SHA-256 hash of the API key
- `key_prefix`: First 8 characters for identification (e.g., "sk_live_abcd1234")
- `name`: Friendly name/description
- `created_by`: Foreign key to users
- `created_at`: Creation timestamp
- `last_used_at`: Last successful API call timestamp
- `expires_at`: Optional expiration timestamp
- `is_active`: Whether key is currently active
- `scopes`: JSONB array of permission scopes (e.g., ["links:read", "links:write"])

**Constraints**:
- `key_hash` must be unique globally
- `key_prefix` must be unique within workspace
- `scopes` must be a valid JSON array

**Indexes**:
- Primary key on `id`
- Unique index on `key_hash` - API authentication lookup
- Index on `(workspace_id, is_active)` - List active keys
- Index on `expires_at` WHERE `expires_at IS NOT NULL` - Cleanup expired keys

**Security Notes**:
- Store only the hash, never the plain API key
- Show the full key only once upon creation
- Use constant-time comparison for key verification
- Rotate keys periodically
- Implement rate limiting per API key

## Complete DDL Schema

```sql
-- ============================================================================
-- EXTENSION: Enable UUID generation (if needed in future)
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE user_role AS ENUM ('admin', 'member', 'viewer');
CREATE TYPE device_type AS ENUM ('desktop', 'mobile', 'tablet', 'bot', 'unknown');

-- ============================================================================
-- TABLE: workspace
-- ============================================================================

CREATE TABLE workspace (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    settings JSONB DEFAULT '{}'::jsonb,

    CONSTRAINT workspace_slug_format CHECK (slug ~ '^[a-z0-9][a-z0-9-]*[a-z0-9]$')
);

-- Indexes for workspace
CREATE UNIQUE INDEX idx_workspace_slug ON workspace(slug);
CREATE INDEX idx_workspace_is_deleted ON workspace(is_deleted) WHERE is_deleted = FALSE;

COMMENT ON TABLE workspace IS 'Multi-tenant workspace/organization table';
COMMENT ON COLUMN workspace.slug IS 'URL-safe identifier for subdomains and routing';
COMMENT ON COLUMN workspace.is_deleted IS 'Soft delete flag to preserve tenant data';
COMMENT ON COLUMN workspace.settings IS 'Workspace-level configuration (JSONB for flexibility)';

-- ============================================================================
-- TABLE: users
-- ============================================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'member',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Indexes for users
CREATE UNIQUE INDEX idx_users_workspace_email ON users(workspace_id, email);
CREATE INDEX idx_users_workspace_id ON users(workspace_id);
CREATE INDEX idx_users_is_deleted ON users(is_deleted) WHERE is_deleted = FALSE;

COMMENT ON TABLE users IS 'User accounts scoped to workspaces';
COMMENT ON COLUMN users.role IS 'User role: admin (full access), member (standard), viewer (read-only)';
COMMENT ON COLUMN users.is_deleted IS 'Soft delete to maintain referential integrity with created content';
COMMENT ON CONSTRAINT users_email_format ON users IS 'Basic email format validation';

-- ============================================================================
-- TABLE: short_link
-- ============================================================================

CREATE TABLE short_link (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    short_code VARCHAR(20) NOT NULL,
    original_url TEXT NOT NULL,
    normalized_url TEXT NOT NULL,
    title VARCHAR(500),
    description TEXT,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    click_count BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    metadata JSONB DEFAULT '{}'::jsonb,

    CONSTRAINT short_link_short_code_format CHECK (short_code ~ '^[a-zA-Z0-9_-]+$'),
    CONSTRAINT short_link_click_count_non_negative CHECK (click_count >= 0),
    CONSTRAINT short_link_original_url_not_empty CHECK (length(trim(original_url)) > 0),
    CONSTRAINT short_link_normalized_url_not_empty CHECK (length(trim(normalized_url)) > 0)
);

-- Indexes for short_link
-- CRITICAL: Main lookup path for URL redirection
CREATE UNIQUE INDEX idx_short_link_workspace_code ON short_link(workspace_id, short_code);

-- CRITICAL: Deterministic reuse - same URL + workspace = same short code
CREATE UNIQUE INDEX idx_short_link_workspace_normalized_url ON short_link(workspace_id, normalized_url);

-- Performance indexes
CREATE INDEX idx_short_link_workspace_created_at ON short_link(workspace_id, created_at DESC);
CREATE INDEX idx_short_link_created_by ON short_link(created_by);
CREATE INDEX idx_short_link_expires_at ON short_link(expires_at) WHERE expires_at IS NOT NULL;

-- Partial index for active, non-deleted links (most common queries)
CREATE INDEX idx_short_link_active ON short_link(workspace_id, created_at DESC)
    WHERE is_deleted = FALSE AND is_active = TRUE;

COMMENT ON TABLE short_link IS 'Core entity for shortened URLs with deterministic reuse';
COMMENT ON COLUMN short_link.short_code IS 'Short identifier (e.g., "abc123") - unique per workspace';
COMMENT ON COLUMN short_link.normalized_url IS 'Normalized URL for deterministic matching - ensures same URL gets same code';
COMMENT ON COLUMN short_link.click_count IS 'Denormalized counter from click_event for performance';
COMMENT ON COLUMN short_link.is_deleted IS 'Soft delete to preserve analytics history';
COMMENT ON CONSTRAINT short_link_short_code_format ON short_link IS 'Alphanumeric with underscore and hyphen allowed';

-- ============================================================================
-- TABLE: click_event
-- ============================================================================

CREATE TABLE click_event (
    id BIGSERIAL PRIMARY KEY,
    short_link_id BIGINT NOT NULL REFERENCES short_link(id) ON DELETE CASCADE,
    clicked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    referer TEXT,
    country VARCHAR(2),
    city VARCHAR(255),
    device_type device_type NOT NULL DEFAULT 'unknown',
    browser VARCHAR(100),
    os VARCHAR(100),

    CONSTRAINT click_event_clicked_at_not_future CHECK (clicked_at <= CURRENT_TIMESTAMP)
);

-- Indexes for click_event
CREATE INDEX idx_click_event_short_link_clicked_at ON click_event(short_link_id, clicked_at DESC);
CREATE INDEX idx_click_event_clicked_at ON click_event(clicked_at DESC);
CREATE INDEX idx_click_event_country ON click_event(short_link_id, country);
CREATE INDEX idx_click_event_device_type ON click_event(short_link_id, device_type);

COMMENT ON TABLE click_event IS 'Analytics and tracking data for each click - consider partitioning by date';
COMMENT ON COLUMN click_event.ip_address IS 'Client IP - consider anonymization for GDPR compliance';
COMMENT ON COLUMN click_event.country IS 'ISO 3166-1 alpha-2 country code derived from IP geolocation';

-- ============================================================================
-- TABLE: api_key
-- ============================================================================

CREATE TABLE api_key (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    key_hash VARCHAR(64) NOT NULL,
    key_prefix VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    scopes JSONB NOT NULL DEFAULT '["links:read", "links:write"]'::jsonb,

    CONSTRAINT api_key_scopes_is_array CHECK (jsonb_typeof(scopes) = 'array')
);

-- Indexes for api_key
CREATE UNIQUE INDEX idx_api_key_hash ON api_key(key_hash);
CREATE UNIQUE INDEX idx_api_key_workspace_prefix ON api_key(workspace_id, key_prefix);
CREATE INDEX idx_api_key_workspace_active ON api_key(workspace_id, is_active);
CREATE INDEX idx_api_key_expires_at ON api_key(expires_at) WHERE expires_at IS NOT NULL;

COMMENT ON TABLE api_key IS 'API keys for programmatic access - store only hashed values';
COMMENT ON COLUMN api_key.key_hash IS 'SHA-256 hash of the API key - never store plain key';
COMMENT ON COLUMN api_key.key_prefix IS 'First 8-12 chars for identification (e.g., "sk_live_abcd1234")';
COMMENT ON COLUMN api_key.scopes IS 'JSONB array of permission scopes (e.g., ["links:read", "links:write", "analytics:read"])';

-- ============================================================================
-- TRIGGERS: Automatic updated_at timestamp
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_workspace_updated_at BEFORE UPDATE ON workspace
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_short_link_updated_at BEFORE UPDATE ON short_link
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON FUNCTION update_updated_at_column() IS 'Trigger function to automatically update updated_at timestamp';

-- ============================================================================
-- TRIGGERS: Increment click_count on click_event insert
-- ============================================================================

CREATE OR REPLACE FUNCTION increment_click_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE short_link
    SET click_count = click_count + 1
    WHERE id = NEW.short_link_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER increment_short_link_click_count AFTER INSERT ON click_event
    FOR EACH ROW EXECUTE FUNCTION increment_click_count();

COMMENT ON FUNCTION increment_click_count() IS 'Trigger function to automatically increment click_count in short_link';

-- ============================================================================
-- VIEWS: Useful views for common queries
-- ============================================================================

-- Active short links with creator information
CREATE VIEW v_active_short_links AS
SELECT
    sl.id,
    sl.workspace_id,
    w.name AS workspace_name,
    w.slug AS workspace_slug,
    sl.short_code,
    sl.original_url,
    sl.title,
    sl.description,
    sl.created_at,
    sl.expires_at,
    sl.click_count,
    u.full_name AS created_by_name,
    u.email AS created_by_email
FROM short_link sl
JOIN workspace w ON sl.workspace_id = w.id
JOIN users u ON sl.created_by = u.id
WHERE sl.is_deleted = FALSE
  AND sl.is_active = TRUE
  AND w.is_deleted = FALSE;

COMMENT ON VIEW v_active_short_links IS 'Active short links with workspace and creator details';

-- Daily click statistics
CREATE VIEW v_daily_click_stats AS
SELECT
    short_link_id,
    DATE(clicked_at) AS click_date,
    COUNT(*) AS total_clicks,
    COUNT(DISTINCT ip_address) AS unique_ips,
    COUNT(*) FILTER (WHERE device_type = 'desktop') AS desktop_clicks,
    COUNT(*) FILTER (WHERE device_type = 'mobile') AS mobile_clicks,
    COUNT(*) FILTER (WHERE device_type = 'tablet') AS tablet_clicks,
    COUNT(DISTINCT country) AS countries_count
FROM click_event
GROUP BY short_link_id, DATE(clicked_at);

COMMENT ON VIEW v_daily_click_stats IS 'Aggregated daily click statistics per short link';

-- ============================================================================
-- SAMPLE ROW-LEVEL SECURITY (RLS) POLICIES (Optional)
-- ============================================================================

-- Example: Enable RLS for multi-tenant isolation (uncomment to use)
-- ALTER TABLE short_link ENABLE ROW LEVEL SECURITY;
--
-- CREATE POLICY short_link_workspace_isolation ON short_link
--     USING (workspace_id = current_setting('app.current_workspace_id')::bigint);
--
-- COMMENT ON POLICY short_link_workspace_isolation ON short_link IS
--     'Enforce workspace isolation at database level - requires setting app.current_workspace_id session variable';

-- ============================================================================
-- INDEXES FOR FULL-TEXT SEARCH (Optional)
-- ============================================================================

-- Full-text search on short_link title and description
CREATE INDEX idx_short_link_fulltext ON short_link
    USING GIN (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, '')));

COMMENT ON INDEX idx_short_link_fulltext IS 'Full-text search index for title and description';

-- ============================================================================
-- MAINTENANCE FUNCTIONS
-- ============================================================================

-- Function to clean up expired short links
CREATE OR REPLACE FUNCTION cleanup_expired_links()
RETURNS TABLE (deactivated_count BIGINT) AS $$
BEGIN
    UPDATE short_link
    SET is_active = FALSE,
        updated_at = CURRENT_TIMESTAMP
    WHERE expires_at IS NOT NULL
      AND expires_at < CURRENT_TIMESTAMP
      AND is_active = TRUE;

    GET DIAGNOSTICS deactivated_count = ROW_COUNT;
    RETURN QUERY SELECT deactivated_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_links() IS 'Deactivate expired short links - run periodically via cron job';

-- Function to clean up expired API keys
CREATE OR REPLACE FUNCTION cleanup_expired_api_keys()
RETURNS TABLE (deleted_count BIGINT) AS $$
BEGIN
    DELETE FROM api_key
    WHERE expires_at IS NOT NULL
      AND expires_at < CURRENT_TIMESTAMP - INTERVAL '30 days';

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN QUERY SELECT deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_api_keys() IS 'Hard delete API keys 30 days after expiration - run periodically';
```

## Index Strategy Summary

### Indexes for Performance

| Table | Index | Purpose | Query Pattern |
|-------|-------|---------|---------------|
| short_link | `(workspace_id, short_code)` | **CRITICAL** - Main redirect lookup | `WHERE workspace_id = ? AND short_code = ?` |
| short_link | `(workspace_id, normalized_url)` | **CRITICAL** - Deterministic lookup | `WHERE workspace_id = ? AND normalized_url = ?` |
| short_link | `(workspace_id, created_at DESC)` | List links by recency | `WHERE workspace_id = ? ORDER BY created_at DESC` |
| click_event | `(short_link_id, clicked_at DESC)` | Link analytics timeline | `WHERE short_link_id = ? ORDER BY clicked_at DESC` |
| click_event | `(clicked_at)` | Time-series queries | Aggregations by date/time |
| api_key | `(key_hash)` | API authentication | `WHERE key_hash = ?` |

### Estimated Index Sizes

Based on typical usage patterns:
- **short_link** primary index: ~50-100 bytes per row
- **click_event** indexes: Consider partitioning after 10M+ rows
- **Full-text search** index: ~30% of total text data size

## Query Patterns

### 1. URL Redirect (Most Critical)
```sql
-- Lookup for redirect (must be sub-10ms)
SELECT original_url, is_active, expires_at
FROM short_link
WHERE workspace_id = ? AND short_code = ? AND is_deleted = FALSE;

-- Uses index: idx_short_link_workspace_code (unique)
```

### 2. Deterministic URL Check
```sql
-- Check if URL already exists before creating new short link
SELECT short_code
FROM short_link
WHERE workspace_id = ? AND normalized_url = ? AND is_deleted = FALSE;

-- Uses index: idx_short_link_workspace_normalized_url (unique)
```

### 3. Create Short Link (Deterministic)
```sql
-- Application logic:
-- 1. Normalize the URL
-- 2. Check if exists using query #2
-- 3. If exists, return existing short_code
-- 4. If not exists, generate new short_code and insert

INSERT INTO short_link (
    workspace_id, short_code, original_url, normalized_url,
    title, created_by
) VALUES (?, ?, ?, ?, ?, ?)
ON CONFLICT (workspace_id, normalized_url) DO NOTHING
RETURNING short_code;

-- If no rows returned, query for existing short_code
```

### 4. Click Analytics
```sql
-- Recent clicks for a link
SELECT clicked_at, country, device_type, browser
FROM click_event
WHERE short_link_id = ?
ORDER BY clicked_at DESC
LIMIT 100;

-- Uses index: idx_click_event_short_link_clicked_at
```

### 5. Top Links by Workspace
```sql
-- Most popular links in workspace
SELECT short_code, original_url, title, click_count
FROM short_link
WHERE workspace_id = ? AND is_deleted = FALSE
ORDER BY click_count DESC
LIMIT 20;

-- Uses index: idx_short_link_workspace_created_at (partial scan)
```

## Migration and Deployment Strategy

### Initial Deployment
1. Run Flyway migration `V1__create_initial_schema.sql`
2. Create initial workspace(s)
3. Create admin user(s)

### Future Migrations
- Use Flyway versioned migrations (V2__, V3__, etc.)
- Always test migrations on staging with production-like data volume
- Consider pt-online-schema-change for large tables in production

### Partitioning click_event (Recommended)
```sql
-- After initial deployment, convert to partitioned table
-- V2__partition_click_event.sql

-- Create new partitioned table
CREATE TABLE click_event_new (LIKE click_event INCLUDING ALL)
PARTITION BY RANGE (clicked_at);

-- Create partitions for past and future months
-- ... create partitions ...

-- Migrate data
INSERT INTO click_event_new SELECT * FROM click_event;

-- Rename tables
DROP TABLE click_event;
ALTER TABLE click_event_new RENAME TO click_event;
```

## Monitoring and Maintenance

### Regular Tasks
1. **Daily**: Run `cleanup_expired_links()` to deactivate expired links
2. **Weekly**: Analyze click statistics and table bloat
3. **Monthly**:
   - Create new click_event partition for next month
   - Archive and drop old click_event partitions (based on retention policy)
   - Run `cleanup_expired_api_keys()` to remove old keys
   - Vacuum and reindex as needed

### Performance Monitoring
- Monitor query execution time for main redirect query (should be <10ms)
- Track index usage with `pg_stat_user_indexes`
- Monitor table growth, especially click_event
- Set up alerts for slow queries (>100ms)

### Backup Strategy
- Full database backup daily (for disaster recovery)
- Point-in-time recovery (PITR) with WAL archival
- Before archiving old click_event data, export to S3/warehouse

## Security Considerations

1. **Row-Level Security**: Consider enabling RLS for workspace isolation
2. **API Keys**: Always hash with SHA-256 or better; never log plain keys
3. **IP Anonymization**: For GDPR compliance, consider anonymizing last octet of IP addresses
4. **Encryption**: Use PostgreSQL SSL/TLS for connections; consider pgcrypto for sensitive data
5. **Audit Logging**: Enable PostgreSQL audit logging for compliance
6. **Backup Encryption**: Encrypt backups at rest and in transit

## Scalability Considerations

### Current Design Handles
- 100M+ short links per workspace
- 1B+ click events (with partitioning)
- 10K+ workspaces
- Sub-10ms redirect queries

### Future Optimization Options
1. **Read Replicas**: For analytics queries, use PostgreSQL read replicas
2. **Caching**: Cache hot short_link records in Redis (99% hit rate expected)
3. **Sharding**: Shard by workspace_id if single database becomes bottleneck
4. **Archival**: Move old click_event data to columnar store (e.g., ClickHouse, BigQuery)
5. **CDN**: Serve redirects from edge locations with cached mappings

## Appendix: Example Data

```sql
-- Example workspace
INSERT INTO workspace (name, slug) VALUES ('Acme Corp', 'acme-corp');

-- Example user
INSERT INTO users (workspace_id, email, password_hash, full_name, role)
VALUES (1, 'admin@acme.com', '$2a$10$...', 'John Admin', 'admin');

-- Example short link
INSERT INTO short_link (
    workspace_id, short_code, original_url, normalized_url,
    title, created_by
) VALUES (
    1,
    'abc123',
    'https://example.com/very/long/url?utm_source=twitter',
    'https://example.com/very/long/url',
    'Example Link',
    1
);

-- Example click event
INSERT INTO click_event (
    short_link_id, ip_address, country, device_type, browser, os
) VALUES (
    1, '192.168.1.1', 'US', 'desktop', 'Chrome 120', 'Windows 11'
);

-- Example API key (key_hash would be SHA-256 of actual key)
INSERT INTO api_key (
    workspace_id, key_hash, key_prefix, name, created_by, scopes
) VALUES (
    1,
    'abc123...def456',
    'sk_live_abcd1234',
    'Production API Key',
    1,
    '["links:read", "links:write", "analytics:read"]'::jsonb
);
```

---

**Document Version**: 1.0
**Last Updated**: 2025-11-18
**Authors**: Database Architecture Team
**Status**: Initial Design - Ready for Implementation
