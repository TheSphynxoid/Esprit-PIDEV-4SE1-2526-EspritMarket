#!/bin/bash
set -e

echo "========================================"
echo "  Start Development Environment"
echo "========================================"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEVOPS_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Starting all services with Docker Compose..."
echo ""

# Check for .env file
if [ ! -f "$DEVOPS_ROOT/../.env" ]; then
    echo "WARNING: .env file not found. Creating from template..."
    cp "$DEVOPS_ROOT/.env.example" "$DEVOPS_ROOT/../.env" 2>/dev/null || echo "Please create .env file manually"
fi

# Start services
cd "$DEVOPS_ROOT"
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build -d

echo ""
echo "========================================"
echo "  Services Starting..."
echo "========================================"
echo ""
echo "Services will be available at:"
echo "  Frontend:    http://localhost:4200"
echo "  Backend:     http://localhost:8088"
echo "  Prometheus:  http://localhost:9090"
echo "  Grafana:     http://localhost:3000 (admin/\$GRAFANA_PASSWORD)"
echo "  PostgreSQL:  localhost:5432"
echo ""
echo "Debug port (backend): localhost:5005"
echo ""
echo "Useful commands:"
echo "  View logs:    docker compose -f $SCRIPT_DIR/../docker-compose.yml -f $SCRIPT_DIR/../docker-compose.dev.yml logs -f"
echo "  Stop:         docker compose -f $SCRIPT_DIR/../docker-compose.yml -f $SCRIPT_DIR/../docker-compose.dev.yml down"
echo ""
echo "After backend starts, generate types:"
echo "  ./scripts/generate-types.sh"
echo ""
