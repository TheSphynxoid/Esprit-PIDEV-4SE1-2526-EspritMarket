#!/bin/bash
set -e

echo "========================================"
echo "  Generate TypeScript Types from API"
echo "========================================"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEVOPS_ROOT="$(dirname "$SCRIPT_DIR")"
WORKSPACE_ROOT="$(dirname "$DEVOPS_ROOT")"
API_TYPES_DIR="$DEVOPS_ROOT/shared/api-types"
OPENAPI_FILE="$API_TYPES_DIR/openapi.json"
K8S_NAMESPACE="esprit-market"
K8S_SERVICE="esprit-market-backend"
K8S_PORT=8088
LOCAL_PORT=8088

echo "Workspace root: $WORKSPACE_ROOT"
echo "DevOps root: $DEVOPS_ROOT"

cleanup_port_forward() {
    if [ -n "$PORT_FORWARD_PID" ] && kill -0 "$PORT_FORWARD_PID" 2>/dev/null; then
        echo ""
        echo "Stopping kubectl port-forward (PID: $PORT_FORWARD_PID)..."
        kill "$PORT_FORWARD_PID" 2>/dev/null || true
        wait "$PORT_FORWARD_PID" 2>/dev/null || true
    fi
}
trap cleanup_port_forward EXIT

BACKEND_URL=""
PORT_FORWARD_PID=""

detect_backend() {
    if curl -sf http://localhost:$LOCAL_PORT/actuator/health > /dev/null 2>&1; then
        echo "✓ Backend detected on localhost:$LOCAL_PORT (local/docker)"
        BACKEND_URL="http://localhost:$LOCAL_PORT"
        return 0
    fi

    if command -v kubectl &> /dev/null && kubectl get svc "$K8S_SERVICE" -n "$K8S_NAMESPACE" > /dev/null 2>&1; then
        echo "✓ Backend detected in Kubernetes ($K8S_NAMESPACE/$K8S_SERVICE)"
        echo "  Starting port-forward on localhost:$LOCAL_PORT..."
        kubectl port-forward svc/$K8S_SERVICE $LOCAL_PORT:$K8S_PORT -n "$K8S_NAMESPACE" > /dev/null 2>&1 &
        PORT_FORWARD_PID=$!

        local retries=0
        while ! curl -sf http://localhost:$LOCAL_PORT/actuator/health > /dev/null 2>&1; do
            retries=$((retries + 1))
            if [ $retries -ge 30 ]; then
                echo "ERROR: Timed out waiting for port-forward to become ready"
                exit 1
            fi
            sleep 1
        done

        echo "✓ Port-forward active (PID: $PORT_FORWARD_PID)"
        BACKEND_URL="http://localhost:$LOCAL_PORT"
        return 0
    fi

    return 1
}

echo ""
echo "Detecting backend..."
if ! detect_backend; then
    echo "ERROR: Backend not reachable"
    echo ""
    echo "Options:"
    echo "  1. Local:    cd $WORKSPACE_ROOT/backend && ./mvnw spring-boot:run"
    echo "  2. Docker:   docker compose -f $DEVOPS_ROOT/docker-compose.yml -f $DEVOPS_ROOT/docker-compose.dev.yml up -d backend"
    echo "  3. K8s:      Ensure kubectl can access the $K8S_NAMESPACE namespace"
    exit 1
fi

echo ""
echo "Fetching OpenAPI specification..."
HTTP_STATUS=$(curl -s -o "$OPENAPI_FILE" -w "%{http_code}" "$BACKEND_URL/v3/api-docs")

if [ "$HTTP_STATUS" != "200" ]; then
    echo "ERROR: Failed to fetch OpenAPI spec (HTTP $HTTP_STATUS)"
    echo ""
    echo "Response:"
    cat "$OPENAPI_FILE"
    echo ""
    echo ""
    echo "This usually means:"
    echo "  - The backend has a springdoc dependency issue (check logs)"
    echo "  - The backend hasn't fully started yet"
    echo ""
    echo "Check backend logs:"
    echo "  kubectl logs deployment/$K8S_SERVICE -n $K8S_NAMESPACE --tail=50"
    exit 1
fi

SPEC_SIZE=$(wc -c < "$OPENAPI_FILE")
echo "✓ OpenAPI spec saved to: $OPENAPI_FILE ($SPEC_SIZE bytes)"

# Check if openapi-generator-cli is available
if ! command -v openapi-generator-cli &> /dev/null; then
    echo ""
    echo "openapi-generator-cli not found, installing..."
    npm install -g @openapitools/openapi-generator-cli
fi

# Ensure output directory exists
mkdir -p "$API_TYPES_DIR/src/generated"

# Generate TypeScript types
echo ""
echo "Generating TypeScript types..."
cd "$API_TYPES_DIR"

openapi-generator-cli generate \
    -i "$OPENAPI_FILE" \
    -g typescript-angular \
    -o src/generated \
    --additional-properties=ngVersion=21.0.0,npmName=@esprit-market/api-types,supportsES6=true,withInterfaces=true

echo "✓ TypeScript types generated in: $API_TYPES_DIR/src/generated"

FRONTEND_DIR="$WORKSPACE_ROOT/frontend"
FRONTEND_GENERATED="$FRONTEND_DIR/src/app/generated"

mkdir -p "$FRONTEND_GENERATED/model"
cp -r "$API_TYPES_DIR/src/generated/model/"* "$FRONTEND_GENERATED/model/"
echo "✓ Types copied to: $FRONTEND_GENERATED/"

cat > "$FRONTEND_GENERATED/index.ts" << 'EOF'
// Auto-generated API types - DO NOT EDIT
export * from './model/models';
EOF

echo ""
echo "========================================"
echo "  Generation Complete!"
echo "========================================"
echo ""
echo "Generated files:"
echo "  $API_TYPES_DIR/src/generated/"
echo "  $FRONTEND_GENERATED/  (for Docker builds)"
echo ""
echo "Usage in frontend:"
echo '  import { ProductResponse } from "@esprit-market/api-types";'
echo ""
