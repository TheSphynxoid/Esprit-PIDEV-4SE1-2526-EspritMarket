#!/bin/bash
set -euo pipefail

NAMESPACE="esprit-market"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../../.env"

if [ -f "$ENV_FILE" ]; then
    echo "=== Loading secrets from $ENV_FILE ==="
    set -a
    source "$ENV_FILE"
    set +a
else
    echo "=== No .env file found, entering interactive mode ==="
    read -sp "PostgreSQL Password: " POSTGRES_PASSWORD
    echo ""
    read -sp "JWT Secret (at least 32 chars): " JWT_SECRET
    echo ""
    read -p "Google Maps API Key (optional, press enter to skip): " GOOGLE_MAPS_API_KEY
    echo ""
fi

if [ -z "${JWT_SECRET:-}" ]; then
    echo "ERROR: JWT_SECRET is required"
    exit 1
fi

if [ -z "${POSTGRES_PASSWORD:-}" ]; then
    echo "ERROR: POSTGRES_PASSWORD is required"
    exit 1
fi

if [ ${#JWT_SECRET} -lt 32 ]; then
    echo "ERROR: JWT_SECRET must be at least 32 characters (got ${#JWT_SECRET})"
    exit 1
fi

echo "=== Creating/updating secrets ==="
kubectl create secret generic backend-secret \
    --namespace=$NAMESPACE \
    --from-literal=SPRING_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD" \
    --from-literal=JWT_SECRET="$JWT_SECRET" \
    --from-literal=GOOGLE_MAPS_API_KEY="${GOOGLE_MAPS_API_KEY:-}" \
    --dry-run=client -o yaml | kubectl apply -f -

echo "=== Secrets created successfully ==="
