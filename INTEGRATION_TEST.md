# Integration Testing Guide

This document provides comprehensive integration testing scenarios for the Linkforge URL Shortener platform.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Test Scenarios](#test-scenarios)
4. [Common Issues and Solutions](#common-issues-and-solutions)
5. [Smoke Tests](#smoke-tests)
6. [API Testing with cURL](#api-testing-with-curl)

---

## Prerequisites

Before running integration tests, ensure you have:

- Docker and Docker Compose installed
- `.env` file configured (copy from `.env.example`)
- Ports 3000, 5432, 6379, 8080, 9092 available
- At least 4GB of available RAM

## Quick Start

### 1. Start the Full Stack

```bash
# Production mode (optimized builds)
docker-compose up -d

# Development mode (hot reload enabled)
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend
docker-compose logs -f frontend
```

### 2. Verify All Services are Healthy

```bash
# Check service status
docker-compose ps

# Expected output:
# NAME                      STATUS
# urlshortener-backend      Up (healthy)
# urlshortener-frontend     Up (healthy)
# urlshortener-postgres     Up (healthy)
# urlshortener-redis        Up (healthy)
# urlshortener-kafka        Up (healthy)
# urlshortener-zookeeper    Up (healthy)
```

### 3. Access the Applications

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api/v1
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

---

## Test Scenarios

### Scenario 1: User Signup Flow

**Objective**: Verify new users can create an account and workspace.

#### Steps:

1. **Open Frontend**
   ```
   Navigate to: http://localhost:3000
   ```

2. **Click "Sign Up" or "Get Started"**

3. **Fill in Registration Form**
   ```
   Email: test@example.com
   Password: Test123456!
   Full Name: Test User
   Workspace Name: Test Workspace
   Workspace Slug: test-workspace
   ```

4. **Submit Form**

#### Expected Results:

✅ User is redirected to dashboard
✅ JWT token is stored in browser (check localStorage: `auth_token`)
✅ User information is displayed in the UI
✅ Workspace is created and accessible

#### API Verification:

```bash
# Test signup endpoint directly
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123456!",
    "fullName": "Test User",
    "workspaceName": "Test Workspace",
    "workspaceSlug": "test-workspace"
  }'

# Expected response:
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "email": "test@example.com",
      "fullName": "Test User",
      "role": "ADMIN",
      "workspaceId": 1,
      "workspaceName": "Test Workspace",
      "workspaceSlug": "test-workspace"
    }
  },
  "message": "User created successfully"
}
```

#### Troubleshooting:

- **409 Conflict**: Email or workspace slug already exists - use different values
- **400 Bad Request**: Check that all required fields are provided
- **500 Internal Server Error**: Check backend logs: `docker-compose logs backend`

---

### Scenario 2: User Login Flow

**Objective**: Verify existing users can authenticate.

#### Steps:

1. **Navigate to Login Page**
   ```
   http://localhost:3000/login
   ```

2. **Enter Credentials**
   ```
   Email: test@example.com
   Password: Test123456!
   ```

3. **Click "Login"**

#### Expected Results:

✅ User is authenticated and redirected to dashboard
✅ JWT token is stored in localStorage
✅ User can access protected pages

#### API Verification:

```bash
# Test login endpoint
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123456!"
  }'

# Expected response:
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": { ... }
  }
}
```

---

### Scenario 3: Create Short Link Flow

**Objective**: Verify authenticated users can create short links.

#### Steps:

1. **Login to Dashboard**

2. **Click "Create Link" or "New Link"**

3. **Enter URL**
   ```
   Original URL: https://www.example.com/very/long/url/to/shorten
   Tags: marketing, campaign-2024 (optional)
   Expires At: 2025-12-31 (optional)
   ```

4. **Click "Create" or "Shorten"**

#### Expected Results:

✅ Short link is created
✅ Short code is displayed (e.g., "abc123")
✅ Full short URL is shown (e.g., "http://localhost:8080/abc123")
✅ Link appears in links list
✅ Copy button works

#### API Verification:

```bash
# Get the token from previous login
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Create short link
curl -X POST http://localhost:8080/api/v1/links \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "originalUrl": "https://www.example.com/very/long/url",
    "tags": ["marketing", "campaign-2024"],
    "expiresAt": "2025-12-31T23:59:59"
  }'

# Expected response:
{
  "success": true,
  "data": {
    "id": 1,
    "short_code": "abc123",
    "short_url": "http://localhost:8080/abc123",
    "original_url": "https://www.example.com/very/long/url",
    "normalized_url": "https://www.example.com/very/long/url",
    "created_at": "2025-11-18T10:30:00",
    "expires_at": "2025-12-31T23:59:59",
    "click_count": 0,
    "is_active": true,
    "tags": ["marketing", "campaign-2024"]
  },
  "message": "Short link created successfully"
}
```

---

### Scenario 4: Redirect Flow (Public Access)

**Objective**: Verify short links redirect to original URLs without authentication.

#### Steps:

1. **Get a Short Code**
   - Use the short code from Scenario 3 (e.g., "abc123")

2. **Open in Browser (Incognito/Private Mode)**
   ```
   http://localhost:8080/abc123
   ```

3. **Observe Redirect**

#### Expected Results:

✅ Browser immediately redirects to original URL
✅ HTTP 302 redirect response
✅ No authentication required
✅ Click is recorded for analytics

#### API Verification:

```bash
# Test redirect (should return 302 with Location header)
curl -v http://localhost:8080/abc123

# Expected response headers:
# HTTP/1.1 302 Found
# Location: https://www.example.com/very/long/url
# Cache-Control: no-cache, no-store, must-revalidate
```

#### Testing Error Cases:

```bash
# Non-existent short code (should return 404)
curl -v http://localhost:8080/nonexistent

# Expected: 404 Not Found

# Expired link (if you set expiration in the past)
# Expected: 410 Gone
```

---

### Scenario 5: View Analytics Flow

**Objective**: Verify users can view click statistics for their links.

#### Steps:

1. **Login to Dashboard**

2. **Click on a Link** to view details

3. **Navigate to Analytics/Stats Tab**

4. **Verify Data Display**

#### Expected Results:

✅ Total clicks displayed
✅ Clicks by date chart shown
✅ Clicks by country (if geoIP is configured)
✅ Clicks by referrer
✅ Clicks by device type

#### API Verification:

```bash
TOKEN="your-jwt-token"
LINK_ID=1

# Get link analytics
curl http://localhost:8080/api/v1/links/$LINK_ID/stats \
  -H "Authorization: Bearer $TOKEN"

# Expected response:
{
  "success": true,
  "data": {
    "short_code": "abc123",
    "total_clicks": 5,
    "clicks_by_date": {
      "2025-11-18": 3,
      "2025-11-17": 2
    },
    "clicks_by_country": {
      "US": 3,
      "GB": 2
    },
    "clicks_by_referrer": {
      "direct": 4,
      "google.com": 1
    },
    "clicks_by_device": {
      "mobile": 3,
      "desktop": 2
    }
  }
}
```

---

### Scenario 6: Bulk Link Creation

**Objective**: Verify users can create multiple links at once.

#### API Test:

```bash
TOKEN="your-jwt-token"

# Bulk create links
curl -X POST http://localhost:8080/api/v1/links/bulk \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "urls": [
      "https://example.com/page1",
      "https://example.com/page2",
      "https://example.com/page3"
    ]
  }'

# Expected response:
{
  "success": true,
  "data": [
    {
      "id": 2,
      "short_code": "def456",
      "original_url": "https://example.com/page1",
      ...
    },
    {
      "id": 3,
      "short_code": "ghi789",
      "original_url": "https://example.com/page2",
      ...
    },
    {
      "id": 4,
      "short_code": "jkl012",
      "original_url": "https://example.com/page3",
      ...
    }
  ],
  "message": "3 short links created successfully"
}
```

---

### Scenario 7: Workspace Management

**Objective**: Verify workspace settings can be viewed and updated.

#### API Tests:

```bash
TOKEN="your-jwt-token"

# Get current workspace
curl http://localhost:8080/api/v1/workspaces/current \
  -H "Authorization: Bearer $TOKEN"

# Update workspace settings (requires ADMIN role)
curl -X PATCH http://localhost:8080/api/v1/workspaces/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Updated Workspace Name",
    "settings": {
      "default_expiration_days": 30,
      "custom_domain": "links.example.com"
    }
  }'

# List workspace members
curl http://localhost:8080/api/v1/workspaces/1/members \
  -H "Authorization: Bearer $TOKEN"
```

---

## Smoke Tests

Quick tests to verify the system is working:

```bash
#!/bin/bash

echo "=== Linkforge Integration Smoke Tests ==="

# Test 1: Backend Health
echo "1. Testing Backend Health..."
curl -f http://localhost:8080/actuator/health || echo "❌ Backend health check failed"
echo "✅ Backend is healthy"

# Test 2: Frontend Health
echo "2. Testing Frontend Health..."
curl -f http://localhost:3000/api/health || echo "❌ Frontend health check failed"
echo "✅ Frontend is healthy"

# Test 3: Database Connection
echo "3. Testing Database Connection..."
docker-compose exec -T postgres pg_isready || echo "❌ Database not ready"
echo "✅ Database is ready"

# Test 4: Redis Connection
echo "4. Testing Redis Connection..."
docker-compose exec -T redis redis-cli ping || echo "❌ Redis not responding"
echo "✅ Redis is responding"

# Test 5: API Documentation
echo "5. Testing API Documentation..."
curl -f http://localhost:8080/swagger-ui.html > /dev/null || echo "❌ Swagger UI not accessible"
echo "✅ Swagger UI is accessible"

echo "=== All smoke tests passed! ==="
```

---

## Common Issues and Solutions

### Issue 1: Frontend Cannot Connect to Backend

**Symptoms:**
- Network errors in browser console
- "Failed to fetch" errors
- CORS errors

**Solutions:**

1. **Check Backend is Running:**
   ```bash
   docker-compose ps backend
   curl http://localhost:8080/actuator/health
   ```

2. **Verify CORS Configuration:**
   - Check backend logs for CORS-related errors
   - Ensure `http://localhost:3000` is in allowed origins

3. **Check API URL Configuration:**
   ```bash
   # In frontend container
   docker-compose exec frontend printenv | grep API_URL

   # Should show:
   # NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
   ```

### Issue 2: JWT Token Not Working

**Symptoms:**
- 401 Unauthorized errors
- "Invalid token" messages

**Solutions:**

1. **Check Token Format:**
   ```javascript
   // In browser console
   localStorage.getItem('auth_token')
   // Should start with "eyJ..."
   ```

2. **Verify JWT Secret Matches:**
   ```bash
   # Check backend JWT secret
   docker-compose exec backend printenv | grep JWT_SECRET
   ```

3. **Check Token Expiration:**
   - Default expiration is 24 hours
   - Try logging in again to get fresh token

### Issue 3: Database Connection Errors

**Symptoms:**
- Backend fails to start
- "Connection refused" errors
- "Could not connect to database" in logs

**Solutions:**

1. **Wait for Database to be Ready:**
   ```bash
   docker-compose up -d postgres
   # Wait 10 seconds
   docker-compose up backend
   ```

2. **Check Database Logs:**
   ```bash
   docker-compose logs postgres
   ```

3. **Verify Database Credentials:**
   ```bash
   docker-compose exec postgres psql -U postgres -d urlshortener_dev -c "\dt"
   ```

### Issue 4: Port Conflicts

**Symptoms:**
- "Port already in use" errors
- Services fail to start

**Solutions:**

1. **Check Which Process is Using the Port:**
   ```bash
   # Linux/Mac
   lsof -i :8080
   lsof -i :3000

   # Windows
   netstat -ano | findstr :8080
   ```

2. **Change Ports in .env:**
   ```bash
   BACKEND_PORT=8081
   FRONTEND_PORT=3001
   ```

### Issue 5: Slow Performance

**Symptoms:**
- Slow API responses
- High memory usage

**Solutions:**

1. **Check Resource Usage:**
   ```bash
   docker stats
   ```

2. **Increase Docker Resources:**
   - Docker Desktop: Settings → Resources → Increase Memory/CPU

3. **Check Redis Cache:**
   ```bash
   docker-compose exec redis redis-cli INFO stats
   ```

---

## Testing Checklist

Use this checklist to verify full integration:

- [ ] All services start without errors
- [ ] All health checks pass
- [ ] User can sign up with new account
- [ ] User can login with existing account
- [ ] JWT token is stored and used correctly
- [ ] User can create short link
- [ ] Short link redirects to original URL
- [ ] Click is recorded (check analytics)
- [ ] User can view link list
- [ ] User can view link analytics
- [ ] User can update link settings
- [ ] User can delete link
- [ ] Bulk link creation works
- [ ] Workspace settings can be viewed
- [ ] Workspace settings can be updated (ADMIN only)
- [ ] Expired links return 410 Gone
- [ ] Invalid short codes return 404
- [ ] Unauthorized requests return 401
- [ ] CORS headers are present
- [ ] API documentation is accessible

---

## Performance Testing

### Load Test with Apache Bench

```bash
# Test redirect performance
ab -n 1000 -c 10 http://localhost:8080/abc123

# Test API performance (with auth)
ab -n 100 -c 5 -H "Authorization: Bearer YOUR_TOKEN" \
   http://localhost:8080/api/v1/links
```

### Expected Performance Metrics

- **Redirect Response Time**: < 50ms (p95)
- **API Response Time**: < 200ms (p95)
- **Throughput**: > 1000 redirects/second
- **Cache Hit Rate**: > 80%

---

## Monitoring Integration

### View Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Application metrics
curl http://localhost:8080/actuator/metrics

# Check Redis cache stats
docker-compose exec redis redis-cli INFO stats
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service with timestamps
docker-compose logs -f --timestamps backend

# Last 100 lines
docker-compose logs --tail=100 frontend
```

---

## Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (CAUTION: deletes all data)
docker-compose down -v

# Remove images
docker-compose down --rmi all

# Full cleanup
docker-compose down -v --rmi all --remove-orphans
```

---

## Next Steps

After successful integration testing:

1. **Set up CI/CD Pipeline** - Automate testing with GitHub Actions
2. **Configure Production Environment** - Update .env for production
3. **Set up Monitoring** - Configure Prometheus/Grafana
4. **Performance Testing** - Run load tests
5. **Security Audit** - Review security configurations
6. **Documentation** - Update API documentation

---

## Support

If you encounter issues not covered here:

1. Check the logs: `docker-compose logs`
2. Review the [INTEGRATION_STATUS.md](./INTEGRATION_STATUS.md) report
3. Consult the backend/frontend implementation documentation
4. Check the GitHub issues page
