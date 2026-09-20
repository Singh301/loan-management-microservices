#!/usr/bin/env bash
set -euo pipefail

CLUSTER="loan-management-cluster"
REGION="ap-south-1"
NAMESPACE="loan-management"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "==> Updating kubeconfig"
aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER" >/dev/null

echo "==> Checking cluster"
kubectl get nodes -o wide

echo "==> Ensuring at least two worker nodes"
MEDIUM_STATUS="$(aws eks describe-nodegroup --cluster-name "$CLUSTER" --nodegroup-name loan-management-workers-medium --region "$REGION" --query 'nodegroup.status' --output text 2>/dev/null || true)"
if [[ "$MEDIUM_STATUS" == "ACTIVE" ]]; then
  echo "    medium nodegroup is ACTIVE"
else
  echo "    medium nodegroup status: ${MEDIUM_STATUS:-NOT_FOUND}; scaling existing worker group to 2 nodes"
  aws eks update-nodegroup-config     --cluster-name "$CLUSTER"     --nodegroup-name loan-management-workers     --scaling-config minSize=2,maxSize=2,desiredSize=2     --region "$REGION" >/dev/null
fi

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
kubectl apply -f "$ROOT/k8s/aws/secret-sync.yaml"

echo "==> Waiting for Redis/Kafka"
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
