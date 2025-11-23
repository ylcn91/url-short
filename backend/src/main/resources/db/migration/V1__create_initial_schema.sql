-- ============================================================================
-- Flyway Migration V1: Initial Schema for URL Shortener Platform
-- ============================================================================
-- Description: Creates the foundational database schema for a multi-tenant
--              URL shortener with deterministic shortening support.
--
-- Entities:
--   - workspace: Multi-tenant organization/workspace
--   - users: User accounts within workspaces
--   - short_link: Core URL shortening entity with deterministic reuse
--   - click_event: Analytics and click tracking
--   - api_key: API keys for programmatic access
--
-- Key Features:
--   - Deterministic shortening: same URL + workspace = same short code
--   - Multi-tenant isolation with workspace_id
--   - Soft delete for workspace, users, short_link
--   - Performance-optimized indexes for redirect lookups
--   - Partitioning-ready click_event table for high-volume analytics
-- ============================================================================

-- ============================================================================
-- EXTENSIONS
-- ============================================================================

-- UUID generation support (for future use)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

-- User roles within a workspace
CREATE TYPE user_role AS ENUM ('admin', 'member', 'viewer');

-- Device types for click analytics
CREATE TYPE device_type AS ENUM ('desktop', 'mobile', 'tablet', 'bot', 'unknown');

-- ============================================================================
-- TABLE: workspace
-- Description: Multi-tenant workspace/organization table
-- ============================================================================

CREATE TABLE workspace (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    settings JSONB DEFAULT '{}'::jsonb,

    -- Slug must be lowercase alphanumeric with hyphens, start/end with alphanumeric
    CONSTRAINT workspace_slug_format CHECK (slug ~ '^[a-z0-9][a-z0-9-]*[a-z0-9]$')
);

-- Indexes
CREATE UNIQUE INDEX idx_workspace_slug ON workspace(slug);
CREATE INDEX idx_workspace_is_deleted ON workspace(is_deleted) WHERE is_deleted = FALSE;

-- Comments
COMMENT ON TABLE workspace IS 'Multi-tenant workspace/organization table';
COMMENT ON COLUMN workspace.slug IS 'URL-safe identifier for subdomains and routing';
COMMENT ON COLUMN workspace.is_deleted IS 'Soft delete flag to preserve tenant data for audit and compliance';
COMMENT ON COLUMN workspace.settings IS 'Workspace-level configuration stored as JSONB for flexibility';

-- ============================================================================
-- TABLE: users
-- Description: User accounts scoped to workspaces
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

    -- Basic email format validation
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Indexes
CREATE UNIQUE INDEX idx_users_workspace_email ON users(workspace_id, email);
CREATE INDEX idx_users_workspace_id ON users(workspace_id);
CREATE INDEX idx_users_is_deleted ON users(is_deleted) WHERE is_deleted = FALSE;

-- Comments
COMMENT ON TABLE users IS 'User accounts scoped to workspaces for multi-tenant isolation';
COMMENT ON COLUMN users.role IS 'User role: admin (full access), member (standard), viewer (read-only)';
COMMENT ON COLUMN users.is_deleted IS 'Soft delete to maintain referential integrity with created content';
COMMENT ON CONSTRAINT users_email_format ON users IS 'Basic email format validation';

-- ============================================================================
-- TABLE: short_link
-- Description: Core entity for shortened URLs with deterministic reuse support
--
-- CRITICAL DESIGN:
-- - UNIQUE(workspace_id, short_code): Each short code is unique per workspace
-- - UNIQUE(workspace_id, normalized_url): Enables deterministic shortening
--   Same URL in same workspace always returns the same short code
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

    -- Constraints
    CONSTRAINT short_link_short_code_format CHECK (short_code ~ '^[a-zA-Z0-9_-]+$'),
    CONSTRAINT short_link_click_count_non_negative CHECK (click_count >= 0),
    CONSTRAINT short_link_original_url_not_empty CHECK (length(trim(original_url)) > 0),
    CONSTRAINT short_link_normalized_url_not_empty CHECK (length(trim(normalized_url)) > 0)
);

-- ============================================================================
-- CRITICAL INDEXES FOR short_link
-- ============================================================================

-- PRIMARY LOOKUP PATH: URL redirection (must be <10ms)
-- This is the most critical query in the entire system
CREATE UNIQUE INDEX idx_short_link_workspace_code ON short_link(workspace_id, short_code);

-- DETERMINISTIC REUSE: Check if normalized URL already exists
-- Ensures same URL + workspace = same short code
CREATE UNIQUE INDEX idx_short_link_workspace_normalized_url ON short_link(workspace_id, normalized_url);

-- Performance indexes for common queries
CREATE INDEX idx_short_link_workspace_created_at ON short_link(workspace_id, created_at DESC);
CREATE INDEX idx_short_link_created_by ON short_link(created_by);
CREATE INDEX idx_short_link_expires_at ON short_link(expires_at) WHERE expires_at IS NOT NULL;

-- Partial index for most common query pattern (active, non-deleted links)
CREATE INDEX idx_short_link_active ON short_link(workspace_id, created_at DESC)
    WHERE is_deleted = FALSE AND is_active = TRUE;

-- Comments
COMMENT ON TABLE short_link IS 'Core entity for shortened URLs with deterministic reuse support';
COMMENT ON COLUMN short_link.short_code IS 'Short identifier (e.g., "abc123") - unique per workspace';
COMMENT ON COLUMN short_link.normalized_url IS 'Normalized URL for deterministic matching - ensures same URL gets same short code';
COMMENT ON COLUMN short_link.click_count IS 'Denormalized counter from click_event for performance - updated via trigger';
COMMENT ON COLUMN short_link.is_deleted IS 'Soft delete to preserve analytics history and prevent code reuse';
COMMENT ON COLUMN short_link.metadata IS 'JSONB field for custom tags, categories, and extensibility';
COMMENT ON CONSTRAINT short_link_short_code_format ON short_link IS 'Alphanumeric with underscore and hyphen allowed';

-- ============================================================================
-- TABLE: click_event
-- Description: Analytics and tracking data for each click on a short link
--
-- DESIGN NOTE: High-volume table - recommend partitioning by clicked_at
-- after reaching 10M+ rows. See documentation for partitioning strategy.
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

    -- Constraint: clicks cannot be in the future
    CONSTRAINT click_event_clicked_at_not_future CHECK (clicked_at <= CURRENT_TIMESTAMP)
);

-- Indexes for analytics queries
CREATE INDEX idx_click_event_short_link_clicked_at ON click_event(short_link_id, clicked_at DESC);
CREATE INDEX idx_click_event_clicked_at ON click_event(clicked_at DESC);
CREATE INDEX idx_click_event_country ON click_event(short_link_id, country);
CREATE INDEX idx_click_event_device_type ON click_event(short_link_id, device_type);

-- Comments
COMMENT ON TABLE click_event IS 'Analytics and tracking data for each click - recommend partitioning by clicked_at for high volume';
COMMENT ON COLUMN click_event.ip_address IS 'Client IP address - consider anonymization for GDPR compliance (e.g., mask last octet)';
COMMENT ON COLUMN click_event.country IS 'ISO 3166-1 alpha-2 country code derived from IP geolocation';
COMMENT ON COLUMN click_event.clicked_at IS 'Click timestamp - partition key for table partitioning strategy';

-- ============================================================================
-- TABLE: api_key
-- Description: API keys for programmatic access to the platform
--
-- SECURITY NOTE: Store only SHA-256 hashes, never plain API keys
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

    -- Scopes must be a JSON array
    CONSTRAINT api_key_scopes_is_array CHECK (jsonb_typeof(scopes) = 'array')
);

-- Indexes
CREATE UNIQUE INDEX idx_api_key_hash ON api_key(key_hash);
CREATE UNIQUE INDEX idx_api_key_workspace_prefix ON api_key(workspace_id, key_prefix);
CREATE INDEX idx_api_key_workspace_active ON api_key(workspace_id, is_active);
CREATE INDEX idx_api_key_expires_at ON api_key(expires_at) WHERE expires_at IS NOT NULL;

-- Comments
COMMENT ON TABLE api_key IS 'API keys for programmatic access - store only SHA-256 hashed values for security';
COMMENT ON COLUMN api_key.key_hash IS 'SHA-256 hash of the API key - NEVER store plain keys in database';
COMMENT ON COLUMN api_key.key_prefix IS 'First 8-12 characters for identification (e.g., "sk_live_abcd1234")';
COMMENT ON COLUMN api_key.scopes IS 'JSONB array of permission scopes (e.g., ["links:read", "links:write", "analytics:read"])';
COMMENT ON COLUMN api_key.last_used_at IS 'Last successful API call timestamp - useful for identifying unused keys';

-- ============================================================================
-- TRIGGERS: Automatic updated_at timestamp management
-- ============================================================================

-- Function to update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_updated_at_column() IS 'Trigger function to automatically update updated_at timestamp on row updates';

-- Apply trigger to tables with updated_at column
CREATE TRIGGER update_workspace_updated_at BEFORE UPDATE ON workspace
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_short_link_updated_at BEFORE UPDATE ON short_link
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TRIGGERS: Denormalized click_count management
-- ============================================================================

-- Function to increment click_count in short_link when click_event is inserted
CREATE OR REPLACE FUNCTION increment_click_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE short_link
    SET click_count = click_count + 1
    WHERE id = NEW.short_link_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION increment_click_count() IS 'Trigger function to automatically increment click_count in short_link when click is recorded';

-- Apply trigger to click_event
CREATE TRIGGER increment_short_link_click_count AFTER INSERT ON click_event
    FOR EACH ROW EXECUTE FUNCTION increment_click_count();

-- ============================================================================
-- VIEWS: Useful views for common queries
-- ============================================================================

-- View: Active short links with workspace and creator details
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

COMMENT ON VIEW v_active_short_links IS 'Active short links with denormalized workspace and creator details for dashboards';

-- View: Daily aggregated click statistics
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

COMMENT ON VIEW v_daily_click_stats IS 'Aggregated daily click statistics per short link for analytics dashboards';

-- ============================================================================
-- INDEXES: Full-text search support (optional but useful)
-- ============================================================================

-- Full-text search on short_link title and description
CREATE INDEX idx_short_link_fulltext ON short_link
    USING GIN (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, '')));

COMMENT ON INDEX idx_short_link_fulltext IS 'Full-text search index for searching links by title and description';

-- ============================================================================
-- MAINTENANCE FUNCTIONS
-- ============================================================================

-- Function: Cleanup expired short links (set is_active = FALSE)
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

COMMENT ON FUNCTION cleanup_expired_links() IS 'Deactivate expired short links - schedule to run periodically (e.g., hourly cron job)';

-- Function: Cleanup expired API keys (hard delete after grace period)
CREATE OR REPLACE FUNCTION cleanup_expired_api_keys()
RETURNS TABLE (deleted_count BIGINT) AS $$
BEGIN
    -- Hard delete API keys that expired more than 30 days ago
    DELETE FROM api_key
    WHERE expires_at IS NOT NULL
      AND expires_at < CURRENT_TIMESTAMP - INTERVAL '30 days';

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN QUERY SELECT deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_api_keys() IS 'Hard delete API keys 30 days after expiration - run periodically (e.g., daily)';

-- ============================================================================
-- SAMPLE ROW-LEVEL SECURITY (RLS) SETUP (commented out by default)
-- ============================================================================

-- Uncomment to enable row-level security for multi-tenant isolation at DB level
-- Requires setting session variable: SET app.current_workspace_id = <workspace_id>

-- ALTER TABLE short_link ENABLE ROW LEVEL SECURITY;
--
-- CREATE POLICY short_link_workspace_isolation ON short_link
--     USING (workspace_id = current_setting('app.current_workspace_id')::bigint);
--
-- COMMENT ON POLICY short_link_workspace_isolation ON short_link IS
--     'Enforce workspace isolation at database level - requires app to set app.current_workspace_id session variable';

-- Similar policies can be added for other tables:
-- ALTER TABLE users ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE api_key ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE click_event ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- END OF MIGRATION V1
-- ============================================================================

-- Grant permissions (adjust as needed for your application user)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO your_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO your_app_user;
