# Local Development Setup Guide

This guide will help you set up the URL Shortener platform on your local machine using Docker and Docker Compose.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Detailed Setup](#detailed-setup)
- [Accessing the Application](#accessing-the-application)
- [Useful Commands](#useful-commands)
- [Troubleshooting](#troubleshooting)
- [Service Architecture](#service-architecture)

## Prerequisites

Before you begin, ensure you have the following installed on your system:

1. **Docker** (version 20.10 or higher)
   - [Install Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/)
   - [Install Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
   - [Install Docker Engine for Linux](https://docs.docker.com/engine/install/)

2. **Docker Compose** (version 2.0 or higher)
   - Usually included with Docker Desktop
   - For Linux: [Install Docker Compose](https://docs.docker.com/compose/install/)

3. **Git** (for cloning the repository)
   - [Install Git](https://git-scm.com/downloads)

### Verify Installation

```bash
docker --version
docker-compose --version
git --version
```

## Quick Start

Get the application running in 5 minutes:

```bash
# 1. Clone the repository
git clone <repository-url>
cd url-short

# 2. Copy the environment file
cp .env.example .env

# 3. Start all services
docker-compose up -d

# 4. View logs (optional)
docker-compose logs -f

# 5. Access the application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# API Documentation: http://localhost:8080/swagger-ui.html
```

## Detailed Setup

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd url-short
```

### Step 2: Configure Environment Variables

Copy the example environment file and customize it:

```bash
cp .env.example .env
```

Open `.env` in your favorite text editor and review/modify the following key variables:

```bash
# Database
POSTGRES_DB=urlshortener_dev
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Redis
REDIS_PASSWORD=redis123

# Backend
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=your-secret-key-change-this

# Frontend
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

**Important**: For production deployments, change all passwords and secrets to strong, random values.

### Step 3: Build and Start Services

Start all services in detached mode:

```bash
docker-compose up -d
```

This command will:
- Pull required Docker images (PostgreSQL, Redis, Kafka, Zookeeper)
- Build the backend (Spring Boot) and frontend (Next.js) applications
- Create and start all containers
- Set up networking and volumes

### Step 4: Verify Services are Running

Check the status of all services:

```bash
docker-compose ps
```

You should see all services with status "Up" and healthy:

```
NAME                        STATUS
urlshortener-backend        Up (healthy)
urlshortener-frontend       Up (healthy)
urlshortener-postgres       Up (healthy)
urlshortener-redis          Up (healthy)
urlshortener-kafka          Up (healthy)
urlshortener-zookeeper      Up (healthy)
```

### Step 5: Wait for Services to be Healthy

The first startup may take a few minutes as services initialize. Monitor the logs:

```bash
docker-compose logs -f
```

Look for these success messages:
- Backend: "Started UrlShortenerApplication"
- Frontend: "ready - started server"
- PostgreSQL: "database system is ready to accept connections"

Press `Ctrl+C` to stop following logs.

## Accessing the Application

Once all services are healthy, you can access:

### Frontend Application
- **URL**: http://localhost:3000
- **Description**: Next.js web interface for creating and managing short URLs

### Backend API
- **URL**: http://localhost:8080
- **Description**: Spring Boot REST API

### API Documentation
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Health Endpoints
- **Backend Health**: http://localhost:8080/actuator/health
- **Backend Metrics**: http://localhost:8080/actuator/metrics

### Database Access

Connect to PostgreSQL using your favorite client:

```
Host: localhost
Port: 5432
Database: urlshortener_dev
Username: postgres
Password: postgres
```

Or via command line:

```bash
docker-compose exec postgres psql -U postgres -d urlshortener_dev
```

### Redis Access

Connect to Redis CLI:

```bash
docker-compose exec redis redis-cli -a redis123
```

### Kafka Access

View Kafka topics:

```bash
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

## Useful Commands

### Start Services

```bash
# Start all services
docker-compose up -d

# Start specific service
docker-compose up -d backend

# Start with logs
docker-compose up
```

### Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v

# Stop specific service
docker-compose stop backend
```

### View Logs

```bash
# View all logs
docker-compose logs

# Follow logs in real-time
docker-compose logs -f

# View specific service logs
docker-compose logs backend
docker-compose logs frontend

# View last 100 lines
docker-compose logs --tail=100 backend
```

### Rebuild Services

```bash
# Rebuild all services
docker-compose build

# Rebuild specific service
docker-compose build backend

# Rebuild and start
docker-compose up -d --build

# Force rebuild without cache
docker-compose build --no-cache
```

### Execute Commands in Containers

```bash
# Backend container bash
docker-compose exec backend sh

# Frontend container bash
docker-compose exec frontend sh

# Run database migrations
docker-compose exec backend java -jar app.jar db:migrate
```

### View Resource Usage

```bash
# View resource usage for all containers
docker stats

# View specific container
docker stats urlshortener-backend
```

### Clean Up

```bash
# Remove stopped containers
docker-compose rm

# Remove all containers, networks, and volumes
docker-compose down -v

# Remove unused Docker resources
docker system prune -a
```

## Troubleshooting

### Issue: Containers fail to start

**Symptoms**: Services show "Exited" status

**Solutions**:
1. Check logs for specific errors:
   ```bash
   docker-compose logs backend
   ```

2. Verify port availability:
   ```bash
   # Check if ports are in use
   lsof -i :3000  # Frontend
   lsof -i :8080  # Backend
   lsof -i :5432  # PostgreSQL
   lsof -i :6379  # Redis
   lsof -i :9092  # Kafka
   ```

3. Stop conflicting services or change ports in `.env`

### Issue: Backend fails to connect to database

**Symptoms**: Backend logs show connection errors

**Solutions**:
1. Ensure PostgreSQL is healthy:
   ```bash
   docker-compose ps postgres
   ```

2. Check database credentials in `.env` match docker-compose.yml

3. Restart the backend service:
   ```bash
   docker-compose restart backend
   ```

### Issue: Frontend can't reach backend API

**Symptoms**: Frontend shows API connection errors

**Solutions**:
1. Verify backend is healthy:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. Check `NEXT_PUBLIC_API_URL` in `.env` is correct

3. Ensure network connectivity:
   ```bash
   docker network inspect url-short_urlshortener-network
   ```

### Issue: Slow build times

**Solutions**:
1. Increase Docker resources (CPU, Memory) in Docker Desktop settings

2. Use build cache:
   ```bash
   docker-compose build
   ```

3. For development, consider running services individually

### Issue: Permission errors on Linux

**Symptoms**: Permission denied errors when accessing volumes

**Solutions**:
1. Fix volume permissions:
   ```bash
   sudo chown -R $USER:$USER .
   ```

2. Or run docker-compose with sudo (not recommended)

### Issue: "port is already allocated" error

**Solutions**:
1. Find process using the port:
   ```bash
   sudo lsof -i :8080
   ```

2. Kill the process or change port in `.env`:
   ```bash
   BACKEND_PORT=8081
   ```

### Issue: Out of disk space

**Solutions**:
1. Clean up Docker resources:
   ```bash
   docker system prune -a --volumes
   ```

2. Check disk usage:
   ```bash
   docker system df
   ```

### Issue: Kafka/Zookeeper won't start

**Symptoms**: Kafka shows unhealthy status

**Solutions**:
1. Ensure Zookeeper is healthy first:
   ```bash
   docker-compose logs zookeeper
   ```

2. Increase start period in docker-compose.yml

3. Restart Kafka:
   ```bash
   docker-compose restart kafka
   ```

### Getting Help

If you encounter issues not covered here:

1. Check the logs: `docker-compose logs -f`
2. Verify your Docker version meets requirements
3. Review the [Docker documentation](https://docs.docker.com/)
4. Check project GitHub issues
5. Contact the development team

## Service Architecture

The application consists of the following services:

### Backend (Spring Boot)
- **Port**: 8080
- **Technology**: Java 21, Spring Boot 3.4
- **Dependencies**: PostgreSQL, Redis, Kafka
- **Health Check**: `/actuator/health`

### Frontend (Next.js)
- **Port**: 3000
- **Technology**: Node 20, Next.js
- **Dependencies**: Backend API
- **Health Check**: `/api/health`

### PostgreSQL
- **Port**: 5432
- **Version**: 15-alpine
- **Purpose**: Primary data storage
- **Volume**: `postgres_data`

### Redis
- **Port**: 6379
- **Version**: 7-alpine
- **Purpose**: Caching and session storage
- **Volume**: `redis_data`

### Kafka
- **Port**: 9092
- **Version**: Confluent Platform 7.5.0
- **Purpose**: Message broker for async events
- **Volume**: `kafka_data`
- **Dependencies**: Zookeeper

### Zookeeper
- **Port**: 2181
- **Version**: Confluent Platform 7.5.0
- **Purpose**: Kafka coordination
- **Volumes**: `zookeeper_data`, `zookeeper_logs`

## Development Workflow

### Making Code Changes

1. **Backend changes**:
   - Modify code in `backend/src`
   - Rebuild: `docker-compose build backend`
   - Restart: `docker-compose up -d backend`

2. **Frontend changes**:
   - Modify code in `frontend/src`
   - Rebuild: `docker-compose build frontend`
   - Restart: `docker-compose up -d frontend`

3. **For faster development**, consider running services locally outside Docker and only use Docker for databases/message brokers

### Running Tests

```bash
# Backend tests
docker-compose exec backend ./mvnw test

# Frontend tests (if configured)
docker-compose exec frontend npm test
```

### Database Migrations

Flyway migrations run automatically on backend startup. To run manually:

```bash
docker-compose exec backend java -jar app.jar db:migrate
```

## Production Deployment

For production deployment:

1. Update `.env` with production values
2. Set `SPRING_PROFILES_ACTIVE=prod`
3. Use strong passwords and secrets
4. Consider using Docker Swarm or Kubernetes
5. Set up proper monitoring and logging
6. Use external managed services for databases
7. Implement proper backup strategies

---

**Last Updated**: 2025-11-18
**Version**: 1.0.0
