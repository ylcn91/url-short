#!/bin/bash
#################################################
# PostgreSQL Database Restore Script
#
# This script restores a PostgreSQL database from
# a backup file created by backup-database.sh
#
# Usage:
#   ./scripts/restore-database.sh <backup_file> [environment]
#
# Example:
#   ./scripts/restore-database.sh backups/database/urlshortener_dev_20250118_143000.sql.gz dev
#################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# Configuration
BACKUP_FILE="$1"
ENVIRONMENT="${2:-dev}"

# Validate backup file
if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: Backup file not found: $BACKUP_FILE"
    exit 1
fi

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

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Confirmation prompt
echo ""
echo "========================================="
echo "DATABASE RESTORE OPERATION"
echo "========================================="
echo "Environment: $ENVIRONMENT"
echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo "Backup file: $BACKUP_FILE"
echo ""
warning "This will DROP and RECREATE the database!"
warning "ALL EXISTING DATA WILL BE LOST!"
echo ""
read -p "Are you sure you want to continue? (type 'yes' to confirm): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Restore cancelled."
    exit 0
fi

# Check if psql is available
if ! command -v psql &> /dev/null; then
    error "psql command not found. Please install PostgreSQL client tools."
fi

log "Starting database restore..."

# Drop existing database
log "Dropping existing database..."
PGPASSWORD="${DB_PASSWORD}" psql \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d postgres \
    -c "DROP DATABASE IF EXISTS $DB_NAME;" || error "Failed to drop database"

# Create new database
log "Creating new database..."
PGPASSWORD="${DB_PASSWORD}" psql \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d postgres \
    -c "CREATE DATABASE $DB_NAME;" || error "Failed to create database"

# Restore from backup
log "Restoring data from backup..."
if gunzip -c "$BACKUP_FILE" | PGPASSWORD="${DB_PASSWORD}" psql \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    --quiet; then
    success "Database restored successfully"
else
    error "Restore failed"
fi

# Verify restoration
log "Verifying restoration..."
TABLE_COUNT=$(PGPASSWORD="${DB_PASSWORD}" psql \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public';" | tr -d ' ')

log "Tables restored: $TABLE_COUNT"

if [ "$TABLE_COUNT" -gt 0 ]; then
    success "Restore completed and verified"
else
    warning "Database restored but no tables found. Please verify manually."
fi

exit 0
