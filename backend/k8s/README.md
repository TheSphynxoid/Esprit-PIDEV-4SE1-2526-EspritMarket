# Kubernetes Deployment Guide

## Prerequisites

- Kubernetes cluster (v1.28+)
- kubectl configured
- Helm 3.x (optional)
- cert-manager installed
- NGINX Ingress Controller

## Quick Start

```bash
# 1. Create namespace
kubectl apply -f namespace.yaml

# 2. Create secrets (IMPORTANT: Replace placeholders!)
kubectl create secret generic backend-secret \
  --from-literal=SPRING_DATASOURCE_PASSWORD=your-secure-password \
  --from-literal=JWT_SECRET=your-jwt-secret \
  --from-literal=GOOGLE_MAPS_API_KEY=your-api-key \
  -n esprit-market

# 3. Deploy in order
kubectl apply -f backend-configmap.yaml
kubectl apply -f postgres.yaml
kubectl apply -f backend-deployment.yaml
kubectl apply -f backend-service.yaml
kubectl apply -f backend-hpa.yaml
kubectl apply -f backend-pdb.yaml
kubectl apply -f prometheus-config.yaml
kubectl apply -f prometheus.yaml
kubectl apply -f network-policy.yaml
kubectl apply -f cert-issuer.yaml
kubectl apply -f ingress.yaml

# 4. Verify deployment
kubectl get all -n esprit-market
```

## Secret Management Options

### Option 1: Sealed Secrets (Recommended)
```bash
# Install sealed-secrets controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Create sealed secret
kubeseal --format=yaml < backend-secret.yaml > sealed-secret.yaml
```

### Option 2: External Secrets Operator
```bash
# Connect to HashiCorp Vault, AWS Secrets Manager, etc.
# See: https://external-secrets.io
```

### Option 3: SOPS (Secrets OPerationS)
```bash
# Encrypt secrets with GPG/KMS
sops --encrypt --kms "arn:aws:kms:..." backend-secret.yaml > backend-secret.enc.yaml
```

## Production Checklist

- [ ] Replace all placeholder secrets
- [ ] Update TLS hosts in ingress.yaml
- [ ] Configure email in cert-issuer.yaml
- [ ] Adjust resource limits based on load testing
- [ ] Enable backup for PostgreSQL PVC
- [ ] Configure alerting rules for Prometheus
- [ ] Review and adjust NetworkPolicies

## Files Overview

| File | Purpose |
|------|---------|
| `namespace.yaml` | Creates the esprit-market namespace |
| `backend-configmap.yaml` | Application configuration |
| `backend-secret.yaml` | Template for secrets (DO NOT COMMIT WITH REAL VALUES) |
| `backend-deployment.yaml` | Application deployment |
| `backend-service.yaml` | ClusterIP service |
| `backend-hpa.yaml` | Horizontal Pod Autoscaler |
| `backend-pdb.yaml` | Pod Disruption Budget |
| `postgres.yaml` | PostgreSQL StatefulSet with PVC |
| `prometheus.yaml` | Prometheus deployment with persistence |
| `prometheus-config.yaml` | Prometheus scrape configuration |
| `network-policy.yaml` | Network segmentation policies |
| `cert-issuer.yaml` | cert-manager ClusterIssuers |
| `ingress.yaml` | NGINX Ingress with TLS |

## Rollback

```bash
# View rollout history
kubectl rollout history deployment/esprit-market-backend -n esprit-market

# Rollback to previous version
kubectl rollout undo deployment/esprit-market-backend -n esprit-market

# Rollback to specific revision
kubectl rollout undo deployment/esprit-market-backend -n esprit-market --to-revision=2
```

## Monitoring

```bash
# Port-forward Prometheus
kubectl port-forward svc/prometheus 9090:9090 -n esprit-market

# Access: http://localhost:9090
```
