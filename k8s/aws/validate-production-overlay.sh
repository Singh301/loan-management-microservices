#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if grep -R -n --exclude='validate-production-overlay.sh' 'REPLACE_WITH_' "$root"; then
  echo "ERROR: AWS production overlay still contains REPLACE_WITH_* placeholders."
  exit 1
fi

if grep -R -n --exclude='validate-production-overlay.sh' 'change-me' "$root"; then
  echo "ERROR: AWS production overlay contains placeholder credentials."
  exit 1
fi

kubectl kustomize "$root" >/tmp/loan-management-aws-manifests.yaml

grep -q 'kind: Ingress' /tmp/loan-management-aws-manifests.yaml
grep -q 'name: document-service-aws' /tmp/loan-management-aws-manifests.yaml
grep -q 'DOCUMENT_STORAGE_TYPE' /tmp/loan-management-aws-manifests.yaml

echo "AWS production overlay validation passed."
