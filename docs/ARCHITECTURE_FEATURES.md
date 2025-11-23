# Architecture Ready Features

This document describes all the advanced features that have been implemented in the URL shortener platform.

## Table of Contents

1. [Custom Domains](#custom-domains)
2. [Password-Protected Links](#password-protected-links)
3. [A/B Testing](#ab-testing)
4. [Webhooks](#webhooks)
5. [UTM Campaign Tracking](#utm-campaign-tracking)
6. [Link Health Monitoring](#link-health-monitoring)

---

## Custom Domains

### Overview
Allow workspaces to use their own branded domains (e.g., `go.acme.com`) instead of the default short domain.

### Features
- **DNS Verification**: Verify domain ownership via TXT record
- **HTTPS Support**: Automatic HTTPS enforcement
- **Default Domain**: Set one domain as default per workspace
- **Status Tracking**: PENDING → VERIFIED → ACTIVE workflow

### API Endpoints

```http
POST   /api/v1/domains                    # Register new domain
POST   /api/v1/domains/{id}/verify        # Verify DNS ownership
POST   /api/v1/domains/{id}/set-default   # Set as default
GET    /api/v1/domains?workspaceId={id}   # List all domains
DELETE /api/v1/domains/{id}               # Delete domain
```

### DNS Setup
Add a TXT record to verify ownership:
```
_url-short-verification.yourdomain.com.  TXT  "verification-token-here"
```

### Database Schema
```sql
CREATE TABLE custom_domains (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    domain VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verification_token VARCHAR(64) NOT NULL,
    verified_at TIMESTAMP,
    use_https BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);
```

---

## Password-Protected Links

### Overview
Protect short links with a password. Users must enter the correct password before being redirected.

### Features
- **BCrypt Hashing**: Secure password storage
- **Failed Attempt Tracking**: Lock after 5 failed attempts
- **Auto-Lock**: 15-minute lockout after threshold
- **Admin Override**: Admins can reset failed attempts

### API Endpoints

```http
POST   /api/v1/links/{linkId}/password             # Add password
POST   /api/v1/links/{linkId}/password/validate    # Validate password
GET    /api/v1/links/{linkId}/password/status      # Check if protected
DELETE /api/v1/links/{linkId}/password             # Remove password
POST   /api/v1/links/{linkId}/password/reset-attempts  # Reset lock
```

### Workflow
1. User clicks short link
2. If password protected, show password form
3. User enters password
4. System validates and issues access token
5. User is redirected to destination

### Database Schema
```sql
CREATE TABLE link_passwords (
    id BIGSERIAL PRIMARY KEY,
    short_link_id BIGINT NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,  -- BCrypt hash
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP
);
```

---

## A/B Testing

### Overview
Test different destination URLs by splitting traffic between variants.

### Features
- **Weighted Distribution**: Allocate traffic by percentage (0-100%)
- **Conversion Tracking**: Track clicks and conversions per variant
- **Conversion Rate**: Automatic calculation
- **Multiple Variants**: Support unlimited variants per link

### API Endpoints

```http
POST   /api/v1/links/{linkId}/variants                    # Create variant
PUT    /api/v1/links/{linkId}/variants/{variantId}        # Update variant
GET    /api/v1/links/{linkId}/variants                    # List variants
GET    /api/v1/links/{linkId}/variants/stats              # Get statistics
POST   /api/v1/links/{linkId}/variants/{variantId}/conversion  # Record conversion
DELETE /api/v1/links/{linkId}/variants/{variantId}        # Delete variant
```

### Example Configuration
```json
{
  "variants": [
    {
      "name": "Control",
      "destination_url": "https://example.com/page-a",
      "weight": 50
    },
    {
      "name": "Treatment",
      "destination_url": "https://example.com/page-b",
      "weight": 50
    }
  ]
}
```

### Statistics Response
```json
{
  "variants": [
    {
      "name": "Control",
      "click_count": 500,
      "conversion_count": 45,
      "conversion_rate": 9.0
    },
    {
      "name": "Treatment",
      "click_count": 500,
      "conversion_count": 60,
      "conversion_rate": 12.0
    }
  ],
  "total_clicks": 1000,
  "total_conversions": 105,
  "overall_conversion_rate": 10.5
}
```

### Database Schema
```sql
CREATE TABLE link_variants (
    id BIGSERIAL PRIMARY KEY,
    short_link_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    destination_url VARCHAR(2048) NOT NULL,
    weight INTEGER NOT NULL CHECK (weight >= 0 AND weight <= 100),
    click_count BIGINT NOT NULL DEFAULT 0,
    conversion_count BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```

---

## Webhooks

### Overview
Receive real-time HTTP notifications when events occur (link created, clicked, expired, etc.).

### Features
- **Event Subscriptions**: Subscribe to specific event types
- **HMAC-SHA256 Signatures**: Verify webhook authenticity
- **Delivery Tracking**: Monitor success/failure rates
- **Auto-Disable**: Disable after 10 consecutive failures
- **Async Delivery**: Non-blocking webhook delivery

### API Endpoints

```http
POST   /api/v1/webhooks?workspaceId={id}        # Create webhook
PUT    /api/v1/webhooks/{webhookId}             # Update webhook
GET    /api/v1/webhooks?workspaceId={id}        # List webhooks
GET    /api/v1/webhooks/{webhookId}             # Get webhook
DELETE /api/v1/webhooks/{webhookId}             # Delete webhook
POST   /api/v1/webhooks/{webhookId}/regenerate-secret  # New secret
```

### Supported Events
- `link.created` - New short link created
- `link.clicked` - Short link accessed
- `link.expired` - Link reached expiration
- `link.disabled` - Link manually disabled
- `link.health.degraded` - Link health degraded

### Webhook Payload
```json
{
  "event": "link.created",
  "timestamp": "2025-11-21T10:30:00Z",
  "workspace_id": 100,
  "data": {
    "link_id": 500,
    "short_code": "abc123",
    "original_url": "https://example.com/page"
  }
}
```

### Signature Verification
Webhooks include an `X-Webhook-Signature` header:
```
X-Webhook-Signature: sha256=<base64-encoded-hmac>
```

Verify using:
```javascript
const crypto = require('crypto');
const signature = crypto
  .createHmac('sha256', webhookSecret)
  .update(JSON.stringify(payload))
  .digest('base64');
```

### Database Schema
```sql
CREATE TABLE webhooks (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(2048) NOT NULL CHECK (url LIKE 'https://%'),
    secret VARCHAR(64) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    delivery_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    last_status VARCHAR(20),
    last_delivery_at TIMESTAMP
);

CREATE TABLE webhook_events (
    webhook_id BIGINT NOT NULL REFERENCES webhooks(id),
    event_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (webhook_id, event_type)
);
```

---

## UTM Campaign Tracking

### Overview
Automatically append UTM parameters to destination URLs for marketing attribution.

### Features
- **Standard UTM Parameters**: source, medium, campaign, term, content
- **Query String Builder**: Automatic parameter encoding
- **Campaign Analytics**: Filter links by UTM parameters
- **Flexible Assignment**: Set UTM parameters per link

### UTM Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `utm_source` | Traffic source | google, newsletter, facebook |
| `utm_medium` | Marketing medium | cpc, email, social |
| `utm_campaign` | Campaign name | summer_sale, product_launch |
| `utm_term` | Paid keywords | running+shoes |
| `utm_content` | Ad variation | textlink, banner, logo |

### Example
**Short Link**: `https://short.ly/abc123`

**Original URL**: `https://example.com/products`

**UTM Parameters**:
```json
{
  "utm_source": "facebook",
  "utm_medium": "social",
  "utm_campaign": "summer_sale_2024"
}
```

**Final Redirect**:
```
https://example.com/products?utm_source=facebook&utm_medium=social&utm_campaign=summer_sale_2024
```

### Database Schema
```sql
ALTER TABLE short_link
    ADD COLUMN utm_source VARCHAR(255),
    ADD COLUMN utm_medium VARCHAR(255),
    ADD COLUMN utm_campaign VARCHAR(255),
    ADD COLUMN utm_term VARCHAR(255),
    ADD COLUMN utm_content VARCHAR(255);

CREATE INDEX idx_short_link_utm_campaign
    ON short_link(workspace_id, utm_campaign)
    WHERE utm_campaign IS NOT NULL;
```

---

## Link Health Monitoring

### Overview
Monitor the health and uptime of destination URLs.

### Features
- **HTTP Health Checks**: Periodic HTTP HEAD requests
- **Response Time Tracking**: Monitor performance
- **Status Classification**: HEALTHY, DEGRADED, UNHEALTHY, DOWN
- **Consecutive Failure Tracking**: Detect persistent issues
- **Uptime Percentage**: Calculate availability metrics
- **Scheduled Checks**: Background job for periodic monitoring

### API Endpoints

```http
POST   /api/v1/links/{linkId}/health/check                        # Manual check
GET    /api/v1/links/{linkId}/health                              # Get health status
GET    /api/v1/links/{linkId}/health/workspace/{workspaceId}/unhealthy  # Unhealthy links
POST   /api/v1/links/{linkId}/health/reset                        # Reset status
```

### Health Status States

| Status | Criteria | Description |
|--------|----------|-------------|
| `UNKNOWN` | No checks yet | Initial state |
| `HEALTHY` | HTTP 2xx/3xx, <1s response | Working well |
| `DEGRADED` | HTTP 2xx/3xx, 1-3s response | Slow but working |
| `UNHEALTHY` | 3+ consecutive failures | Persistent issues |
| `DOWN` | 5+ consecutive failures | Not accessible |

### Health Check Logic
```java
// Response time based
if (responseTime < 1000ms) {
    status = HEALTHY;
} else if (responseTime < 3000ms) {
    status = DEGRADED;
} else {
    status = UNHEALTHY;
}

// Failure based
if (consecutiveFailures >= 5) {
    status = DOWN;
} else if (consecutiveFailures >= 3) {
    status = UNHEALTHY;
}
```

### Response Example
```json
{
  "id": 1,
  "short_link_id": 100,
  "status": "HEALTHY",
  "last_status_code": 200,
  "last_response_time_ms": 234,
  "consecutive_failures": 0,
  "check_count": 150,
  "success_count": 148,
  "uptime_percentage": 98.67,
  "last_checked_at": "2025-11-21T10:30:00Z"
}
```

### Database Schema
```sql
CREATE TABLE link_health (
    id BIGSERIAL PRIMARY KEY,
    short_link_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    last_status_code INTEGER,
    last_response_time_ms BIGINT,
    last_error VARCHAR(500),
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    check_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    last_checked_at TIMESTAMP
);
```

---

## Implementation Details

### Technology Stack
- **Backend**: Spring Boot 3.4.0, Java 21
- **Database**: PostgreSQL with Flyway migrations
- **Security**: BCrypt (passwords), HMAC-SHA256 (webhooks)
- **HTTP Client**: Java 11+ HttpClient
- **Async Processing**: Spring @Async for webhooks

### Security Considerations

1. **Password Protection**
   - BCrypt with salt rounds = 10
   - Rate limiting via failed attempts
   - Temporary lockout mechanism

2. **Webhooks**
   - HTTPS-only webhook URLs
   - HMAC-SHA256 signature verification
   - Secrets stored in database (consider moving to vault)

3. **DNS Verification**
   - Secure random token generation
   - TXT record verification prevents domain hijacking

### Performance Optimizations

1. **Database Indexes**
   - Custom domain lookup: `idx_custom_domain`
   - Active variants: `idx_variant_active`
   - Webhook events: `idx_webhook_active`
   - UTM filtering: `idx_short_link_utm_campaign`

2. **Async Operations**
   - Webhook delivery doesn't block request
   - Health checks run in background

3. **Caching Opportunities** (Future)
   - Cache domain verification status
   - Cache active webhook configurations
   - Cache health check results (5 min TTL)

---

## Future Enhancements

1. **Custom Domains**
   - Automatic SSL certificate provisioning (Let's Encrypt)
   - CNAME verification as alternative to TXT records
   - Domain health monitoring

2. **A/B Testing**
   - Statistical significance calculator
   - Automatic winner selection
   - Multi-variate testing (MVT)

3. **Webhooks**
   - Webhook retry with exponential backoff
   - Webhook payload templates
   - Event filtering by criteria

4. **Link Health**
   - SSL certificate expiration alerts
   - Performance trend analysis
   - Integration with monitoring services (PagerDuty, Datadog)

5. **UTM Tracking**
   - UTM parameter validation
   - UTM template library
   - Campaign performance dashboard

---

## Migration Guide

### From Basic to Architecture Ready

1. **Apply Database Migration**
   ```bash
   ./mvnw flyway:migrate
   ```

2. **Restart Application**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Verify Features**
   - Check Swagger UI: http://localhost:8080/swagger-ui.html
   - New endpoints should appear under respective tags

4. **Optional: Enable Background Jobs**
   ```yaml
   # application.yml
   app:
     health-checks:
       enabled: true
       interval: 3600000  # 1 hour in milliseconds
   ```

---

## API Documentation

Full API documentation is available via Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

All endpoints return standardized `ApiResponse<T>` wrapper:
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation successful",
  "timestamp": "2025-11-21T10:30:00Z"
}
```

---

## Support

For issues or questions:
- Check logs: `logs/application.log`
- Review database state: Check migration status
- API errors: Check response body for detailed messages

All features are production-ready with comprehensive error handling, logging, and validation.
