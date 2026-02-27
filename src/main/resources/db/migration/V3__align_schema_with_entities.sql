-- Migration V3: Align database schema with JPA entities
-- Fixes mismatches between Flyway migrations and entity definitions

-- 1. Add is_deleted column to api_key (exists in ApiKey entity but missing from V1)
ALTER TABLE api_key ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Add max_clicks column to short_link (exists in ShortLink entity but missing from V1)
ALTER TABLE short_link ADD COLUMN max_clicks BIGINT;
ALTER TABLE short_link ADD CONSTRAINT short_link_max_clicks_positive CHECK (max_clicks >= 1);

-- 3. Drop views that depend on columns we need to alter
DROP VIEW IF EXISTS v_daily_click_stats;
DROP VIEW IF EXISTS v_active_short_links;

-- 4. Change click_event.ip_address from INET to VARCHAR(45) to match entity
ALTER TABLE click_event ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::VARCHAR(45);

-- 5. Drop click_event future timestamp constraint
ALTER TABLE click_event DROP CONSTRAINT IF EXISTS click_event_clicked_at_not_future;

-- 6. Convert PostgreSQL enum types to VARCHAR to match JPA @Enumerated(STRING)
-- Drop defaults first (they reference the enum type)
ALTER TABLE users ALTER COLUMN role DROP DEFAULT;
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20) USING role::TEXT;
DROP TYPE IF EXISTS user_role CASCADE;

ALTER TABLE click_event ALTER COLUMN device_type DROP DEFAULT;
ALTER TABLE click_event ALTER COLUMN device_type TYPE VARCHAR(20) USING device_type::TEXT;
DROP TYPE IF EXISTS device_type CASCADE;

-- 7. Recreate views with updated column types
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

CREATE VIEW v_daily_click_stats AS
SELECT
    short_link_id,
    DATE(clicked_at) AS click_date,
    COUNT(*) AS total_clicks,
    COUNT(DISTINCT ip_address) AS unique_ips,
    COUNT(*) FILTER (WHERE device_type = 'DESKTOP') AS desktop_clicks,
    COUNT(*) FILTER (WHERE device_type = 'MOBILE') AS mobile_clicks,
    COUNT(*) FILTER (WHERE device_type = 'TABLET') AS tablet_clicks,
    COUNT(DISTINCT country) AS countries_count
FROM click_event
GROUP BY short_link_id, DATE(clicked_at);
