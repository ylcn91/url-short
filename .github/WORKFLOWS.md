# GitHub Actions CI/CD Workflows

This document describes the GitHub Actions workflows configured for the URL Shortener project.

## Workflows Overview

### 1. Backend CI Workflow (`backend-ci.yml`)

**Trigger Events:**
- Push to `main` branch with changes in `src/**`, `pom.xml`, or workflow file
- Pull requests to `main` branch with changes in `src/**`, `pom.xml`, or workflow file

**Jobs (Sequential):**

#### Job: Test
- **Purpose:** Compile code and run JUnit tests
- **Steps:**
  1. Checkout code
  2. Setup Java 21 (Temurin distribution)
  3. Cache Maven dependencies (automatic)
  4. Run `mvn verify` (skips integration tests)
  5. Upload test results as artifacts
  6. Publish test results in GitHub UI

#### Job: Build (depends on: test)
- **Purpose:** Package the application into a JAR file
- **Steps:**
  1. Checkout code
  2. Setup Java 21
  3. Cache Maven dependencies
  4. Run `mvn clean package -DskipTests`
  5. Upload JAR artifact (30-day retention)

#### Job: Docker (depends on: build, main branch only)
- **Purpose:** Build and push Docker image to GitHub Container Registry
- **Steps:**
  1. Checkout code
  2. Setup Docker Buildx for multi-platform builds
  3. Login to GitHub Container Registry
  4. Extract image metadata (tags, labels)
  5. Build and push Docker image with:
     - Tags: branch, semver, SHA, and latest (for main)
     - GitHub Actions cache layer for faster builds

**Environment Variables:**
- Java version: 21
- Maven cache: Enabled
- Docker registry: ghcr.io

**Artifacts:**
- `test-results/`: JUnit test reports
- `backend-jar/`: Compiled JAR file

---

### 2. Frontend CI Workflow (`frontend-ci.yml`)

**Trigger Events:**
- Push to `main` branch with changes in `frontend/**` or workflow file
- Pull requests to `main` branch with changes in `frontend/**` or workflow file

**Jobs (Parallel where possible):**

#### Job: Lint
- **Purpose:** Run ESLint to check code quality
- **Steps:**
  1. Checkout code
  2. Setup Node.js 20
  3. Cache npm dependencies
  4. Install dependencies with `npm ci`
  5. Run `npm run lint`

#### Job: Test (parallel with Lint)
- **Purpose:** Run Jest unit tests with coverage reports
- **Steps:**
  1. Checkout code
  2. Setup Node.js 20
  3. Cache npm dependencies
  4. Install dependencies with `npm ci`
  5. Run `npm test -- --coverage --watchAll=false`
  6. Upload coverage reports as artifacts
  7. Upload coverage to Codecov for tracking

#### Job: Build (depends on: lint, test)
- **Purpose:** Build Next.js production bundle
- **Steps:**
  1. Checkout code
  2. Setup Node.js 20
  3. Cache npm dependencies
  4. Install dependencies with `npm ci`
  5. Run `npm run build`
  6. Upload build artifacts (.next/ and public/)

#### Job: Docker (depends on: build, main branch only)
- **Purpose:** Build and push Docker image to GitHub Container Registry
- **Steps:**
  1. Checkout code
  2. Setup Docker Buildx
  3. Login to GitHub Container Registry
  4. Extract image metadata
  5. Build and push Docker image with caching

**Environment Variables:**
- Node.js version: 20
- npm cache: Enabled
- Docker registry: ghcr.io

**Artifacts:**
- `frontend-coverage/`: Jest coverage reports
- `frontend-build/`: Next.js build output

---

### 3. Integration Tests Workflow (`integration-test.yml`)

**Trigger Events:**
- Push to `main` branch with changes in `src/**`, `frontend/**`, docker-compose files, or workflow file
- Pull requests to `main` branch with similar path changes

**Jobs:**

#### Job: Integration
- **Purpose:** Run E2E tests with full stack in Docker
- **Services:**
  1. **PostgreSQL 16** (Alpine)
     - Database for backend
     - Health checks enabled
     - Ports: 5432
  2. **Redis 7** (Alpine)
     - Caching service
     - Health checks enabled
     - Ports: 6379

- **Steps:**
  1. Checkout code
  2. Setup Java 21
  3. Setup Node.js 20
  4. Start docker-compose.test.yml services
  5. Wait for services to be ready (timeout: 60s)
  6. Install frontend dependencies
  7. Run E2E tests with: `npm run test:e2e`
  8. Run backend integration tests with: `mvn verify -Dgroups=integration`
  9. Upload test results as artifacts
  10. Stop and cleanup Docker services
  11. Publish test results

**Environment Variables:**
- `SPRING_DATASOURCE_URL`: jdbc:postgresql://localhost:5432/url_shortener_test
- `SPRING_DATASOURCE_USERNAME`: postgres
- `SPRING_DATASOURCE_PASSWORD`: postgres
- `NEXT_PUBLIC_API_URL`: http://localhost:8080

**Artifacts:**
- `integration-test-results/`: Backend test reports and frontend test results

---

## Dependabot Configuration

Dependabot is configured in `.github/dependabot.yml` to automatically check for dependency updates.

### Package Ecosystems Monitored

#### 1. Maven (Backend)
- **Schedule:** Weekly (Monday, 3:00 AM UTC)
- **Scope:** Direct and indirect dependencies
- **PR Limit:** 10 open PRs maximum
- **Labels:** `dependencies`, `maven`
- **Commit prefix:** `chore`

#### 2. npm (Frontend)
- **Directory:** `/frontend`
- **Schedule:** Weekly (Monday, 3:30 AM UTC)
- **Scope:** Direct and indirect dependencies
- **PR Limit:** 10 open PRs maximum
- **Labels:** `dependencies`, `npm`, `frontend`
- **Commit prefix:** `chore`

#### 3. GitHub Actions
- **Schedule:** Weekly (Tuesday, 3:00 AM UTC)
- **PR Limit:** 5 open PRs maximum
- **Labels:** `dependencies`, `github-actions`
- **Commit prefix:** `ci`

#### 4. Docker
- **Schedule:** Monthly (1st of month, 4:00 AM UTC)
- **Labels:** `dependencies`, `docker`
- **Commit prefix:** `chore`

### Dependabot Features
- Auto-rebase strategy for PRs
- Automatic reviewer/assignee assignment to "owner"
- Descriptive commit messages with scope
- Branch naming convention: `dependabot/<ecosystem>/<dependency>`

---

## Docker Configuration

### Dockerfiles

#### `Dockerfile.backend`
Multi-stage build for Spring Boot application:
- **Stage 1 (Builder):** Compile with Maven on JDK 21
- **Stage 2 (Runtime):** Minimal JRE 21 Alpine image
- **Non-root user:** appuser (UID 1000)
- **Health check:** Checks `/actuator/health` endpoint
- **Entrypoint:** Java with container optimizations

#### `frontend/Dockerfile`
Multi-stage build for Next.js application:
- **Stage 1 (Dependencies):** Install production dependencies only
- **Stage 2 (Builder):** Build Next.js bundle
- **Stage 3 (Runner):** Lightweight runtime with only necessary files
- **Non-root user:** nextjs (UID 1000)
- **Health check:** HTTP GET to `/api/health`
- **Optimization:** Uses standalone mode for minimal image size

### Docker Compose

#### `docker-compose.yml` (Production/Development)
Services:
- **PostgreSQL 16:** Database backend
- **Redis 7:** Caching layer
- **Backend:** Spring Boot application
- **Frontend:** Next.js application

All services include:
- Health checks with retry logic
- Persistent volumes for data
- Custom network: `url-shortener-network`
- Environment variable configuration via `.env` file

#### `docker-compose.test.yml` (Testing)
Lightweight version for CI/CD with:
- PostgreSQL with test database
- Redis for caching
- Built backend and frontend services
- Container health checks and dependencies
- Test-specific configurations

---

## Environment Variables

### Required Variables (.env file)

```
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=url_shortener
POSTGRES_PORT=5432
REDIS_PORT=6379
SPRING_PROFILES_ACTIVE=prod
BACKEND_PORT=8080
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
NEXT_PUBLIC_API_URL=http://localhost:8080
FRONTEND_PORT=3000
```

See `.env.example` for full reference.

---

## GitHub Secrets Configuration

The following secrets should be configured in your GitHub repository settings:

### Automatically Available
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions (for GHCR login)

### Optional (if needed)
- `DOCKER_USERNAME`: Docker Hub username (if pushing to Docker Hub)
- `DOCKER_PASSWORD`: Docker Hub password (if pushing to Docker Hub)
- `CODECOV_TOKEN`: Codecov token for coverage tracking

---

## Concurrency Control

All workflows use concurrency groups to:
- Cancel previous runs when new commits are pushed
- Prevent duplicate workflow executions
- Ensure only latest version runs

**Format:** `{workflow-name}-${{ github.ref }}`

---

## Caching Strategy

### Maven Cache
- **Key:** Based on pom.xml
- **Scope:** Entire ~/.m2 directory
- **Benefit:** Faster builds by reusing dependencies

### npm Cache
- **Key:** Based on package-lock.json
- **Scope:** node_modules and npm cache
- **Benefit:** Faster frontend builds

### Docker Cache
- **Type:** GitHub Actions cache layer
- **Mode:** Read and write
- **Benefit:** Faster image builds on rebuilds

---

## Status Badges

Add these badges to your README.md:

```markdown
## CI/CD Status

[![Backend CI](https://github.com/USERNAME/url-short/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/USERNAME/url-short/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/USERNAME/url-short/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/USERNAME/url-short/actions/workflows/frontend-ci.yml)
[![Integration Tests](https://github.com/USERNAME/url-short/actions/workflows/integration-test.yml/badge.svg)](https://github.com/USERNAME/url-short/actions/workflows/integration-test.yml)
```

---

## Artifact Retention

- **Backend JAR:** 30 days
- **Frontend Build:** 30 days
- **Test Results:** Unlimited (published in GitHub UI)
- **Coverage Reports:** Uploaded to Codecov for tracking

---

## Troubleshooting

### Common Issues

**1. Docker Build Fails in CI**
- Ensure `Dockerfile.backend` and `frontend/Dockerfile` exist
- Check Docker syntax is valid
- Verify base images are accessible

**2. Tests Fail in Integration Test**
- Check docker-compose.test.yml is valid
- Verify test database credentials match environment variables
- Check application startup logs in workflow output

**3. Dependabot PRs Not Created**
- Verify repository has Dependabot enabled in settings
- Check branch protection rules don't conflict with Dependabot
- Review Dependabot logs in repository settings

**4. Maven Cache Issues**
- Clear cache by re-running workflow
- Check for corrupted cache: Remove and recreate
- Ensure pom.xml format is correct

---

## Best Practices

1. **Keep workflows DRY:** Use reusable workflows for common patterns
2. **Fast Feedback:** Lint and test in parallel where possible
3. **Fail Fast:** Stop on first test failure
4. **Secure:** Use `GITHUB_TOKEN` for registry access, never commit credentials
5. **Version Control:** Pin action versions to specific releases
6. **Documentation:** Keep this file updated with workflow changes
7. **Testing:** Test workflows locally before pushing
8. **Monitoring:** Check Action logs regularly for failures

---

## References

- [GitHub Actions Documentation](https://docs.github.com/actions)
- [GitHub Container Registry](https://docs.github.com/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Dependabot Documentation](https://docs.github.com/code-security/dependabot)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Spring Boot Docker Support](https://spring.io/guides/topicals/spring-boot-docker/)
