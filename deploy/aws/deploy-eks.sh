#!/usr/bin/env bash
set -euo pipefail

CLUSTER="loan-management-eks"
REGION="ap-south-1"
NAMESPACE="loan-management"
KUBECONFIG="${KUBECONFIG:-$HOME/.kube/eks-config}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export KUBECONFIG

echo "==> Updating kubeconfig"
mkdir -p "$(dirname "$KUBECONFIG")"
aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER" --kubeconfig "$KUBECONFIG" >/dev/null

echo "==> Checking cluster"
kubectl get nodes -o wide
echo "==> Checking managed node group"
NODEGROUP="loan-management-ng"
NODEGROUP_STATUS="$(aws eks describe-nodegroup --cluster-name "$CLUSTER" --nodegroup-name "$NODEGROUP" --region "$REGION" --query 'nodegroup.status' --output text 2>/dev/null || true)"
if [[ "$NODEGROUP_STATUS" != "ACTIVE" ]]; then
  echo "ERROR: node group $NODEGROUP is not ACTIVE (status: ${NODEGROUP_STATUS:-NOT_FOUND})" >&2
  exit 1
fi
# t3.micro nodes have very limited pod capacity; keep the current 4-node baseline
# so the AWS Load Balancer Controller and application workloads can be scheduled.
aws eks update-nodegroup-config \
  --cluster-name "$CLUSTER" \
  --nodegroup-name "$NODEGROUP" \
  --scaling-config minSize=4,maxSize=4,desiredSize=4 \
  --region "$REGION" >/dev/null

echo "==> Waiting for worker capacity"
for i in {1..60}; do
  READY="$(kubectl get nodes --no-headers 2>/dev/null | awk '$2=="Ready"{c++} END{print c+0}')"
  if [[ "$READY" -ge 2 ]]; then
    break
  fi
  sleep 10
done
kubectl get nodes -o wide

echo "==> Applying AWS runtime infrastructure"
kubectl apply -f "$ROOT/k8s/namespace.yaml"
kubectl apply -f "$ROOT/k8s/redis.yaml"
kubectl apply -f "$ROOT/k8s/kafka.yaml"
kubectl apply -f "$ROOT/k8s/aws/app-secret-provider.yaml"
kubectl apply -f "$ROOT/k8s/aws/secrets-serviceaccount.yaml"
kubectl apply -f "$ROOT/k8s/aws/secret-sync.yaml"

echo "==> Waiting for secrets, Redis, and Kafka"
kubectl -n "$NAMESPACE" rollout status deployment/secret-sync --timeout=180s
for i in {1..30}; do
  if kubectl -n "$NAMESPACE" get secret loan-management-secrets >/dev/null 2>&1; then break; fi
  sleep 5
done
kubectl -n "$NAMESPACE" get secret loan-management-secrets >/dev/null
kubectl -n "$NAMESPACE" rollout status deployment/redis --timeout=180s
kubectl -n "$NAMESPACE" rollout status deployment/zookeeper --timeout=180s
kubectl -n "$NAMESPACE" rollout status deployment/kafka --timeout=240s

echo "==> Applying application"
kubectl apply -k "$ROOT/k8s/aws"

echo "==> Waiting for deployments"
kubectl -n "$NAMESPACE" get deployments
kubectl -n "$NAMESPACE" rollout status deployment/discovery-server --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/auth-service --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/api-gateway --timeout=300s

echo "==> Application status"
kubectl -n "$NAMESPACE" get pods -o wide
kubectl -n "$NAMESPACE" get ingress loan-management -o wide

echo "==> ALB hostname"
kubectl -n "$NAMESPACE" get ingress loan-management -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'; echo
