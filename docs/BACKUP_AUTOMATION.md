# Database Backup Automation

This document describes the automated backup system for the URL Shortener PostgreSQL database.

## Overview

The backup system provides:
- **Automated daily backups** via cron
- **Retention policy** (30 days by default)
- **Compressed backups** using gzip
- **Environment-specific** configurations
- **Easy restore** process
- **Optional cloud upload** (S3, GCS, etc.)

## Backup Scripts

### 1. backup-database.sh

Creates compressed PostgreSQL backups with timestamp-based naming.

**Location:** `scripts/backup-database.sh`

**Usage:**
```bash
# Backup dev database
./scripts/backup-database.sh dev

# Backup production database
./scripts/backup-database.sh prod
```

**Features:**
- Automatic directory creation
- Timestamp-based file naming
- Compressed output (gzip)
- Retention policy (removes backups older than 30 days)
- Detailed logging
- Backup verification

**Output:**
- Backup files: `backups/database/urlshortener_{env}_{timestamp}.sql.gz`
- Log file: `backups/database/backup.log`

### 2. restore-database.sh

Restores a database from a backup file.

**Location:** `scripts/restore-database.sh`

**Usage:**
```bash
# Restore from backup file
./scripts/restore-database.sh backups/database/urlshortener_dev_20250118_143000.sql.gz dev
```

**Features:**
- Safety confirmation prompt
- Database recreation
- Restoration verification
- Table count check

**⚠️ WARNING:** This script will DROP and RECREATE the database. All existing data will be lost.

## Automated Scheduling with Cron

### Production Setup (Recommended)

#### 1. Make scripts executable
```bash
chmod +x scripts/backup-database.sh
chmod +x scripts/restore-database.sh
```

#### 2. Edit crontab
```bash
crontab -e
```

#### 3. Add backup schedule

**Daily backup at 2:00 AM:**
```cron
0 2 * * * /path/to/url-short/scripts/backup-database.sh prod >> /var/log/urlshort/backup-cron.log 2>&1
```

**Daily backup at 2:00 AM with email notifications:**
```cron
MAILTO=admin@example.com
0 2 * * * /path/to/url-short/scripts/backup-database.sh prod
```

**Multiple daily backups:**
```cron
# Every 6 hours
0 */6 * * * /path/to/url-short/scripts/backup-database.sh prod >> /var/log/urlshort/backup-cron.log 2>&1
```

**Weekly backup on Sunday at 3:00 AM:**
```cron
0 3 * * 0 /path/to/url-short/scripts/backup-database.sh prod >> /var/log/urlshort/backup-weekly.log 2>&1
```

#### 4. Verify cron job
```bash
# List active cron jobs
crontab -l

# Monitor cron logs
tail -f /var/log/cron
# or on Ubuntu/Debian
tail -f /var/log/syslog | grep CRON
```

### Development Setup

For development environments, you may want less frequent backups:

```cron
# Daily at midnight
0 0 * * * /path/to/url-short/scripts/backup-database.sh dev >> /var/log/urlshort/backup-dev.log 2>&1
```

## Environment Variables

Set these environment variables for the backup scripts:

```bash
# Database configuration
export DB_NAME=urlshortener_prod
export DB_USER=postgres
export DB_PASSWORD=your_secure_password
export DB_HOST=localhost
export DB_PORT=5432

# Optional: AWS S3 for cloud backup
export AWS_S3_BUCKET=my-backups
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret
```

### Using a .env file

Create a `.env.backup` file:

```bash
DB_NAME=urlshortener_prod
DB_USER=postgres
DB_PASSWORD=secure_password
DB_HOST=localhost
DB_PORT=5432
AWS_S3_BUCKET=my-backups
```

Source it before running backup:
```bash
source .env.backup && ./scripts/backup-database.sh prod
```

## Cloud Storage Integration

### AWS S3

Uncomment the S3 upload section in `backup-database.sh`:

```bash
if [ -n "${AWS_S3_BUCKET:-}" ]; then
    log "Uploading backup to S3..."
    aws s3 cp "$BACKUP_FILE" "s3://${AWS_S3_BUCKET}/backups/database/" || warning "S3 upload failed"
fi
```

Install AWS CLI:
```bash
# Ubuntu/Debian
sudo apt-get install awscli

# macOS
brew install awscli

# Configure
aws configure
```

### Google Cloud Storage

Add to `backup-database.sh`:

```bash
if [ -n "${GCS_BUCKET:-}" ]; then
    log "Uploading backup to GCS..."
    gsutil cp "$BACKUP_FILE" "gs://${GCS_BUCKET}/backups/database/" || warning "GCS upload failed"
fi
```

### Azure Blob Storage

Add to `backup-database.sh`:

```bash
if [ -n "${AZURE_STORAGE_ACCOUNT:-}" ]; then
    log "Uploading backup to Azure..."
    az storage blob upload \
        --account-name "$AZURE_STORAGE_ACCOUNT" \
        --container-name backups \
        --name "database/$(basename "$BACKUP_FILE")" \
        --file "$BACKUP_FILE" || warning "Azure upload failed"
fi
```

## Monitoring and Alerting

### 1. Email Notifications

Add to crontab:
```cron
MAILTO=admin@example.com
0 2 * * * /path/to/url-short/scripts/backup-database.sh prod
```

### 2. Slack Notifications

Add to end of `backup-database.sh`:

```bash
# Slack notification
if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
    curl -X POST -H 'Content-type: application/json' \
        --data "{\"text\":\"✅ Database backup completed: $BACKUP_FILE ($BACKUP_SIZE)\"}" \
        "$SLACK_WEBHOOK_URL"
fi
```

### 3. Health Check Monitoring

Use services like Healthchecks.io:

```bash
# Add to end of backup-database.sh
if [ -n "${HEALTHCHECK_URL:-}" ]; then
    curl -fsS --retry 3 "$HEALTHCHECK_URL" > /dev/null
fi
```

## Backup Retention Policy

Default retention: **30 days**

To change retention period, edit `backup-database.sh`:

```bash
RETENTION_DAYS=60  # Keep backups for 60 days
```

### Custom Retention Strategy

Implement a tiered retention policy:

```bash
# Keep:
# - Daily backups for 7 days
# - Weekly backups for 4 weeks
# - Monthly backups for 12 months

# Add to cleanup section in backup-database.sh:
# Daily backups (older than 7 days)
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -type f -mtime +7 -delete

# Weekly backups (keep Sundays for 4 weeks)
# Monthly backups (keep first of month for 12 months)
# ... (implement as needed)
```

## Disaster Recovery Procedure

### 1. Identify the backup to restore

```bash
ls -lh backups/database/
```

### 2. Stop the application

```bash
docker-compose down
# or
systemctl stop urlshortener
```

### 3. Restore the database

```bash
./scripts/restore-database.sh backups/database/urlshortener_prod_20250118_020000.sql.gz prod
```

### 4. Verify restoration

```bash
psql -U postgres -d urlshortener_prod -c "SELECT COUNT(*) FROM short_links;"
```

### 5. Restart the application

```bash
docker-compose up -d
# or
systemctl start urlshortener
```

### 6. Verify application health

```bash
curl http://localhost:8080/actuator/health
```

## Testing Backups

### Regular Backup Testing

Test restores periodically to ensure backups are valid:

```bash
# 1. Create a test database
createdb urlshortener_test_restore

# 2. Restore to test database
export DB_NAME=urlshortener_test_restore
./scripts/restore-database.sh backups/database/latest_backup.sql.gz test

# 3. Verify data integrity
psql -U postgres -d urlshortener_test_restore -c "SELECT COUNT(*) FROM short_links;"

# 4. Clean up
dropdb urlshortener_test_restore
```

## Backup Checklist

- [ ] Backup scripts are executable
- [ ] Cron job is configured and active
- [ ] Environment variables are set
- [ ] Backup directory has sufficient space
- [ ] Retention policy is configured
- [ ] Cloud storage is configured (if applicable)
- [ ] Monitoring/alerting is set up
- [ ] Backup restoration has been tested
- [ ] Disaster recovery procedure is documented
- [ ] Team is trained on restore procedure

## Troubleshooting

### Backup fails with "permission denied"

```bash
# Make script executable
chmod +x scripts/backup-database.sh

# Check backup directory permissions
chmod 755 backups/database
```

### Cron job doesn't run

```bash
# Check cron service status
systemctl status cron

# Check cron logs
tail -f /var/log/syslog | grep CRON

# Verify crontab entry
crontab -l
```

### pg_dump not found

```bash
# Install PostgreSQL client tools
sudo apt-get install postgresql-client

# Verify installation
which pg_dump
```

### Out of disk space

```bash
# Check disk usage
df -h

# Manually clean old backups
rm backups/database/urlshortener_prod_2024*.sql.gz

# Reduce retention period in backup script
RETENTION_DAYS=7
```

## Best Practices

1. **Test restores regularly** - Monthly test restores to verify backup integrity
2. **Monitor backup size** - Sudden changes may indicate issues
3. **Use cloud storage** - Store backups off-site for disaster recovery
4. **Encrypt backups** - Use gpg for sensitive data
5. **Document procedures** - Keep recovery procedures up-to-date
6. **Set up alerting** - Get notified of backup failures immediately
7. **Verify backup completion** - Check logs after each backup
8. **Maintain multiple backup locations** - Local + cloud storage
9. **Use separate credentials** - Dedicated backup user with minimal permissions
10. **Audit backup access** - Monitor who accesses backups

## Security Considerations

### Encrypt Backups

```bash
# Encrypt backup file
gpg --symmetric --cipher-algo AES256 backup.sql.gz

# Decrypt for restore
gpg --decrypt backup.sql.gz.gpg | gunzip | psql ...
```

### Secure Backup Storage

```bash
# Restrict backup directory access
chmod 700 backups/database

# Use dedicated backup user
createuser --no-superuser --no-createdb --no-createrole backup_user
GRANT SELECT ON ALL TABLES IN SCHEMA public TO backup_user;
```

### Environment Variable Security

Never commit `.env` files with credentials. Use:
- Secrets management (Vault, AWS Secrets Manager)
- Encrypted environment files
- CI/CD secret variables

## Support

For issues or questions:
- Check logs: `backups/database/backup.log`
- Review documentation: `docs/DATABASE_SCHEMA.md`
- Contact: devops@example.com
