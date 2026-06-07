#!/bin/bash
set -euo pipefail

# ============================================
# Esprit Market - VPS Setup Script for kubeadm
# Run as root on a fresh Ubuntu VPS
# ============================================

DOMAIN="esprit.thesphynx.net"
EMAIL="admin@thesphynx.net"

echo "=== Updating system ==="
apt update && apt upgrade -y

echo "=== Installing dependencies ==="
apt install -y curl wget apt-transport-https ca-certificates gnupg lsb-release software-properties-common conntrack

echo "=== Disabling swap ==="
swapoff -a
sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab

echo "=== Loading kernel modules ==="
cat <<EOF | tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF
modprobe overlay
modprobe br_netfilter

echo "=== Setting sysctl params ==="
cat <<EOF | tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF
sysctl --system

echo "=== Installing containerd ==="
apt install -y containerd
mkdir -p /etc/containerd
containerd config default | tee /etc/containerd/config.toml
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
systemctl restart containerd
systemctl enable containerd

echo "=== Installing Kubernetes (kubeadm, kubelet, kubectl) ==="
KUBERNETES_VERSION="1.31"

curl -fsSL https://pkgs.k8s.io/core:/stable:/v${KUBERNETES_VERSION}/deb/Release.key | gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
chmod 644 /etc/apt/keyrings/kubernetes-apt-keyring.gpg

echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v${KUBERNETES_VERSION}/deb/ /" | tee /etc/apt/sources.list.d/kubernetes.list

apt update
apt install -y kubelet kubeadm kubectl
apt-mark hold kubelet kubeadm kubectl
systemctl enable kubelet

echo "=== Initializing Kubernetes cluster ==="
kubeadm init --pod-network-cidr=10.244.0.0/16 --cri-socket unix:///var/run/containerd/containerd.sock

echo "=== Setting up kubectl for current user ==="
mkdir -p $HOME/.kube
cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
chown $(id -u):$(id -g) $HOME/.kube/config

echo "=== Installing Calico CNI ==="
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/v3.28.0/manifests/calico.yaml

echo "=== Removing master taint (single-node cluster) ==="
kubectl taint nodes --all node-role.kubernetes.io/control-plane-

echo "=== Installing local-path-provisioner for storage ==="
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/v0.0.24/deploy/local-path-storage.yaml
kubectl patch storageclass local-path -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

echo ""
echo "=== Setting up persistent storage for uploads ==="
# Get the actual node hostname
NODE_NAME=$(kubectl get nodes -o jsonpath='{.items[0].metadata.name}')
export KUBE_NODE_NAME=$NODE_NAME
# Note: setup-storage.sh needs to be run from the devops directory
# It will be run after git clone in the deployment instructions

echo "=== Installing NGINX Ingress Controller ==="
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.2/deploy/static/provider/baremetal/deploy.yaml

echo "=== Waiting for NGINX Ingress to be ready ==="
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s

echo "=== Installing cert-manager ==="
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.15.0/cert-manager.yaml

echo "=== Waiting for cert-manager to be ready ==="
kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager -n cert-manager
kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager-webhook -n cert-manager

echo "=== Firewall setup ==="
ufw allow 22
ufw allow 80
ufw allow 443
ufw allow 6443
ufw --force enable

echo ""
echo "=== Setup Complete! ==="
echo ""
echo "Next steps:"
echo "1. Clone the repo: git clone https://github.com/your-repo/esprit-market.git"
echo "2. cd esprit-market"
echo "3. Setup persistent storage: sudo ./devops/scripts/setup-storage.sh"
echo "4. Create secrets: cd devops && ./scripts/create-secrets.sh"
echo "5. Deploy: kubectl apply -k devops/k8s/overlays/single-node/"
echo ""
echo "To get the NodePort for ingress:"
echo "  kubectl get svc -n ingress-nginx"
