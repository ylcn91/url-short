#!/bin/bash
#################################################
# PostgreSQL Database Backup Script
#
# This script creates automated backups of the
# URL shortener PostgreSQL database with
# timestamp-based file naming and retention policy.
#
# Usage:
#   ./scripts/backup-database.sh [environment]
#
# Environment: dev, prod (default: dev)
#################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# Configuration
ENVIRONMENT="${1:-dev}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="${PROJECT_ROOT}/backups/database"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Database configuration based on environment
if [ "$ENVIRONMENT" = "prod" ]; then
    DB_NAME="${DB_NAME:-urlshortener_prod}"
    DB_USER="${DB_USER:-postgres}"
    DB_HOST="${DB_HOST:-localhost}"
    DB_PORT="${DB_PORT:-5432}"
else
    DB_NAME="${DB_NAME:-urlshortener_dev}"
    DB_USER="${DB_USER:-postgres}"
    DB_HOST="${DB_HOST:-localhost}"
    DB_PORT="${DB_PORT:-5432}"
fi

BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"
BACKUP_LOG="${BACKUP_DIR}/backup.log"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$BACKUP_LOG"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$BACKUP_LOG"
    exit 1
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$BACKUP_LOG"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$BACKUP_LOG"
}

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

log "Starting database backup for environment: $ENVIRONMENT"
log "Database: $DB_NAME"
log "Host: $DB_HOST:$DB_PORT"
log "Backup file: $BACKUP_FILE"

# Check if pg_dump is available
if ! command -v pg_dump &> /dev/null; then
    error "pg_dump command not found. Please install PostgreSQL client tools."
fi

# Perform the backup
log "Creating backup..."
if PGPASSWORD="${DB_PASSWORD}" pg_dump \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    --format=plain \
    --no-owner \
    --no-acl \
    --verbose \
    2>> "$BACKUP_LOG" | gzip > "$BACKUP_FILE"; then

    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    success "Backup created successfully: $BACKUP_FILE ($BACKUP_SIZE)"
else
    error "Backup failed. Check log file: $BACKUP_LOG"
fi

# Verify backup file
if [ ! -s "$BACKUP_FILE" ]; then
    error "Backup file is empty or doesn't exist"
fi

# Clean up old backups (older than retention period)
log "Cleaning up old backups (older than $RETENTION_DAYS days)..."
OLD_BACKUPS=$(find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -type f -mtime +$RETENTION_DAYS)

if [ -n "$OLD_BACKUPS" ]; then
    echo "$OLD_BACKUPS" | while read -r old_backup; do
        log "Removing old backup: $(basename "$old_backup")"
        rm -f "$old_backup"
    done
    success "Old backups cleaned up"
else
    log "No old backups to clean up"
fi

# Display backup statistics
log "=== Backup Statistics ==="
log "Total backups: $(find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -type f | wc -l)"
log "Total size: $(du -sh "$BACKUP_DIR" | cut -f1)"
log "Latest backup: $(basename "$BACKUP_FILE")"
log "========================="

success "Backup process completed successfully"

# Optional: Upload to S3 or other cloud storage
# Uncomment and configure if needed
# if [ -n "${AWS_S3_BUCKET:-}" ]; then
#     log "Uploading backup to S3..."
#     aws s3 cp "$BACKUP_FILE" "s3://${AWS_S3_BUCKET}/backups/database/" || warning "S3 upload failed"
# fi

exit 0
