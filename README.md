# Linkforge URL Shortener Platform

A production-ready URL shortener with deterministic short codes, workspace isolation, and comprehensive analytics. Built for teams that need predictable, collision-free short links.

## Key Features

- **Deterministic Short Codes**: Same URL in the same workspace always produces the same short code
- **Workspace Isolation**: Each workspace has its own namespace—no collision risks across teams
- **Analytics Built-In**: Track clicks by geography, device, referrer, and time
- **API-First Design**: RESTful API with comprehensive documentation
- **Production Ready**: Built with Spring Boot 3, PostgreSQL, and designed for scale

## Tech Stack

**Backend:**
- Java 21
- Spring Boot 3.4.0
- Spring Data JPA
- Spring Security
- PostgreSQL 15+ (with Flyway migrations)
- Maven

**Planned:**
- Next.js 14 (frontend dashboard)
- Apache Kafka (analytics pipeline)
- Redis (caching layer)

## Architecture Overview

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────┐
│   Spring Boot API       │
│  ┌──────────────────┐   │
│  │  REST Controllers│   │
│  └────────┬─────────┘   │
│           ▼             │
│  ┌──────────────────┐   │
│  │  Service Layer   │   │
│  │  - Deterministic │   │
│  │    Algorithm     │   │
│  │  - URL Canonical.│   │
│  └────────┬─────────┘   │
│           ▼             │
│  ┌──────────────────┐   │
│  │  JPA Repository  │   │
│  └────────┬─────────┘   │
└───────────┼─────────────┘
            ▼
  ┌──────────────────┐
  │   PostgreSQL     │
  │  - Workspaces    │
  │  - Short Links   │
  │  - Click Events  │
  └──────────────────┘
```

## Quick Start with Docker Compose

The fastest way to run the entire stack:

```bash
# Clone the repository
git clone https://github.com/yourorg/url-short.git
cd url-short

# Start all services (PostgreSQL + Spring Boot)
docker-compose up -d

# Check logs
docker-compose logs -f

# Access the API
curl http://localhost:8080/actuator/health
```

The API will be available at `http://localhost:8080`.

## Development Setup (Local)

### Prerequisites

- Java 21 (OpenJDK or Oracle JDK)
- Maven 3.8+
- PostgreSQL 15+
- Git

### Database Setup

1. Install PostgreSQL:
   ```bash
   # macOS
   brew install postgresql@15
   brew services start postgresql@15

   # Ubuntu/Debian
   sudo apt install postgresql-15
   sudo systemctl start postgresql
   ```

2. Create database:
   ```bash
   createdb urlshort
   ```

3. Configure connection (optional - defaults work for local):
   ```bash
   # Edit src/main/resources/application.properties if needed
   spring.datasource.url=jdbc:postgresql://localhost:5432/urlshort
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   ```

### Run the Application

```bash
# Build the project
mvn clean install

# Run tests
mvn test

# Start the application
mvn spring-boot:run
```

The API will start on `http://localhost:8080`.

Flyway migrations will run automatically on startup, creating all database tables.

### Verify Setup

```bash
# Health check
curl http://localhost:8080/actuator/health

# API documentation (Swagger UI)
open http://localhost:8080/swagger-ui.html
```

## API Quick Reference

**Create a short link:**
```bash
POST /api/v1/workspaces/{workspaceId}/links
Content-Type: application/json

{
  "originalUrl": "https://example.com/very/long/url?param=value",
  "expiresAt": "2025-12-31T23:59:59",
  "tags": ["campaign", "q4-2025"]
}
```

**Response:**
```json
{
  "id": 123,
  "shortCode": "MaSgB7xKpQ",
  "shortUrl": "http://localhost:8080/MaSgB7xKpQ",
  "originalUrl": "https://example.com/very/long/url?param=value",
  "normalizedUrl": "https://example.com/very/long/url?param=value",
  "clickCount": 0,
  "createdAt": "2025-11-18T10:30:00",
  "isActive": true
}
```

**Redirect (GET):**
```bash
GET /{shortCode}
# Returns 302 redirect to original URL
```

**Get link stats:**
```bash
GET /api/v1/workspaces/{workspaceId}/links/{shortCode}/stats
```

Full API documentation: [docs/API.md](docs/API.md)

## Documentation

- [API Documentation](docs/API.md) - Complete REST API reference
- [Architecture Guide](docs/ARCHITECTURE.md) - System design and data flow
- [Algorithm Specification](docs/ALGORITHM.md) - Deterministic URL shortening algorithm
- [Deployment Guide](docs/DEPLOYMENT.md) - Production deployment instructions
- [Frontend Guide](docs/FRONTEND.md) - Frontend architecture (planned)
- [Contributing Guidelines](CONTRIBUTING.md) - How to contribute

## Key Concepts

### Deterministic Short Codes

The same URL in the same workspace always produces the same short code:

```bash
# First call
POST /api/v1/workspaces/1/links {"originalUrl": "https://example.com"}
# Returns: {"shortCode": "Xy9KmN2qWz"}

# Second call with same URL
POST /api/v1/workspaces/1/links {"originalUrl": "https://example.com"}
# Returns: {"shortCode": "Xy9KmN2qWz"}  # Same code!
```

This prevents duplicate short links and enables predictable infrastructure.

### Workspace Isolation

Different workspaces can have different short codes for the same URL:

```bash
# Workspace 1
POST /api/v1/workspaces/1/links {"originalUrl": "https://example.com"}
# Returns: {"shortCode": "abc123"}

# Workspace 2
POST /api/v1/workspaces/2/links {"originalUrl": "https://example.com"}
# Returns: {"shortCode": "xyz789"}  # Different code!
```

### URL Canonicalization

URLs are normalized before hashing to ensure consistency:

- `HTTP://Example.com/Path` → `http://example.com/Path`
- `https://example.com:443/` → `https://example.com/`
- `http://example.com?z=1&a=2` → `http://example.com?a=2&z=1`

## Configuration

Key application properties (see `src/main/resources/application.properties`):

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/urlshort
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway migrations
spring.flyway.enabled=true

# Short URL base
app.short-url.base-url=http://localhost:8080

# Logging
logging.level.com.urlshort=DEBUG
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ShortLinkServiceTest

# Run with coverage
mvn test jacoco:report
```

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for:

- Code style guidelines
- Commit message format
- Pull request process
- Testing requirements

## Limitations & Trade-offs

This project makes deliberate trade-offs:

- **Collision handling is probabilistic**: With Base58 10-character codes, collisions are extremely rare (< 0.01% for 1M URLs per workspace), but the retry mechanism handles them deterministically.
- **Short codes are not sequential**: Codes are generated via hash, not incremented counters, which means you can't predict the next code.
- **URL editing is not supported**: Once created, a short link's destination URL cannot be changed (prevents cache poisoning and maintains analytics integrity).
- **Soft deletes only**: Deleted links remain in the database to preserve analytics history.

## Performance Characteristics

Based on load testing with 10k URLs:

- **Link creation**: ~50-150ms (includes DB write)
- **Link lookup**: ~5-15ms (cached), ~20-50ms (uncached)
- **Redirect response**: <30ms (p50), <65ms (p95)
- **Analytics queries**: ~100-500ms (depending on date range)

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Support

- GitHub Issues: [https://github.com/yourorg/url-short/issues](https://github.com/yourorg/url-short/issues)
- Documentation: [docs/](docs/)
- API Reference: [docs/API.md](docs/API.md)

---

Built with care by the Linkforge team. Questions? Open an issue.
