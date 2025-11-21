# GitHub Actions CI/CD - Quick Reference Guide

## Workflow Triggers

| Workflow | Trigger Event | Paths |
|----------|---------------|-------|
| **backend-ci.yml** | Push/PR to main | src/**, pom.xml |
| **frontend-ci.yml** | Push/PR to main | frontend/** |
| **integration-test.yml** | Push/PR to main | src/**, frontend/**, docker-compose.* |

## Job Dependencies

### Backend CI
```
test (Java 21, Maven)
  ↓
build (Package JAR)
  ↓
docker (Build & Push image) [main branch only]
```

### Frontend CI
```
lint (ESLint)  ←→  test (Jest + Coverage)
  ↓                      ↓
  └──────→ build (Next.js) ←─┘
              ↓
           docker (Build & Push) [main branch only]
```

### Integration Tests
```
PostgreSQL 16 + Redis 7 + Backend + Frontend Services
  ↓
Backend Integration Tests + Frontend E2E Tests
  ↓
Publish Results
```

## Common Commands

### Local Development
```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Stop services
docker-compose down

# Rebuild services
docker-compose up -d --build
```

### Testing
```bash
# Backend tests
mvn clean test

# Backend full verification
mvn verify

# Frontend tests
cd frontend && npm test

# Frontend with coverage
cd frontend && npm test -- --coverage
```

### Building
```bash
# Backend JAR
mvn clean package

# Frontend production build
cd frontend && npm run build

# Docker images
docker build -f Dockerfile.backend -t url-shortener:backend .
docker build -f frontend/Dockerfile -t url-shortener:frontend ./frontend
```

### Integration Testing
```bash
# Start test environment
docker-compose -f docker-compose.test.yml up -d

# Run tests
mvn verify -Dgroups=integration
cd frontend && npm run test:e2e

# Cleanup
docker-compose -f docker-compose.test.yml down -v
```

## Environment Setup

```bash
# 1. Copy template
cp .env.example .env

# 2. Edit for your environment
nano .env

# 3. Common variables to change
POSTGRES_PASSWORD=your_secure_password
POSTGRES_DB=url_shortener
SPRING_PROFILES_ACTIVE=prod
NEXT_PUBLIC_API_URL=http://your-domain.com
```

## GitHub Actions Interface

### View Workflow Status
1. Go to repository → **Actions** tab
2. Select workflow name (Backend CI, Frontend CI, Integration Tests)
3. Click latest run to see details

### View Logs
1. Click on job name
2. Click on step to expand logs
3. Search for errors with Ctrl+F

### Re-run Workflow
1. Click "Re-run jobs" button (top right)
2. Or re-run failed jobs only

### View Artifacts
1. Click workflow run
2. Scroll to "Artifacts" section
3. Download test results, JARs, or build outputs

## Secrets Configuration

### Set a Secret
1. Go to repository → **Settings** → **Secrets and variables** → **Actions**
2. Click "New repository secret"
3. Name: `SECRET_NAME`
4. Value: `secret_value`
5. Click "Add secret"

### Available Secrets
- `GITHUB_TOKEN` (automatic)
- `CODECOV_TOKEN` (optional)
- `DOCKER_USERNAME` (optional)
- `DOCKER_PASSWORD` (optional)

## Dependabot Configuration

### Review Dependabot PRs
1. Go to **Pull requests** tab
2. Filter by `author:dependabot`
3. Review changes, run tests, merge

### Edit Dependabot Schedule
1. Edit `.github/dependabot.yml`
2. Modify `schedule.interval` (daily, weekly, monthly)
3. Modify `schedule.day` and `schedule.time`
4. Commit and push

### Disable Dependabot for Ecosystem
1. Edit `.github/dependabot.yml`
2. Remove the ecosystem section
3. Commit and push

## Caching

### Cache Strategies in Workflows

| Ecosystem | Cache Key | Directory |
|-----------|-----------|-----------|
| Maven | pom.xml | ~/.m2 |
| npm | package-lock.json | ./node_modules |
| Docker | GitHub Actions | Layer cache |

### Force Cache Clear
1. Go to **Settings** → **Actions** → **General**
2. Click "Clear all" in Actions cache section
3. Or re-run workflow with cache cleared

## Docker Registry (GHCR)

### Image Tagging
- `latest` - Latest version (main branch only)
- `main` - Main branch version
- `v1.2.3` - Semver release
- `sha-abc123` - Specific commit SHA

### Push to GHCR
```bash
docker login ghcr.io
docker tag url-shortener:backend ghcr.io/username/url-short/backend:latest
docker push ghcr.io/username/url-short/backend:latest
```

### Pull from GHCR
```bash
docker pull ghcr.io/username/url-short/backend:latest
docker pull ghcr.io/username/url-short/frontend:latest
```

## Troubleshooting Checklist

### Workflow Won't Trigger
- [ ] Check branch is `main`
- [ ] Verify file changes match workflow paths
- [ ] Check branch protection rules
- [ ] Verify workflow file syntax (YAML)

### Tests Failing
- [ ] Check test logs in GitHub Actions
- [ ] Run tests locally first
- [ ] Verify dependencies are cached
- [ ] Check environment variables

### Docker Build Fails
- [ ] Verify Dockerfile syntax
- [ ] Check base image availability
- [ ] Ensure Dockerfile is in correct location
- [ ] Check Docker buildx is available

### Services Not Starting
- [ ] Check docker-compose syntax
- [ ] Verify port availability
- [ ] Check service dependencies
- [ ] Review service logs

### Dependabot Issues
- [ ] Enable Dependabot in settings
- [ ] Check branch protection rules
- [ ] Review Dependabot permissions
- [ ] Check for merge conflicts

## Performance Tips

### Speed Up Maven Builds
```bash
# Use parallel compilation
mvn -T 1C verify

# Skip checkstyle/quality plugins
mvn verify -DskipChecks
```

### Speed Up npm Builds
```bash
# Use npm ci instead of npm install
npm ci

# Skip optional dependencies
npm ci --no-optional
```

### Improve CI/CD Speed
1. Use caching (configured)
2. Run jobs in parallel
3. Skip unnecessary steps
4. Use Alpine images (configured)
5. Cache Docker layers

## Monitoring & Alerts

### Status Badges
Add to README.md:
```markdown
[![Backend CI](https://github.com/USER/repo/actions/workflows/backend-ci.yml/badge.svg)](...)
[![Frontend CI](https://github.com/USER/repo/actions/workflows/frontend-ci.yml/badge.svg)](...)
```

### Branch Protection Rules
1. Go to **Settings** → **Branches**
2. Add rule for `main` branch
3. Require status checks to pass:
   - Backend CI
   - Frontend CI
4. Enable auto-dismiss stale reviews
5. Enable require code reviews

### Enable Notifications
1. Click your profile icon → **Settings**
2. Go to **Notifications**
3. Configure GitHub Actions notifications
4. Set up email alerts for failures

## Workflow Customization

### Add New Workflow
1. Create `.github/workflows/my-workflow.yml`
2. Copy structure from existing workflow
3. Modify triggers and jobs
4. Test with `act` locally (optional)

### Modify Job Steps
1. Edit `.github/workflows/workflow-name.yml`
2. Add/remove steps in job
3. Use available actions from GitHub Marketplace
4. Commit and push

### Add New Service
1. Edit `docker-compose.yml` or `docker-compose.test.yml`
2. Add new service definition
3. Configure ports, volumes, environment
4. Update dependent services

## Resources

- **Workflow Syntax:** `.github/WORKFLOWS.md`
- **Implementation Details:** `.github/IMPLEMENTATION_SUMMARY.md`
- **GitHub Actions Docs:** https://docs.github.com/actions
- **Docker Documentation:** https://docs.docker.com

---

**Last Updated:** November 2024
**Workflows Count:** 3
**Total Steps:** 40+
