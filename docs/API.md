# API Documentation

Complete REST API reference for the Linkforge URL Shortener Platform.

**Base URL:** `https://api.linkforge.io` (production) or `http://localhost:8080` (local development)

**API Version:** v1

**Interactive Documentation:** Available at `/swagger-ui.html` when running the application

## Table of Contents

- [Authentication](#authentication)
- [Rate Limiting](#rate-limiting)
- [Pagination](#pagination)
- [Error Handling](#error-handling)
- [Endpoints](#endpoints)
  - [Short Links](#short-links)
  - [Analytics](#analytics)
  - [Workspaces](#workspaces)
  - [Users](#users)
  - [API Keys](#api-keys)

---

## Authentication

The API supports two authentication methods:

### 1. JWT Bearer Token (User Authentication)

Used for user-initiated requests via web dashboard or mobile apps.

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Obtaining a JWT:**

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "your_password"
}
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "refresh_abc123...",
  "expires_in": 3600,
  "token_type": "Bearer"
}
```

**Token Expiration:** Access tokens expire after 1 hour. Use the refresh token to obtain a new access token:

```bash
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refresh_token": "refresh_abc123..."
}
```

### 2. API Key Authentication

Used for programmatic access (CI/CD pipelines, integrations, scripts).

```http
X-API-Key: sk_live_abc123def456...
```

**Creating an API Key:**

Via the dashboard at `/settings/api-keys` or via API:

```bash
POST /api/v1/workspaces/{workspaceId}/api-keys
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "Production API Key",
  "scopes": ["links:read", "links:write", "analytics:read"],
  "expires_at": "2026-12-31T23:59:59"
}
```

**Available Scopes:**
- `links:read` - Read short links
- `links:write` - Create, update, delete short links
- `links:delete` - Delete short links
- `analytics:read` - Read analytics data
- `workspaces:read` - Read workspace information
- `workspaces:write` - Manage workspace settings
- `users:read` - Read user information
- `users:write` - Manage users

---

## Rate Limiting

Rate limits are enforced per workspace and authentication method:

| Tier | Authenticated (JWT) | API Key | Anonymous (Redirects) |
|------|---------------------|---------|----------------------|
| **Free** | 100 req/min | 60 req/min | 1000 req/min |
| **Pro** | 1000 req/min | 600 req/min | 10,000 req/min |
| **Team** | 5000 req/min | 3000 req/min | 50,000 req/min |
| **Enterprise** | Custom | Custom | Custom |

**Rate Limit Headers:**

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1700000000
```

**429 Response (Rate Limit Exceeded):**

```json
{
  "error": "rate_limit_exceeded",
  "message": "Rate limit exceeded. Retry after 60 seconds.",
  "retry_after": 60
}
```

---

## Pagination

List endpoints support cursor-based pagination for consistent results:

**Query Parameters:**
- `page` - Page number (default: 0, zero-indexed)
- `size` - Items per page (default: 20, max: 100)
- `sort` - Sort field and direction (e.g., `createdAt,desc`)

**Example Request:**

```bash
GET /api/v1/workspaces/1/links?page=0&size=20&sort=createdAt,desc
```

**Response Structure:**

```json
{
  "content": [...],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 347,
    "totalPages": 18
  },
  "links": {
    "first": "/api/v1/workspaces/1/links?page=0&size=20",
    "next": "/api/v1/workspaces/1/links?page=1&size=20",
    "last": "/api/v1/workspaces/1/links?page=17&size=20"
  }
}
```

---

## Error Handling

All errors follow a consistent JSON structure:

```json
{
  "error": "resource_not_found",
  "message": "Short link not found with code: abc123",
  "details": {
    "workspace_id": 1,
    "short_code": "abc123"
  },
  "timestamp": "2025-11-18T10:30:00Z",
  "path": "/api/v1/workspaces/1/links/abc123"
}
```

### HTTP Status Codes

| Code | Error Type | Description |
|------|-----------|-------------|
| **200** | OK | Request successful |
| **201** | Created | Resource created successfully |
| **204** | No Content | Resource deleted successfully |
| **400** | Bad Request | Invalid request parameters or body |
| **401** | Unauthorized | Missing or invalid authentication |
| **403** | Forbidden | Authenticated but not authorized for this action |
| **404** | Not Found | Resource not found |
| **409** | Conflict | Resource already exists or constraint violation |
| **422** | Unprocessable Entity | Validation error |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Internal Server Error | Server error (should be rare) |
| **503** | Service Unavailable | Temporary unavailability (maintenance, overload) |

### Common Error Codes

| Error Code | HTTP Status | Description |
|------------|------------|-------------|
| `invalid_request` | 400 | Malformed request body or parameters |
| `validation_error` | 422 | Request body failed validation |
| `authentication_required` | 401 | No authentication provided |
| `invalid_credentials` | 401 | Invalid email/password |
| `token_expired` | 401 | JWT token has expired |
| `insufficient_permissions` | 403 | User lacks required permissions |
| `resource_not_found` | 404 | Requested resource doesn't exist |
| `duplicate_resource` | 409 | Resource with same identifier already exists |
| `invalid_url` | 422 | URL format is invalid |
| `link_expired` | 410 | Short link has expired |
| `workspace_quota_exceeded` | 429 | Workspace has exceeded link quota |
| `rate_limit_exceeded` | 429 | Too many requests |

---

## Endpoints

## Short Links

### Create Short Link

Creates a new short link or returns an existing one if the URL already exists (deterministic behavior).

```http
POST /api/v1/workspaces/{workspaceId}/links
```

**Path Parameters:**
- `workspaceId` (integer, required) - ID of the workspace

**Request Body:**

```json
{
  "original_url": "https://www.example.com/very/long/url?param=value",
  "custom_code": "my-code",
  "expires_at": "2025-12-31T23:59:59",
  "max_clicks": 1000,
  "tags": ["marketing", "campaign-2024"]
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `original_url` | string | Yes | The URL to shorten. Must be valid HTTP/HTTPS URL |
| `custom_code` | string | No | Custom short code (3-20 alphanumeric, hyphens, underscores). If omitted, code is generated deterministically |
| `expires_at` | datetime | No | ISO 8601 timestamp. Link becomes inactive after this time |
| `max_clicks` | integer | No | Link becomes inactive after this many clicks |
| `tags` | array[string] | No | Tags for categorization and filtering |

**Response:** `201 Created`

```json
{
  "id": 12345,
  "short_code": "MaSgB7xKpQ",
  "short_url": "https://short.ly/MaSgB7xKpQ",
  "original_url": "https://www.example.com/very/long/url?param=value",
  "normalized_url": "https://www.example.com/very/long/url?param=value",
  "created_at": "2025-11-18T10:30:00",
  "expires_at": "2025-12-31T23:59:59",
  "click_count": 0,
  "is_active": true,
  "tags": ["marketing", "campaign-2024"]
}
```

**Examples:**

```bash
# Create link with auto-generated code
curl -X POST https://api.linkforge.io/api/v1/workspaces/1/links \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "original_url": "https://example.com/page"
  }'

# Create link with custom code
curl -X POST https://api.linkforge.io/api/v1/workspaces/1/links \
  -H "X-API-Key: sk_live_abc123..." \
  -H "Content-Type: application/json" \
  -d '{
    "original_url": "https://example.com/promo",
    "custom_code": "black-friday-2025"
  }'

# Create expiring link
curl -X POST https://api.linkforge.io/api/v1/workspaces/1/links \
  -H "X-API-Key: sk_live_abc123..." \
  -H "Content-Type: application/json" \
  -d '{
    "original_url": "https://example.com/sale",
    "expires_at": "2025-12-01T00:00:00",
    "max_clicks": 5000,
    "tags": ["sale", "limited-time"]
  }'
```

**Notes:**
- If the same `original_url` is submitted twice for the same workspace, the existing short link is returned (deterministic reuse)
- `custom_code` must be unique within the workspace
- Normalized URLs are used for matching (e.g., `http://Example.com` and `http://example.com` are treated as identical)

---

### Get Short Link by Code

Retrieves a short link by its short code.

```http
GET /api/v1/workspaces/{workspaceId}/links/{shortCode}
```

**Path Parameters:**
- `workspaceId` (integer, required) - ID of the workspace
- `shortCode` (string, required) - The short code

**Response:** `200 OK`

```json
{
  "id": 12345,
  "short_code": "MaSgB7xKpQ",
  "short_url": "https://short.ly/MaSgB7xKpQ",
  "original_url": "https://www.example.com/very/long/url",
  "normalized_url": "https://www.example.com/very/long/url",
  "created_at": "2025-11-18T10:30:00",
  "expires_at": null,
  "click_count": 142,
  "is_active": true,
  "tags": []
}
```

**Example:**

```bash
curl https://api.linkforge.io/api/v1/workspaces/1/links/MaSgB7xKpQ \
  -H "X-API-Key: sk_live_abc123..."
```

---

### List Short Links

Lists all short links in a workspace with pagination and filtering.

```http
GET /api/v1/workspaces/{workspaceId}/links
```

**Path Parameters:**
- `workspaceId` (integer, required)

**Query Parameters:**
- `page` (integer, default: 0) - Zero-based page number
- `size` (integer, default: 20, max: 100) - Items per page
- `sort` (string, default: `createdAt,desc`) - Sort field and direction
- `tag` (string, optional) - Filter by tag
- `active` (boolean, optional) - Filter by active status
- `search` (string, optional) - Full-text search in originalUrl and tags

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 12345,
      "short_code": "MaSgB7xKpQ",
      "short_url": "https://short.ly/MaSgB7xKpQ",
      "original_url": "https://www.example.com/page1",
      "click_count": 142,
      "created_at": "2025-11-18T10:30:00",
      "is_active": true
    },
    {
      "id": 12346,
      "short_code": "Xy9KmN2qWz",
      "short_url": "https://short.ly/Xy9KmN2qWz",
      "original_url": "https://www.example.com/page2",
      "click_count": 87,
      "created_at": "2025-11-17T15:20:00",
      "is_active": true
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 347,
    "totalPages": 18
  }
}
```

**Example:**

```bash
# List all links
curl https://api.linkforge.io/api/v1/workspaces/1/links \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Filter by tag
curl https://api.linkforge.io/api/v1/workspaces/1/links?tag=marketing \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Search
curl "https://api.linkforge.io/api/v1/workspaces/1/links?search=example.com" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Delete Short Link

Soft deletes a short link. The link becomes inaccessible but data is preserved for analytics.

```http
DELETE /api/v1/workspaces/{workspaceId}/links/{linkId}
```

**Path Parameters:**
- `workspaceId` (integer, required)
- `linkId` (integer, required) - ID of the link (not the short code)

**Response:** `204 No Content`

**Example:**

```bash
curl -X DELETE https://api.linkforge.io/api/v1/workspaces/1/links/12345 \
  -H "X-API-Key: sk_live_abc123..."
```

---

### Redirect (Public Endpoint)

Resolves a short code and redirects to the original URL. This endpoint is public and does not require authentication.

```http
GET /{shortCode}
```

**Path Parameters:**
- `shortCode` (string, required)

**Response:** `302 Found`

```http
HTTP/1.1 302 Found
Location: https://www.example.com/very/long/url
```

**Notes:**
- This endpoint logs a click event for analytics
- If the link is expired or inactive, returns `410 Gone`
- If the link is not found, returns `404 Not Found`
- Redirect is cached at CDN layer for performance

**Example:**

```bash
curl -L https://short.ly/MaSgB7xKpQ
# Automatically follows redirect to original URL
```

---

## Analytics

### Get Link Statistics

Retrieves comprehensive analytics for a short link.

```http
GET /api/v1/workspaces/{workspaceId}/links/{shortCode}/stats
```

**Path Parameters:**
- `workspaceId` (integer, required)
- `shortCode` (string, required)

**Query Parameters:**
- `from` (date, optional) - Start date (ISO 8601, default: 30 days ago)
- `to` (date, optional) - End date (ISO 8601, default: today)

**Response:** `200 OK`

```json
{
  "short_code": "MaSgB7xKpQ",
  "total_clicks": 1523,
  "clicks_by_date": {
    "2025-11-15": 234,
    "2025-11-16": 312,
    "2025-11-17": 289,
    "2025-11-18": 688
  },
  "clicks_by_country": {
    "US": 678,
    "UK": 234,
    "CA": 189,
    "DE": 156,
    "FR": 134,
    "other": 132
  },
  "clicks_by_referrer": {
    "twitter.com": 456,
    "facebook.com": 289,
    "direct": 234,
    "linkedin.com": 178,
    "other": 366
  },
  "clicks_by_device": {
    "mobile": 892,
    "desktop": 534,
    "tablet": 89,
    "bot": 8
  }
}
```

**Example:**

```bash
# Get stats for last 30 days
curl https://api.linkforge.io/api/v1/workspaces/1/links/MaSgB7xKpQ/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Get stats for specific date range
curl "https://api.linkforge.io/api/v1/workspaces/1/links/MaSgB7xKpQ/stats?from=2025-11-01&to=2025-11-30" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Workspace Analytics

Aggregate analytics across all links in a workspace.

```http
GET /api/v1/workspaces/{workspaceId}/analytics
```

**Path Parameters:**
- `workspaceId` (integer, required)

**Query Parameters:**
- `from` (date, optional) - Start date
- `to` (date, optional) - End date
- `group_by` (string, optional) - Grouping dimension: `date`, `link`, `country`, `referrer`

**Response:** `200 OK`

```json
{
  "workspace_id": 1,
  "period": {
    "from": "2025-11-01",
    "to": "2025-11-18"
  },
  "total_clicks": 15234,
  "total_links": 347,
  "active_links": 298,
  "top_links": [
    {
      "short_code": "MaSgB7xKpQ",
      "clicks": 1523,
      "percentage": 10.0
    },
    {
      "short_code": "Xy9KmN2qWz",
      "clicks": 1289,
      "percentage": 8.5
    }
  ],
  "clicks_by_date": {
    "2025-11-15": 834,
    "2025-11-16": 912,
    "2025-11-17": 789,
    "2025-11-18": 1188
  }
}
```

**Example:**

```bash
curl https://api.linkforge.io/api/v1/workspaces/1/analytics \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Workspaces

### Get Workspace

Retrieves workspace details.

```http
GET /api/v1/workspaces/{workspaceId}
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "Acme Corp",
  "slug": "acme-corp",
  "plan": "pro",
  "created_at": "2025-01-15T08:00:00",
  "settings": {
    "custom_domain": "go.acme.com",
    "default_expiration_days": null,
    "analytics_retention_days": 365
  },
  "quota": {
    "links_limit": 10000,
    "links_used": 347,
    "api_calls_limit": 1000,
    "api_calls_used": 234
  }
}
```

---

### Update Workspace Settings

```http
PATCH /api/v1/workspaces/{workspaceId}
```

**Request Body:**

```json
{
  "name": "Acme Corporation",
  "settings": {
    "custom_domain": "go.acme.com",
    "default_expiration_days": 90
  }
}
```

**Response:** `200 OK`

---

## Users

### List Workspace Users

```http
GET /api/v1/workspaces/{workspaceId}/users
```

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "email": "admin@acme.com",
      "full_name": "John Admin",
      "role": "admin",
      "created_at": "2025-01-15T08:00:00",
      "last_login_at": "2025-11-18T09:15:00"
    },
    {
      "id": 2,
      "email": "member@acme.com",
      "full_name": "Jane Member",
      "role": "member",
      "created_at": "2025-02-10T10:30:00",
      "last_login_at": "2025-11-17T14:22:00"
    }
  ]
}
```

---

## API Keys

### Create API Key

```http
POST /api/v1/workspaces/{workspaceId}/api-keys
```

**Request Body:**

```json
{
  "name": "Production API Key",
  "scopes": ["links:read", "links:write", "analytics:read"],
  "expires_at": "2026-12-31T23:59:59"
}
```

**Response:** `201 Created`

```json
{
  "id": 1,
  "name": "Production API Key",
  "key": "sk_live_abc123def456...",
  "key_prefix": "sk_live_abc1",
  "scopes": ["links:read", "links:write", "analytics:read"],
  "created_at": "2025-11-18T10:00:00",
  "expires_at": "2026-12-31T23:59:59"
}
```

**Warning:** The full `key` is only returned once upon creation. Store it securely.

---

### List API Keys

```http
GET /api/v1/workspaces/{workspaceId}/api-keys
```

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "name": "Production API Key",
      "key_prefix": "sk_live_abc1",
      "scopes": ["links:read", "links:write", "analytics:read"],
      "created_at": "2025-11-18T10:00:00",
      "last_used_at": "2025-11-18T14:30:00",
      "expires_at": "2026-12-31T23:59:59"
    }
  ]
}
```

---

### Delete API Key

```http
DELETE /api/v1/workspaces/{workspaceId}/api-keys/{keyId}
```

**Response:** `204 No Content`

---

## Webhooks

### Create Webhook

Subscribe to events (clicks, link created, link deleted).

```http
POST /api/v1/workspaces/{workspaceId}/webhooks
```

**Request Body:**

```json
{
  "url": "https://your-app.com/webhooks/linkforge",
  "events": ["link.clicked", "link.created", "link.deleted"],
  "secret": "whsec_abc123..."
}
```

**Response:** `201 Created`

---

## SDK Examples

### JavaScript / Node.js

```javascript
const LinkforgeClient = require('@linkforge/node');

const client = new LinkforgeClient({
  apiKey: 'sk_live_abc123...'
});

// Create link
const link = await client.links.create(workspaceId, {
  originalUrl: 'https://example.com/page',
  tags: ['marketing']
});

console.log(link.shortUrl); // https://short.ly/MaSgB7xKpQ

// Get stats
const stats = await client.links.getStats(workspaceId, 'MaSgB7xKpQ');
console.log(stats.totalClicks); // 1523
```

### Python

```python
from linkforge import LinkforgeClient

client = LinkforgeClient(api_key='sk_live_abc123...')

# Create link
link = client.links.create(
    workspace_id=1,
    original_url='https://example.com/page',
    tags=['marketing']
)

print(link.short_url)  # https://short.ly/MaSgB7xKpQ

# Get stats
stats = client.links.get_stats(workspace_id=1, short_code='MaSgB7xKpQ')
print(stats.total_clicks)  # 1523
```

---

## Postman Collection

Download the complete Postman collection: [linkforge-api.postman_collection.json](../postman/linkforge-api.postman_collection.json)

---

## OpenAPI Specification

The complete OpenAPI 3.1 specification is available at:

- **JSON:** `GET /v3/api-docs`
- **YAML:** `GET /v3/api-docs.yaml`
- **Interactive UI:** `GET /swagger-ui.html`

---

## Changelog

### v1.0.0 (2025-11-18)

- Initial API release
- Short link CRUD operations
- Analytics endpoints
- Workspace management
- API key authentication

---

**Questions or issues?** Open an issue on [GitHub](https://github.com/yourorg/url-short/issues) or email support@linkforge.io
