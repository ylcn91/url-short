# Production Secrets Management

This guide covers secure management of sensitive configuration and secrets for the URL Shortener application in production environments.

## Overview

Proper secrets management is critical for security. Never commit secrets to version control.

## Required Secrets

### Application Secrets

| Secret | Purpose | Example | Strength Requirements |
|--------|---------|---------|----------------------|
| `JWT_SECRET` | JWT token signing | Random 256-bit key | Minimum 32 characters, cryptographically random |
| `DB_PASSWORD` | PostgreSQL password | Strong password | Minimum 16 characters, mixed case, numbers, symbols |
| `REDIS_PASSWORD` | Redis authentication | Strong password | Minimum 16 characters |
| `KAFKA_PASSWORD` | Kafka authentication | Strong password (if using SASL) | Minimum 16 characters |
| `API_KEYS` | External service keys | Various | As required by service |

### Infrastructure Secrets

| Secret | Purpose | Required In |
|--------|---------|-------------|
| `AWS_ACCESS_KEY_ID` | AWS S3 backups | Production |
| `AWS_SECRET_ACCESS_KEY` | AWS S3 backups | Production |
| `SLACK_WEBHOOK_URL` | Monitoring alerts | Production |
| `SMTP_PASSWORD` | Email notifications | Production |

## Secrets Management Approaches

### 1. Environment Variables (Basic)

**Use for:** Development, staging

**Setup:**
```bash
# Create .env file (never commit!)
cat > .env << EOF
JWT_SECRET=$(openssl rand -hex 32)
DB_PASSWORD=$(openssl rand -base64 24)
REDIS_PASSWORD=$(openssl rand -base64 24)
EOF

# Secure the file
chmod 600 .env

# Load in application
source .env
```

**Docker Compose:**
```yaml
services:
  backend:
    env_file:
      - .env
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - DB_PASSWORD=${DB_PASSWORD}
```

### 2. Docker Secrets (Recommended for Docker Swarm)

**Use for:** Production with Docker Swarm

**Setup:**
```bash
# Create secrets
echo "my_jwt_secret_key_here" | docker secret create jwt_secret -
echo "my_db_password_here" | docker secret create db_password -
echo "my_redis_password" | docker secret create redis_password -
```

**Docker Compose (Swarm mode):**
```yaml
version: '3.8'

services:
  backend:
    image: url-shortener:latest
    secrets:
      - jwt_secret
      - db_password
      - redis_password
    environment:
      - JWT_SECRET_FILE=/run/secrets/jwt_secret
      - DB_PASSWORD_FILE=/run/secrets/db_password
      - REDIS_PASSWORD_FILE=/run/secrets/redis_password

secrets:
  jwt_secret:
    external: true
  db_password:
    external: true
  redis_password:
    external: true
```

### 3. HashiCorp Vault (Enterprise Grade)

**Use for:** Large-scale production deployments

**Setup:**
```bash
# Install Vault
wget https://releases.hashicorp.com/vault/1.15.0/vault_1.15.0_linux_amd64.zip
unzip vault_1.15.0_linux_amd64.zip
sudo mv vault /usr/local/bin/

# Start Vault server (development)
vault server -dev

# Set Vault address
export VAULT_ADDR='http://127.0.0.1:8200'

# Store secrets
vault kv put secret/urlshortener/prod \
  jwt_secret="your_secret_here" \
  db_password="your_password_here" \
  redis_password="your_redis_pass"

# Retrieve secrets
vault kv get -field=jwt_secret secret/urlshortener/prod
```

**Application Integration:**
```java
// Add to pom.xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>

// application.yml
spring:
  cloud:
    vault:
      uri: https://vault.example.com
      authentication: TOKEN
      token: ${VAULT_TOKEN}
      kv:
        enabled: true
        backend: secret
        profile-separator: '/'
        default-context: urlshortener
        application-name: prod
```

### 4. AWS Secrets Manager

**Use for:** AWS-hosted production deployments

**Setup:**
```bash
# Install AWS CLI
pip install awscli

# Configure AWS credentials
aws configure

# Create secrets
aws secretsmanager create-secret \
    --name urlshortener/prod/jwt-secret \
    --secret-string "your_jwt_secret_here"

aws secretsmanager create-secret \
    --name urlshortener/prod/db-password \
    --secret-string "your_db_password"

# Retrieve secret
aws secretsmanager get-secret-value \
    --secret-id urlshortener/prod/jwt-secret \
    --query SecretString \
    --output text
```

**Application Integration:**
```java
// Add to pom.xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>secretsmanager</artifactId>
</dependency>

// Java code
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public String getSecret(String secretName) {
    SecretsManagerClient client = SecretsManagerClient.create();
    GetSecretValueRequest request = GetSecretValueRequest.builder()
        .secretId(secretName)
        .build();
    return client.getSecretValue(request).secretString();
}
```

### 5. Kubernetes Secrets

**Use for:** Kubernetes deployments

**Setup:**
```bash
# Create secret from literals
kubectl create secret generic urlshortener-secrets \
    --from-literal=jwt-secret='your_jwt_secret' \
    --from-literal=db-password='your_db_password' \
    --from-literal=redis-password='your_redis_password'

# Or from file
kubectl create secret generic urlshortener-secrets \
    --from-file=jwt-secret=./jwt-secret.txt \
    --from-file=db-password=./db-password.txt
```

**Kubernetes Deployment:**
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: urlshortener-backend
spec:
  containers:
  - name: backend
    image: url-shortener:latest
    env:
    - name: JWT_SECRET
      valueFrom:
        secretKeyRef:
          name: urlshortener-secrets
          key: jwt-secret
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: urlshortener-secrets
          key: db-password
```

## Generating Secure Secrets

### JWT Secret (256-bit)

```bash
# OpenSSL method (recommended)
openssl rand -hex 32

# Base64 encoded
openssl rand -base64 32

# UUID method
uuidgen | tr -d '-' | tr '[:upper:]' '[:lower:]'

# Python
python3 -c "import secrets; print(secrets.token_hex(32))"
```

### Database Password

```bash
# Strong password (24 characters)
openssl rand -base64 24

# Very strong password (32 characters, alphanumeric + symbols)
openssl rand -base64 32 | tr -d "=+/" | cut -c1-32

# Using pwgen
pwgen -s 32 1
```

### API Keys

```bash
# API key format
openssl rand -base64 32 | tr -d "=+/" | cut -c1-32

# With prefix
echo "urlshort_$(openssl rand -hex 20)"
```

## Configuration by Environment

### Development

```bash
# .env.development (git-ignored)
JWT_SECRET=dev_secret_not_for_production
DB_PASSWORD=postgres
REDIS_PASSWORD=redis
KAFKA_PASSWORD=kafka
```

### Staging

```bash
# .env.staging (git-ignored, stored securely)
JWT_SECRET=$(vault kv get -field=jwt_secret secret/urlshortener/staging)
DB_PASSWORD=$(vault kv get -field=db_password secret/urlshortener/staging)
REDIS_PASSWORD=$(vault kv get -field=redis_password secret/urlshortener/staging)
```

### Production

```bash
# Use secrets manager (Vault, AWS Secrets Manager, etc.)
# Never use .env files in production
# Inject secrets at runtime through orchestration platform
```

## Security Best Practices

### 1. Never Commit Secrets

```bash
# Add to .gitignore
echo ".env*" >> .gitignore
echo "*.pem" >> .gitignore
echo "*.key" >> .gitignore
echo "secrets/" >> .gitignore

# Scan for accidentally committed secrets
git secrets --scan-history
```

### 2. Rotate Secrets Regularly

**Rotation Schedule:**
- JWT secrets: Every 90 days
- Database passwords: Every 90 days
- API keys: Every 180 days
- Service accounts: Every 180 days

**Rotation Procedure:**
```bash
# 1. Generate new secret
NEW_JWT_SECRET=$(openssl rand -hex 32)

# 2. Update in secrets manager
vault kv put secret/urlshortener/prod jwt_secret="$NEW_JWT_SECRET"

# 3. Deploy application with new secret
kubectl rollout restart deployment/urlshortener-backend

# 4. Verify application health
kubectl get pods -l app=urlshortener

# 5. Document rotation in change log
```

### 3. Use Least Privilege

```sql
-- Create dedicated database user
CREATE USER urlshortener_app WITH PASSWORD 'strong_password';

-- Grant only necessary permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO urlshortener_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO urlshortener_app;

-- Revoke dangerous permissions
REVOKE CREATE ON SCHEMA public FROM urlshortener_app;
REVOKE ALL ON DATABASE urlshortener FROM urlshortener_app;
```

### 4. Enable Audit Logging

```yaml
# application.yml
logging:
  level:
    org.springframework.security: INFO
  file:
    name: /var/log/urlshortener/security.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Log secret access (not values!)
- Secret retrieved: jwt_secret
- Secret rotated: db_password
- Failed auth attempt: user@example.com
```

### 5. Encrypt Secrets at Rest

```bash
# Encrypt secrets file
gpg --symmetric --cipher-algo AES256 .env.production

# Decrypt when needed
gpg --decrypt .env.production.gpg > .env.production
source .env.production
rm .env.production  # Clean up immediately
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Deploy Production

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Configure secrets
        env:
          JWT_SECRET: ${{ secrets.JWT_SECRET }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
        run: |
          echo "JWT_SECRET=$JWT_SECRET" >> .env
          echo "DB_PASSWORD=$DB_PASSWORD" >> .env

      - name: Deploy
        run: docker-compose up -d
```

### GitLab CI

```yaml
deploy:production:
  stage: deploy
  environment: production
  variables:
    JWT_SECRET: $JWT_SECRET
    DB_PASSWORD: $DB_PASSWORD
  script:
    - docker-compose up -d
  only:
    - main
```

## Emergency Procedures

### Secret Compromised

1. **Immediately rotate the compromised secret**
2. **Deploy new secret to all environments**
3. **Invalidate all sessions/tokens if JWT secret compromised**
4. **Audit logs for unauthorized access**
5. **Notify security team**
6. **Document incident**

### Lost Access to Secrets

1. **Use emergency access procedure (if configured)**
2. **Contact secrets manager administrator**
3. **Use backup secrets (if available)**
4. **Re-initialize secrets from secure backup**
5. **Update documentation**

## Secrets Checklist

- [ ] All secrets generated with cryptographically secure methods
- [ ] Secrets stored in secrets manager (not .env files)
- [ ] .gitignore configured to prevent secret commits
- [ ] Pre-commit hooks prevent accidental commits
- [ ] Secrets rotated on schedule
- [ ] Least privilege principle applied
- [ ] Audit logging enabled
- [ ] Backup access procedure documented
- [ ] Team trained on secrets management
- [ ] Emergency procedures documented

## Tools and Resources

### Secret Scanning

```bash
# TruffleHog - Find secrets in git history
docker run --rm -v $(pwd):/repo trufflesecurity/trufflehog:latest \
    filesystem /repo

# git-secrets - Prevent committing secrets
git secrets --install
git secrets --register-aws

# Gitleaks - Scan for secrets
docker run --rm -v $(pwd):/path zricethezav/gitleaks:latest \
    detect --source /path
```

### Password Managers (Team)

- 1Password Teams
- LastPass Enterprise
- Bitwarden
- HashiCorp Vault

### Secrets Management Platforms

- HashiCorp Vault
- AWS Secrets Manager
- Azure Key Vault
- Google Cloud Secret Manager
- CyberArk

## Support

For secrets management issues:
- Security Team: security@example.com
- DevOps Team: devops@example.com
- Emergency Hotline: +1-XXX-XXX-XXXX
