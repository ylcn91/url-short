# URLShort

Full-stack URL shortener platform built with Spring Boot 3.4 and Next.js 14.

[![Backend CI](https://github.com/ylcn91/url-short/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/ylcn91/url-short/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/ylcn91/url-short/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/ylcn91/url-short/actions/workflows/frontend-ci.yml)
[![Deploy Pages](https://github.com/ylcn91/url-short/actions/workflows/deploy-github-pages.yml/badge.svg)](https://github.com/ylcn91/url-short/actions/workflows/deploy-github-pages.yml)

**Live Demo:** [ylcn91.github.io/url-short](https://ylcn91.github.io/url-short/)

## Features

- **Deterministic Short Codes** — Same URL always produces the same short code within a workspace
- **Advanced Analytics** — Click tracking with device, browser, location, and referrer data
- **Custom Domains** — Branded short links with DNS verification
- **Password Protection** — Secure links with password gates
- **A/B Testing Variants** — Split traffic across multiple destinations with weighted routing
- **Link Health Monitoring** — Automatic dead link detection
- **Webhooks** — Real-time notifications on link events
- **Multi-Tenant Workspaces** — Team collaboration with role-based access (Admin, Member, Viewer)
- **Bulk Operations** — Create multiple short links at once
- **Rate Limiting** — Configurable per-endpoint rate limits with Bucket4j + Redis

## Tech Stack

### Backend

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 LTS | Language |
| Spring Boot | 3.4.0 | Framework |
| PostgreSQL | 15+ | Primary database |
| Redis | 7+ | Caching & rate limiting |
| Apache Kafka | 3.x | Click event streaming |
| Flyway | — | Database migrations |
| JWT (jjwt) | 0.12.5 | Authentication |
| Bucket4j | 8.10.1 | Rate limiting |
| Testcontainers | 1.20.4 | Integration testing |
| ArchUnit | 1.3.0 | Architecture testing |

### Frontend

| Technology | Version | Purpose |
|-----------|---------|---------|
| Next.js | 14.2 | React framework |
| TypeScript | 5.3 | Type safety |
| Tailwind CSS | 3.4 | Styling |
| shadcn/ui + Radix | — | Component library |
| Zustand | 4.5 | State management |
| TanStack Query | 5.28 | Server state |
| Recharts | 2.12 | Analytics charts |

## Quick Start

### Prerequisites

- Java 21+
- Node.js 20+
- PostgreSQL 15+
- Redis 7+
- Kafka (optional)

### With Docker Compose

```bash
docker compose up -d
```

This starts PostgreSQL, Redis, Zookeeper, Kafka, backend (`:8080`), and frontend (`:3000`).

### Manual Setup

```bash
# Backend
./mvnw spring-boot:run

# Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

Backend: `http://localhost:8080` | Frontend: `http://localhost:3000`

## API Overview

All endpoints under `/api/v1/`. Full interactive docs at [/docs](https://ylcn91.github.io/url-short/docs).

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/auth/login` | POST | Authenticate and get JWT tokens |
| `/auth/signup` | POST | Create account + workspace |
| `/auth/refresh` | POST | Refresh access token |
| `/auth/me` | GET | Current user profile |
| `/links` | POST | Create short link |
| `/links` | GET | List links (paginated) |
| `/links/{id}/stats` | GET | Link analytics |
| `/links/bulk` | POST | Bulk create links |
| `/domains` | POST | Register custom domain |
| `/links/{id}/password` | POST | Add password protection |
| `/links/{id}/variants` | POST | Create A/B test variant |
| `/links/{id}/health/check` | POST | Check link health |
| `/webhooks` | POST | Register webhook |
| `/workspaces/current` | GET | Current workspace |
| `/{code}` | GET | Redirect (public) |

## Project Structure

```
url-short/
├── src/main/java/com/urlshort/
│   ├── config/          # Spring configuration (Cache, Kafka, Redis, Security)
│   ├── controller/      # REST controllers (9 controllers)
│   ├── domain/          # JPA entities (11 entities + 2 enums)
│   ├── dto/             # DTOs organized by domain
│   │   ├── auth/        # Login, Signup, Auth, User responses
│   │   ├── link/        # Create, Update, ShortLink, Stats
│   │   ├── domain/      # CustomDomain request/response
│   │   ├── password/    # Password protection DTOs
│   │   ├── variant/     # A/B testing DTOs
│   │   ├── webhook/     # Webhook DTOs
│   │   ├── workspace/   # Workspace & member DTOs
│   │   └── common/      # ApiResponse, ErrorResponse
│   ├── event/           # Kafka producer & consumer
│   ├── exception/       # Custom exception hierarchy
│   ├── repository/      # Spring Data JPA repositories
│   ├── security/        # JWT filter, UserDetails, SecurityConfig
│   ├── service/         # Business logic (10 services)
│   └── util/            # Base58, ShortCodeGenerator, URL canonicalizer
├── src/test/java/       # Unit, integration, architecture tests
├── frontend/            # Next.js 14 application
│   ├── src/app/         # App Router pages
│   ├── src/components/  # UI components (shadcn/ui)
│   ├── src/lib/         # Auth client, types, utils
│   └── src/stores/      # Zustand stores
├── docs/                # Project documentation
├── docker-compose.yml   # Full stack orchestration
└── .github/workflows/   # CI/CD pipelines
```

## Testing

```bash
# Run all tests (149 tests)
./mvnw test

# Run specific test
./mvnw test -Dtest=ShortLinkServiceTest

# Frontend
cd frontend && npm run lint && npm run type-check
```

## Documentation

| Document | Description |
|----------|-------------|
| [API Reference](https://ylcn91.github.io/url-short/docs) | Interactive API documentation |
| [Architecture](docs/ARCHITECTURE.md) | System design and patterns |
| [Database Schema](docs/DATABASE_SCHEMA.md) | Entity relationships and indexes |
| [Algorithm](docs/ALGORITHM.md) | Deterministic short code generation |
| [Kafka Decision](docs/KAFKA_DECISION.md) | Event streaming architecture |
| [Deployment](docs/DEPLOYMENT.md) | Production deployment guide |
| [Local Setup](docs/LOCAL_SETUP.md) | Development environment |
| [Contributing](https://ylcn91.github.io/url-short/CONTRIBUTING.md) | Contribution guidelines |
| [Frontend](https://ylcn91.github.io/url-short/docs/FRONTEND.md) | Frontend documentation |

## License

MIT
