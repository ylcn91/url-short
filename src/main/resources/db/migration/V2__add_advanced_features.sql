-- Migration V2: Add Architecture Ready Features
-- Custom Domains, Password Protection, A/B Testing, Webhooks, UTM, Health Monitoring

-- ============================================================================
-- 1. Custom Domains
-- ============================================================================
CREATE TABLE custom_domains (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    domain VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verification_token VARCHAR(64) NOT NULL,
    verified_at TIMESTAMP,
    use_https BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_domain_status CHECK (status IN ('PENDING', 'VERIFIED', 'FAILED', 'DISABLED'))
);

CREATE INDEX idx_custom_domain ON custom_domains(domain);
CREATE INDEX idx_workspace_domain ON custom_domains(workspace_id);
CREATE INDEX idx_domain_status ON custom_domains(status);

COMMENT ON TABLE custom_domains IS 'Custom branded domains for workspaces';
COMMENT ON COLUMN custom_domains.domain IS 'Custom domain like go.acme.com';
COMMENT ON COLUMN custom_domains.verification_token IS 'Token for DNS TXT record verification';

-- ============================================================================
-- 2. Link Passwords (Password-Protected Links)
-- ============================================================================
CREATE TABLE link_passwords (
    id BIGSERIAL PRIMARY KEY,
    short_link_id BIGINT NOT NULL UNIQUE REFERENCES short_link(id) ON DELETE CASCADE,
    password_hash VARCHAR(60) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_failed_attempts CHECK (failed_attempts >= 0)
);

CREATE INDEX idx_link_password ON link_passwords(short_link_id);

COMMENT ON TABLE link_passwords IS 'Password protection for short links';
COMMENT ON COLUMN link_passwords.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN link_passwords.failed_attempts IS 'Number of failed password attempts';
COMMENT ON COLUMN link_passwords.locked_until IS 'Lock time after too many failed attempts';

-- ============================================================================
-- 3. Link Variants (A/B Testing)
-- ============================================================================
CREATE TABLE link_variants (
    id BIGSERIAL PRIMARY KEY,
    short_link_id BIGINT NOT NULL REFERENCES short_link(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    destination_url VARCHAR(2048) NOT NULL,
    weight INTEGER NOT NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    conversion_count BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_variant_weight CHECK (weight >= 0 AND weight <= 100),
    CONSTRAINT chk_variant_clicks CHECK (click_count >= 0),
    CONSTRAINT chk_variant_conversions CHECK (conversion_count >= 0 AND conversion_count <= click_count)
);

CREATE INDEX idx_variant_link ON link_variants(short_link_id);
CREATE INDEX idx_variant_active ON link_variants(is_active);

COMMENT ON TABLE link_variants IS 'A/B testing variants for short links';
COMMENT ON COLUMN link_variants.weight IS 'Traffic allocation percentage (0-100)';
COMMENT ON COLUMN link_variants.click_count IS 'Number of clicks this variant received';
COMMENT ON COLUMN link_variants.conversion_count IS 'Number of conversions tracked';

-- ============================================================================
-- 4. Webhooks
-- ============================================================================
CREATE TABLE webhooks (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    secret VARCHAR(64) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    delivery_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    last_status VARCHAR(20),
    last_delivery_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_webhook_url CHECK (url LIKE 'https://%'),
    CONSTRAINT chk_delivery_count CHECK (delivery_count >= 0),
    CONSTRAINT chk_failure_count CHECK (failure_count >= 0 AND failure_count <= delivery_count)
);

CREATE TABLE webhook_events (
    webhook_id BIGINT NOT NULL REFERENCES webhooks(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,

    PRIMARY KEY (webhook_id, event_type)
);

CREATE INDEX idx_webhook_workspace ON webhooks(workspace_id);
CREATE INDEX idx_webhook_active ON webhooks(is_active);

COMMENT ON TABLE webhooks IS 'Webhook configurations for event notifications';
COMMENT ON TABLE webhook_events IS 'Event types subscribed by each webhook';
COMMENT ON COLUMN webhooks.secret IS 'Secret key for HMAC-SHA256 signature verification';
COMMENT ON COLUMN webhooks.delivery_count IS 'Total delivery attempts';
COMMENT ON COLUMN webhooks.failure_count IS 'Failed delivery attempts';

-- ============================================================================
-- 5. UTM Parameters (Add to ShortLink)
-- ============================================================================
ALTER TABLE short_link ADD COLUMN utm_source VARCHAR(255);
ALTER TABLE short_link ADD COLUMN utm_medium VARCHAR(255);
ALTER TABLE short_link ADD COLUMN utm_campaign VARCHAR(255);
ALTER TABLE short_link ADD COLUMN utm_term VARCHAR(255);
ALTER TABLE short_link ADD COLUMN utm_content VARCHAR(255);

CREATE INDEX idx_short_link_utm_campaign ON short_link(workspace_id, utm_campaign) WHERE utm_campaign IS NOT NULL;
CREATE INDEX idx_short_link_utm_source ON short_link(workspace_id, utm_source) WHERE utm_source IS NOT NULL;

COMMENT ON COLUMN short_link.utm_source IS 'UTM source parameter for campaign tracking';
COMMENT ON COLUMN short_link.utm_medium IS 'UTM medium parameter for campaign tracking';
COMMENT ON COLUMN short_link.utm_campaign IS 'UTM campaign parameter for campaign tracking';
COMMENT ON COLUMN short_link.utm_term IS 'UTM term parameter for paid search keywords';
COMMENT ON COLUMN short_link.utm_content IS 'UTM content parameter to differentiate ads';

-- ============================================================================
-- 6. Link Health Monitoring
-- ============================================================================
CREATE TABLE link_health (
    id BIGSERIAL PRIMARY KEY,
    short_link_id BIGINT NOT NULL UNIQUE REFERENCES short_link(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    last_status_code INTEGER,
    last_response_time_ms BIGINT,
    last_error VARCHAR(500),
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    check_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    last_checked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_health_status CHECK (status IN ('UNKNOWN', 'HEALTHY', 'DEGRADED', 'UNHEALTHY', 'DOWN')),
    CONSTRAINT chk_health_checks CHECK (check_count >= 0),
    CONSTRAINT chk_health_success CHECK (success_count >= 0 AND success_count <= check_count),
    CONSTRAINT chk_consecutive_failures CHECK (consecutive_failures >= 0)
);

CREATE INDEX idx_health_link ON link_health(short_link_id);
CREATE INDEX idx_health_status ON link_health(status);
CREATE INDEX idx_health_check_time ON link_health(last_checked_at);

COMMENT ON TABLE link_health IS 'Health monitoring for short link destinations';
COMMENT ON COLUMN link_health.status IS 'Current health status of the link';
COMMENT ON COLUMN link_health.last_status_code IS 'Last HTTP status code received';
COMMENT ON COLUMN link_health.last_response_time_ms IS 'Last response time in milliseconds';
COMMENT ON COLUMN link_health.consecutive_failures IS 'Number of consecutive failed checks';
COMMENT ON COLUMN link_health.check_count IS 'Total number of health checks performed';
COMMENT ON COLUMN link_health.success_count IS 'Number of successful health checks';

-- ============================================================================
-- 7. Update Triggers
-- ============================================================================

-- Auto-update trigger for custom_domains
CREATE TRIGGER update_custom_domains_updated_at
    BEFORE UPDATE ON custom_domains
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Auto-update trigger for webhooks
CREATE TRIGGER update_webhooks_updated_at
    BEFORE UPDATE ON webhooks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Auto-update trigger for link_health
CREATE TRIGGER update_link_health_updated_at
    BEFORE UPDATE ON link_health
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 8. Seed Example Data (Optional - for development)
-- ============================================================================

-- Insert example webhook events
COMMENT ON TABLE webhook_events IS 'Supported events: link.created, link.clicked, link.expired, link.disabled, link.health.degraded';
