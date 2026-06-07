#!/bin/bash
set -e

echo "========================================"
echo "  Esprit Market Workspace Setup"
echo "========================================"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WORKSPACE_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo "Workspace root: $WORKSPACE_ROOT"

if [ ! -d "$WORKSPACE_ROOT/backend" ]; then
    echo "ERROR: backend/ folder not found"
    echo "Please ensure the backend repo is cloned/moved to: $WORKSPACE_ROOT/backend"
    exit 1
fi

if [ ! -d "$WORKSPACE_ROOT/frontend" ]; then
    echo "ERROR: frontend/ folder not found"
    echo "Please ensure the frontend repo is cloned/moved to: $WORKSPACE_ROOT/frontend"
    exit 1
fi

echo "✓ Backend found at: $WORKSPACE_ROOT/backend"
echo "✓ Frontend found at: $WORKSPACE_ROOT/frontend"

echo ""
echo "========================================"
echo "  Installing Backend Dependencies"
echo "========================================"
cd "$WORKSPACE_ROOT/backend"
if [ -f "./mvnw" ]; then
    ./mvnw dependency:go-offline -B || echo "Warning: Some dependencies may not be cached"
else
    echo "Maven wrapper not found, skipping..."
fi

if [ -f "./mvnw.cmd" ]; then
    ./mvnw.cmd dependency:go-offline -B || echo "Warning: Some dependencies may not be cached"
else
    echo "Maven wrapper not found, skipping..."
fi

echo ""
echo "========================================"
echo "  Installing Frontend Dependencies"
echo "========================================"
cd "$WORKSPACE_ROOT/frontend"
if [ -f "package.json" ]; then
    npm install || echo "Warning: npm install had issues"
else
    echo "package.json not found, skipping..."
fi

echo ""
echo "========================================"
echo "  Installing DevOps Dependencies"
echo "========================================"
cd "$WORKSPACE_ROOT/devops/shared/api-types"
if [ ! -f "package.json" ]; then
    echo '{"name": "@esprit-market/api-types", "version": "1.0.0", "main": "src/generated/index.ts", "types": "src/generated/index.ts"}' > package.json
fi
npm install || echo "Warning: npm install had issues"

echo ""
echo "Installing openapi-generator-cli..."
npm install -g @openapitools/openapi-generator-cli || echo "Warning: Could not install openapi-generator-cli globally"

echo ""
if [ ! -f "$WORKSPACE_ROOT/.env" ]; then
    echo "Creating .env file from template..."
    cat > "$WORKSPACE_ROOT/.env" << 'ENVEOF'
# Database
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-secure-password-here

# Application Secrets
JWT_SECRET=your-jwt-secret-key-at-least-256-bits-long
GOOGLE_MAPS_API_KEY=

# Monitoring
GRAFANA_PASSWORD=admin
ENVEOF
    echo "✓ Created .env file - Please update with your values!"
else
    echo "✓ .env file already exists"
fi

echo ""
echo "========================================"
echo "  Setup Complete!"
echo "========================================"
echo ""
echo "Next steps:"
echo "  1. Edit $WORKSPACE_ROOT/.env with your secrets"
echo "  2. Run: ./scripts/start-dev.sh"
echo "  3. Run: ./scripts/generate-types.sh (after backend starts)"
echo ""
