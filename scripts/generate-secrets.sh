#!/bin/bash

#############################################
# Security Secrets Generator
#############################################
# Generates cryptographically secure random
# secrets for production deployment.
#
# Usage:
#   ./scripts/generate-secrets.sh
#
# Requirements:
#   - openssl (usually pre-installed)
#   - bash
#############################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo "╔═══════════════════════════════════════════════════════╗"
echo "║  Security Secrets Generator                           ║"
echo "║  URL Shortener Platform                               ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# Check if openssl is installed
if ! command -v openssl &> /dev/null; then
    echo -e "${RED}Error: openssl is not installed.${NC}"
    echo ""
    echo "Please install openssl:"
    echo "  - Ubuntu/Debian: sudo apt-get install openssl"
    echo "  - macOS: brew install openssl (usually pre-installed)"
    echo "  - Windows: Use Git Bash or WSL"
    exit 1
fi

echo -e "${BLUE}Generating secure random secrets...${NC}"
echo ""

# Generate JWT Secret (256 bits = 32 bytes, encoded as base64 = 43 characters)
JWT_SECRET=$(openssl rand -base64 32)

# Generate Postgres Password (20 characters alphanumeric)
POSTGRES_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 24)

# Generate Redis Password (20 characters alphanumeric)
REDIS_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 24)

# Display results
echo "═══════════════════════════════════════════════════════"
echo -e "${GREEN}✓ Secrets Generated Successfully${NC}"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Copy these values to your .env file or set as environment variables:"
echo ""

echo -e "${YELLOW}JWT_SECRET${NC}"
echo "  $JWT_SECRET"
echo ""

echo -e "${YELLOW}POSTGRES_PASSWORD${NC}"
echo "  $POSTGRES_PASSWORD"
echo ""

echo -e "${YELLOW}REDIS_PASSWORD${NC}"
echo "  $REDIS_PASSWORD"
echo ""

echo "═══════════════════════════════════════════════════════"
echo ""

# Offer to create .env file
read -p "Would you like to create/update .env file with these secrets? (y/N): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    ENV_FILE=".env"

    if [ -f "$ENV_FILE" ]; then
        echo -e "${YELLOW}Warning: .env file already exists.${NC}"
        read -p "Backup existing .env to .env.backup? (Y/n): " -n 1 -r
        echo ""

        if [[ ! $REPLY =~ ^[Nn]$ ]]; then
            cp "$ENV_FILE" ".env.backup"
            echo -e "${GREEN}✓ Backup created: .env.backup${NC}"
        fi
    fi

    # Check if .env.example exists
    if [ -f ".env.example" ]; then
        # Copy .env.example to .env
        cp .env.example "$ENV_FILE"
        echo -e "${GREEN}✓ Copied .env.example to .env${NC}"

        # Replace secrets in .env file (macOS and Linux compatible)
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            sed -i '' "s|JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|" "$ENV_FILE"
            sed -i '' "s|POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$POSTGRES_PASSWORD|" "$ENV_FILE"
            sed -i '' "s|REDIS_PASSWORD=.*|REDIS_PASSWORD=$REDIS_PASSWORD|" "$ENV_FILE"
            sed -i '' "s|DATABASE_PASSWORD=.*|DATABASE_PASSWORD=$POSTGRES_PASSWORD|" "$ENV_FILE"
        else
            # Linux
            sed -i "s|JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|" "$ENV_FILE"
            sed -i "s|POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$POSTGRES_PASSWORD|" "$ENV_FILE"
            sed -i "s|REDIS_PASSWORD=.*|REDIS_PASSWORD=$REDIS_PASSWORD|" "$ENV_FILE"
            sed -i "s|DATABASE_PASSWORD=.*|DATABASE_PASSWORD=$POSTGRES_PASSWORD|" "$ENV_FILE"
        fi

        echo -e "${GREEN}✓ Updated $ENV_FILE with secure secrets${NC}"
    else
        echo -e "${RED}Error: .env.example not found. Cannot create .env file.${NC}"
        exit 1
    fi
else
    echo "Skipped .env file creation."
fi

echo ""
echo "═══════════════════════════════════════════════════════"
echo -e "${GREEN}IMPORTANT SECURITY NOTES:${NC}"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "1. ${YELLOW}NEVER commit .env file to version control${NC}"
echo "   - Already in .gitignore, but double-check"
echo ""
echo "2. ${YELLOW}Store secrets securely in production:${NC}"
echo "   - Use AWS Secrets Manager"
echo "   - Use HashiCorp Vault"
echo "   - Use Docker Secrets"
echo "   - Use Kubernetes Secrets"
echo ""
echo "3. ${YELLOW}Rotate secrets regularly:${NC}"
echo "   - JWT secrets: every 6 months"
echo "   - Database passwords: every 6-12 months"
echo "   - After any security incident: immediately"
echo ""
echo "4. ${YELLOW}Use different secrets for each environment:${NC}"
echo "   - Development: can be simpler (but still not default)"
echo "   - Staging: should be production-strength"
echo "   - Production: must be cryptographically random"
echo ""
echo "═══════════════════════════════════════════════════════"
echo ""
echo "For more information, see: docs/PRODUCTION_SECRETS.md"
echo ""
