# Kubernetes Deployment Guide (kubeadm)

This guide covers deploying Esprit Market to a VPS using kubeadm.

## Prerequisites

- Ubuntu 22.04+ VPS
- At least 4GB RAM, 2 vCPUs recommended
- Domain pointing to VPS IP: `esprit.thesphynx.net`

---

## Step 1: Initialize Kubernetes Cluster

Run these commands on your VPS:

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Disable swap
sudo swapoff -a
sudo sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab

# Load kernel modules
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF
sudo modprobe overlay
sudo modprobe br_netfilter

# Sysctl params
cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF
sudo sysctl --system

# Install containerd
sudo apt install -y containerd
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
sudo systemctl restart containerd
sudo systemctl enable containerd

# Install Kubernetes
KUBERNETES_VERSION="1.31"
curl -fsSL https://pkgs.k8s.io/core:/stable:/v${KUBERNETES_VERSION}/deb/Release.key | \
  sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] \
  https://pkgs.k8s.io/core:/stable:/v${KUBERNETES_VERSION}/deb/ /" | \
  sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt update
sudo apt install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
sudo systemctl enable kubelet

# Initialize cluster
sudo kubeadm init --pod-network-cidr=10.244.0.0/16

# Configure kubectl
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# Install Calico CNI
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.28.0/manifests/calico.yaml

# Single-node: remove master taint
kubectl taint nodes --all node-role.kubernetes.io/control-plane-

# Install local-path-provisioner for storage
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/v0.0.24/deploy/local-path-storage.yaml
kubectl patch storageclass local-path -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.2/deploy/static/provider/cloud/deploy.yaml

# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.15.0/cert-manager.yaml

# Wait for ingress to be ready
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s
```

---

## Step 2: Clone & Deploy Application

```bash
# Clone repository
git clone https://github.com/your-repo/esprit-market.git
cd esprit-market

# Setup persistent storage for image uploads (IMPORTANT!)
sudo ./devops/scripts/setup-storage.sh

# Navigate to devops directory
cd devops

# Create secrets
./scripts/create-secrets.sh

# Deploy using kustomize (single-node overlay)
kubectl apply -k k8s/overlays/single-node/

# Or for full deployment (multi-node)
# kubectl apply -k k8s/
```

---

## Storage Configuration

The application needs persistent storage for product image uploads. The setup script automatically:

1. Creates `/mnt/data/esprit-market/uploads` directory on the node
2. Sets proper permissions for the container user (UID 1000)
3. Updates the PersistentVolume manifest with your node's hostname

**If you need to run the storage setup script later:**
```bash
sudo ./devops/scripts/setup-storage.sh
```

**To verify storage is working:**
```bash
# Check PersistentVolumeClaim is bound
kubectl get pvc -n esprit-market

# Check the pod can write to the volume
POD=$(kubectl get pods -n esprit-market -l app=esprit-market-backend -o jsonpath='{.items[0].metadata.name}')
kubectl exec -it $POD -n esprit-market -- ls -la /app/uploads
```

---

## Step 3: Configure DNS & Access

Get the LoadBalancer IP or configure NodePort:

```bash
# Check ingress service
kubectl get svc -n ingress-nginx

# For bare-metal with NodePort, patch the ingress service:
kubectl patch svc ingress-nginx-controller -n ingress-nginx -p '{"spec": {"type": "LoadBalancer"}}'
```

If your VPS doesn't support LoadBalancer, use NodePort:

```bash
# Get NodePort
kubectl get svc ingress-nginx-controller -n ingress-nginx
```

Then configure your domain DNS or firewall:

```bash
# Allow HTTP/HTTPS through firewall
sudo ufw allow 80
sudo ufw allow 443
sudo ufw allow 6443  # Kubernetes API (optional, for remote kubectl)
sudo ufw enable
```

---

## Step 4: Verify Deployment

```bash
# Check all pods
kubectl get pods -n esprit-market

# Check services
kubectl get svc -n esprit-market

# Check ingress
kubectl get ingress -n esprit-market

# Check certificate
kubectl get certificate -n esprit-market

# View logs
kubectl logs -f deployment/esprit-market-backend -n esprit-market
```

---

## Step 5: Access Application

- **Frontend:** https://esprit.thesphynx.net
- **Backend API:** https://esprit.thesphynx.net/api
- **Swagger UI:** https://esprit.thesphynx.net/swagger-ui.html

---

## Common Commands

```bash
# Scale deployments
kubectl scale deployment esprit-market-backend -n esprit-market --replicas=2

# Update image
kubectl set image deployment/esprit-market-backend \
  backend=thesphynx2000/espritmarket-backend:new-tag \
  -n esprit-market

# View resource usage
kubectl top pods -n esprit-market

# Describe pod for troubleshooting
kubectl describe pod <pod-name> -n esprit-market

# Port forward for debugging
kubectl port-forward svc/esprit-market-backend 8088:8088 -n esprit-market
```

---

## Troubleshooting

### Pods stuck in Pending
```bash
kubectl describe pod <pod-name> -n esprit-market
# Usually storage or resource issues
```

### Certificate not issued
```bash
kubectl describe certificate esprit-market-tls -n esprit-market
kubectl logs -n cert-manager -l app=cert-manager
```

### Ingress not working
```bash
kubectl describe ingress esprit-market-ingress -n esprit-market
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller
```

### Reset cluster
```bash
sudo kubeadm reset
sudo rm -rf /etc/kubernetes /var/lib/etcd
```
