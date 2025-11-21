# Production Deployment Guide

Complete guide for deploying Linkforge URL Shortener to production environments.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Environment Variables](#environment-variables)
- [Database Setup](#database-setup)
- [Application Deployment](#application-deployment)
- [Docker Deployment](#docker-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Scaling Strategy](#scaling-strategy)
- [Monitoring and Observability](#monitoring-and-observability)
- [Backup and Disaster Recovery](#backup-and-disaster-recovery)
- [Security Hardening](#security-hardening)
- [Performance Tuning](#performance-tuning)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Infrastructure Requirements

**Minimum (Single Instance):**
- VM: 2 vCPU, 4GB RAM (e.g., AWS t3.medium, GCP e2-medium)
- PostgreSQL: 2 vCPU, 4GB RAM (e.g., AWS db.t3.medium)
- Storage: 50GB SSD
- Network: Static IP or load balancer with SSL certificate

**Recommended (Production):**
- VMs: 2+ instances behind load balancer (4 vCPU, 16GB RAM each)
- PostgreSQL: Primary + read replica (4 vCPU, 16GB RAM)
- Storage: 200GB SSD with automated backups
- CDN: CloudFlare or AWS CloudFront for redirects
- Redis: Cluster with 16GB RAM (for caching)
- Monitoring: Prometheus + Grafana or DataDog

### Software Requirements

- Java 21 (OpenJDK or Amazon Corretto)
- PostgreSQL 15+
- Maven 3.8+ (for building)
- Docker 24+ (optional, for containerized deployment)
- Kubernetes 1.27+ (optional, for k8s deployment)

---

## Environment Variables

### Required Variables

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://db.example.com:5432/urlshort
SPRING_DATASOURCE_USERNAME=urlshort_user
SPRING_DATASOURCE_PASSWORD=your_secure_password_here

# Application
SERVER_PORT=8080
APP_SHORT_URL_BASE_URL=https://short.ly

# Security
JWT_SECRET=your_256_bit_secret_key_here
JWT_EXPIRATION=3600000  # 1 hour in milliseconds

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_URLSHORT=INFO
```

### Optional Variables

```bash
# Database Connection Pool
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=10
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000

# Redis (for caching - v2.0)
SPRING_REDIS_HOST=redis.example.com
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=redis_password

# Kafka (for analytics - v2.0)
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092,kafka3:9092

# Monitoring
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true

# CORS (if frontend on different domain)
CORS_ALLOWED_ORIGINS=https://dashboard.linkforge.io

# Rate Limiting
RATE_LIMIT_ENABLED=true
RATE_LIMIT_REQUESTS_PER_MINUTE=100
```

### Secure Secret Management

**Do not hardcode secrets in code or config files.**

**Option 1: Environment Variables (Simple)**
```bash
export SPRING_DATASOURCE_PASSWORD=$(cat /secrets/db_password)
java -jar app.jar
```

**Option 2: AWS Secrets Manager**
```bash
export DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id prod/urlshort/db-password \
  --query SecretString \
  --output text)
```

**Option 3: HashiCorp Vault**
```bash
export SPRING_DATASOURCE_PASSWORD=$(vault kv get -field=password secret/urlshort/database)
```

**Option 4: Kubernetes Secrets**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: urlshort-secrets
type: Opaque
stringData:
  database-password: your_password_here
  jwt-secret: your_jwt_secret_here
```

---

## Database Setup

### 1. Create Production Database

```bash
# Connect to PostgreSQL
psql -h db.example.com -U postgres

# Create database
CREATE DATABASE urlshort;

# Create user with limited permissions
CREATE USER urlshort_user WITH PASSWORD 'your_secure_password';

# Grant permissions
GRANT CONNECT ON DATABASE urlshort TO urlshort_user;
\c urlshort
GRANT USAGE ON SCHEMA public TO urlshort_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO urlshort_user;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO urlshort_user;

# Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

### 2. Run Flyway Migrations

Migrations run automatically on application startup, or you can run manually:

```bash
# Manual migration with Maven
mvn flyway:migrate \
  -Dflyway.url=jdbc:postgresql://db.example.com:5432/urlshort \
  -Dflyway.user=urlshort_user \
  -Dflyway.password=your_password
```

### 3. Verify Schema

```bash
psql -h db.example.com -U urlshort_user -d urlshort

# List tables
\dt

# Expected output:
# workspace, users, short_link, click_event, api_key, flyway_schema_history

# Verify indexes
\di

# Check constraints
SELECT conname, contype FROM pg_constraint WHERE conrelid = 'short_link'::regclass;
```

---

## Application Deployment

### Option 1: JAR Deployment (Traditional)

**Build the application:**

```bash
# Clone repository
git clone https://github.com/yourorg/url-short.git
cd url-short

# Build JAR
mvn clean package -DskipTests

# JAR located at: target/url-shortener-0.0.1-SNAPSHOT.jar
```

**Deploy to server:**

```bash
# Copy JAR to server
scp target/url-shortener-0.0.1-SNAPSHOT.jar user@server:/opt/urlshort/

# SSH to server
ssh user@server

# Create systemd service
sudo nano /etc/systemd/system/urlshort.service
```

**systemd service file:**

```ini
[Unit]
Description=Linkforge URL Shortener
After=network.target postgresql.service

[Service]
Type=simple
User=urlshort
WorkingDirectory=/opt/urlshort
ExecStart=/usr/bin/java \
  -Xms2g -Xmx4g \
  -Dspring.profiles.active=production \
  -jar /opt/urlshort/url-shortener-0.0.1-SNAPSHOT.jar

# Environment
EnvironmentFile=/opt/urlshort/.env

# Restart policy
Restart=always
RestartSec=10

# Logging
StandardOutput=journal
StandardError=journal

# Security
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

**Start the service:**

```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable on boot
sudo systemctl enable urlshort

# Start service
sudo systemctl start urlshort

# Check status
sudo systemctl status urlshort

# View logs
sudo journalctl -u urlshort -f
```

---

## Docker Deployment

### 1. Create Dockerfile

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S urlshort && adduser -S urlshort -G urlshort

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown -R urlshort:urlshort /app

# Switch to non-root user
USER urlshort

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. Create docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: urlshort-db
    environment:
      POSTGRES_DB: urlshort
      POSTGRES_USER: urlshort_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U urlshort_user"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    container_name: urlshort-app
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/urlshort
      SPRING_DATASOURCE_USERNAME: urlshort_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SERVER_PORT: 8080
      APP_SHORT_URL_BASE_URL: ${BASE_URL}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: urlshort-nginx
    depends_on:
      - app
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    ports:
      - "80:80"
      - "443:443"
    restart: unless-stopped

volumes:
  postgres_data:
```

### 3. Deploy with Docker Compose

```bash
# Set environment variables
export DB_PASSWORD=your_secure_password
export BASE_URL=https://short.ly
export JWT_SECRET=your_jwt_secret

# Build and start
docker-compose up -d

# View logs
docker-compose logs -f

# Check health
curl http://localhost:8080/actuator/health

# Stop
docker-compose down
```

---

## Kubernetes Deployment

### 1. Create Kubernetes Manifests

**namespace.yaml:**

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: urlshort
```

**configmap.yaml:**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: urlshort-config
  namespace: urlshort
data:
  SERVER_PORT: "8080"
  SPRING_PROFILES_ACTIVE: "production"
  LOGGING_LEVEL_ROOT: "INFO"
```

**secret.yaml:**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: urlshort-secrets
  namespace: urlshort
type: Opaque
stringData:
  database-url: jdbc:postgresql://postgres-service:5432/urlshort
  database-username: urlshort_user
  database-password: your_password_here
  jwt-secret: your_jwt_secret_here
```

**deployment.yaml:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: urlshort-app
  namespace: urlshort
spec:
  replicas: 3
  selector:
    matchLabels:
      app: urlshort
  template:
    metadata:
      labels:
        app: urlshort
    spec:
      containers:
      - name: urlshort
        image: your-registry/urlshort:v1.0.0
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: urlshort-config
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: urlshort-secrets
              key: database-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: urlshort-secrets
              key: database-username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: urlshort-secrets
              key: database-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: urlshort-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
```

**service.yaml:**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: urlshort-service
  namespace: urlshort
spec:
  type: ClusterIP
  selector:
    app: urlshort
  ports:
  - port: 80
    targetPort: 8080
```

**ingress.yaml:**

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: urlshort-ingress
  namespace: urlshort
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.linkforge.io
    secretName: urlshort-tls
  rules:
  - host: api.linkforge.io
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: urlshort-service
            port:
              number: 80
```

### 2. Deploy to Kubernetes

```bash
# Apply manifests
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f ingress.yaml

# Check deployment
kubectl -n urlshort get pods
kubectl -n urlshort get svc
kubectl -n urlshort get ingress

# View logs
kubectl -n urlshort logs -f deployment/urlshort-app

# Scale deployment
kubectl -n urlshort scale deployment/urlshort-app --replicas=5
```

---

## Scaling Strategy

### Horizontal Scaling (Application Tier)

**Stateless application servers allow easy horizontal scaling:**

```bash
# Docker Compose
docker-compose up --scale app=3

# Kubernetes
kubectl -n urlshort scale deployment/urlshort-app --replicas=5

# AWS Auto Scaling Group
aws autoscaling set-desired-capacity \
  --auto-scaling-group-name urlshort-asg \
  --desired-capacity 5
```

**Load Balancer Configuration (nginx):**

```nginx
upstream urlshort_backend {
    least_conn;  # Use least connections algorithm
    server app1:8080 max_fails=3 fail_timeout=30s;
    server app2:8080 max_fails=3 fail_timeout=30s;
    server app3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name api.linkforge.io;

    location / {
        proxy_pass http://urlshort_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 5s;
        proxy_send_timeout 10s;
        proxy_read_timeout 10s;
    }
}
```

### Vertical Scaling (Database Tier)

**PostgreSQL Read Replicas:**

```yaml
# Primary database (writes)
SPRING_DATASOURCE_URL=jdbc:postgresql://db-primary:5432/urlshort

# Read replica (analytics queries)
SPRING_DATASOURCE_READ_REPLICA_URL=jdbc:postgresql://db-replica:5432/urlshort
```

**Configure read/write splitting in Spring:**

```java
@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        // Write datasource
    }

    @Bean
    public DataSource replicaDataSource() {
        // Read datasource
    }
}
```

---

## Monitoring and Observability

### Metrics (Prometheus + Grafana)

**Enable Prometheus endpoint:**

```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

**Prometheus scrape config:**

```yaml
scrape_configs:
  - job_name: 'urlshort'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app1:8080', 'app2:8080', 'app3:8080']
```

**Key metrics to monitor:**

```
# Application
http_server_requests_seconds_count
http_server_requests_seconds_sum
jvm_memory_used_bytes
jvm_threads_live_threads

# Business
linkforge_links_created_total
linkforge_redirects_total
linkforge_redirect_duration_seconds{quantile="0.95"}
```

### Logging (ELK Stack or CloudWatch)

**Structured JSON logging:**

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

**Ship logs to centralized system:**

```bash
# Fluentd
docker run -d \
  -v /var/log:/var/log \
  fluent/fluentd \
  -c /fluentd/etc/fluent.conf
```

### Health Checks

**Liveness probe (is app running?):**
```bash
curl http://localhost:8080/actuator/health/liveness
```

**Readiness probe (is app ready for traffic?):**
```bash
curl http://localhost:8080/actuator/health/readiness
```

### Alerting

**Example alerts (AlertManager):**

```yaml
groups:
  - name: urlshort
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate detected"

      - alert: HighLatency
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 1
        for: 5m
        annotations:
          summary: "P95 latency > 1 second"

      - alert: DatabaseDown
        expr: up{job="postgres"} == 0
        for: 1m
        annotations:
          summary: "PostgreSQL is down"
```

---

## Backup and Disaster Recovery

### Database Backups

**Daily automated backups:**

```bash
#!/bin/bash
# backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/backups
DB_HOST=db.example.com
DB_NAME=urlshort

# Full database backup
pg_dump -h $DB_HOST -U urlshort_user $DB_NAME | gzip > $BACKUP_DIR/urlshort_$DATE.sql.gz

# Upload to S3
aws s3 cp $BACKUP_DIR/urlshort_$DATE.sql.gz s3://urlshort-backups/

# Delete local backup older than 7 days
find $BACKUP_DIR -name "urlshort_*.sql.gz" -mtime +7 -delete
```

**Schedule with cron:**

```bash
# Run daily at 2 AM
0 2 * * * /opt/scripts/backup.sh >> /var/log/backup.log 2>&1
```

### Point-in-Time Recovery (PITR)

**Enable WAL archiving:**

```sql
-- postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'cp %p /archive/%f'
```

### Restore Procedure

```bash
# Download backup
aws s3 cp s3://urlshort-backups/urlshort_20251118.sql.gz .

# Restore
gunzip urlshort_20251118.sql.gz
psql -h db.example.com -U urlshort_user -d urlshort < urlshort_20251118.sql
```

---

## Security Hardening

### 1. Network Security

**Firewall rules (iptables):**

```bash
# Allow HTTP/HTTPS
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# Allow SSH (restrict to admin IP)
iptables -A INPUT -p tcp -s 1.2.3.4 --dport 22 -j ACCEPT

# Block everything else
iptables -A INPUT -j DROP
```

### 2. SSL/TLS Configuration

**nginx SSL config:**

```nginx
server {
    listen 443 ssl http2;
    server_name api.linkforge.io;

    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
}
```

### 3. Application Security

**Enable HTTPS only:**

```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
```

**Enable CORS properly:**

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://dashboard.linkforge.io")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true);
    }
}
```

---

## Performance Tuning

### JVM Tuning

```bash
java \
  -Xms2g \
  -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+ParallelRefProcEnabled \
  -jar app.jar
```

### PostgreSQL Tuning

```sql
-- postgresql.conf

# Memory
shared_buffers = 4GB
effective_cache_size = 12GB
work_mem = 64MB
maintenance_work_mem = 1GB

# Connections
max_connections = 200

# Checkpoints
checkpoint_completion_target = 0.9
wal_buffers = 16MB

# Query planner
random_page_cost = 1.1  # For SSD
effective_io_concurrency = 200
```

### Connection Pool Tuning

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

---

## Troubleshooting

### Application Won't Start

```bash
# Check logs
journalctl -u urlshort -n 100 --no-pager

# Common issues:
# 1. Database connection failed
# 2. Port 8080 already in use
# 3. Missing environment variables

# Test database connection
psql -h db.example.com -U urlshort_user -d urlshort

# Check port
netstat -tuln | grep 8080
```

### High Latency

```bash
# Check JVM heap
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Check database connections
curl http://localhost:8080/actuator/metrics/hikari.connections.active

# Check slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

### Out of Memory

```bash
# Increase heap size
export JAVA_OPTS="-Xms4g -Xmx8g"

# Enable heap dump on OOM
export JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof"
```

---

**Last Updated:** 2025-11-18
**Document Version:** 1.0
