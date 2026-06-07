#!/bin/bash
set -euo pipefail

# ============================================
# Storage Setup Script for Esprit Market
# Initializes persistent storage on K8s nodes
# ============================================

STORAGE_PATH="/mnt/data/esprit-market/uploads"
STORAGE_UID=1000
STORAGE_GID=1000

echo "=== Setting up persistent storage for image uploads ==="

# Get the node name dynamically
if [ -z "${KUBE_NODE_NAME:-}" ]; then
    echo "Detecting Kubernetes node name..."
    KUBE_NODE_NAME=$(hostname)
    echo "Using node: $KUBE_NODE_NAME"
fi

# Create storage directory
echo "Creating storage directory: $STORAGE_PATH"
mkdir -p "$STORAGE_PATH"

# Set proper permissions for UID 1000 (app user in container)
echo "Setting permissions..."
chown -R $STORAGE_UID:$STORAGE_GID "$STORAGE_PATH"
chmod 755 "$STORAGE_PATH"

# Verify
if [ -d "$STORAGE_PATH" ]; then
    echo "✓ Storage directory created successfully"
    ls -la "$STORAGE_PATH"
else
    echo "✗ Failed to create storage directory"
    exit 1
fi

# Update the PV manifest with the correct node name
echo ""
echo "Updating PersistentVolume manifest with node name: $KUBE_NODE_NAME"

PV_MANIFEST="devops/k8s/backend/uploads-pvc.yaml"
if [ -f "$PV_MANIFEST" ]; then
    # Replace the placeholder node name with actual node
    sed -i "s/node1/$KUBE_NODE_NAME/g" "$PV_MANIFEST"
    echo "✓ Updated $PV_MANIFEST"
else
    echo "⚠ Warning: Could not find $PV_MANIFEST. Please update manually."
    echo "  Edit $PV_MANIFEST and replace 'node1' with: $KUBE_NODE_NAME"
fi

echo ""
echo "=== Storage setup complete! ==="
echo ""
echo "Storage location: $STORAGE_PATH"
echo "Node: $KUBE_NODE_NAME"
echo ""
echo "Next steps:"
echo "1. Deploy the application: kubectl apply -k devops/k8s/overlays/single-node/"
echo "2. Verify PVC is bound: kubectl get pvc esprit-market-uploads-pvc -n esprit-market"
