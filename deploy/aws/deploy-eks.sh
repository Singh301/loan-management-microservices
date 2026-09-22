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
aws eks update-kubeconfig \
  --region "$REGION" \
  --name "$CLUSTER" \
  --kubeconfig "$KUBECONFIG" >/dev/null

echo "==> Checking cluster"
kubectl get nodes -o wide

echo "==> Checking primary managed node group"
NODEGROUP="loan-management-ng"

NODEGROUP_STATUS="$(aws eks describe-nodegroup \
  --cluster-name "$CLUSTER" \
  --nodegroup-name "$NODEGROUP" \
  --region "$REGION" \
  --query 'nodegroup.status' \
  --output text 2>/dev/null || true)"

if [[ "$NODEGROUP_STATUS" != "ACTIVE" ]]; then
  echo "ERROR: node group $NODEGROUP is not ACTIVE (status: ${NODEGROUP_STATUS:-NOT_FOUND})" >&2
  exit 1
fi

echo "==> Checking worker capacity"

READY=0

for i in {1..60}; do
  READY="$(kubectl get nodes --no-headers 2>/dev/null | awk '$2=="Ready"{c++} END{print c+0}')"

  if [[ "$READY" -ge 4 ]]; then
    break
  fi

  sleep 10
done

if [[ "$READY" -lt 4 ]]; then
  echo "ERROR: expected at least 4 Ready nodes, found $READY" >&2
  exit 1
fi

kubectl get nodes -o wide

echo "==> Validating AWS manifests"
kubectl kustomize "$ROOT/k8s/aws" >/dev/null

echo "==> Applying AWS production overlay"
kubectl apply -k "$ROOT/k8s/aws"

echo "==> Waiting for ExternalSecret"

kubectl -n "$NAMESPACE" wait \
  --for=condition=Ready \
  externalsecret/loan-management-secrets \
  --timeout=180s

kubectl -n "$NAMESPACE" get secret loan-management-secrets >/dev/null

echo "==> Waiting for application deployments"

kubectl -n "$NAMESPACE" rollout status deployment/discovery-server --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/auth-service --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/api-gateway --timeout=300s

echo "==> Application status"

kubectl -n "$NAMESPACE" get deployments
kubectl -n "$NAMESPACE" get pods -o wide

echo "==> Ingress status"

kubectl -n "$NAMESPACE" get ingress loan-management -o wide

echo "==> ALB hostname"

kubectl -n "$NAMESPACE" get ingress loan-management \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'

echo