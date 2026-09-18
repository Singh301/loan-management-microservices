# Kubernetes deployment

## 1. Build and publish images

The Jenkins pipeline builds each service image and publishes it to the configured container registry.

Update the image registry in `Jenkinsfile` and `k8s/loan-services.yaml` before deployment.

## 2. Create secrets

Do not commit real credentials and do not apply `k8s/secret-template.yaml`. That file is only a placeholder/example.

Create the Kubernetes Secret from your secure environment before running the Jenkins deployment:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl -n loan-management create secret generic loan-management-secrets \
  --from-literal=DB_USERNAME="$DB_USERNAME" \
  --from-literal=DB_PASSWORD="$DB_PASSWORD" \
  --from-literal=JWT_SECRET="$JWT_SECRET"
```

For an existing secret, update it rather than creating a second copy:

```bash
kubectl -n loan-management create secret generic loan-management-secrets \
  --from-literal=DB_USERNAME="$DB_USERNAME" \
  --from-literal=DB_PASSWORD="$DB_PASSWORD" \
  --from-literal=JWT_SECRET="$JWT_SECRET" \
  --dry-run=client -o yaml | kubectl apply -f -
```

The Jenkins pipeline does not create or print secret values. Before deployment it verifies that `loan-management-secrets` exists and contains the required keys: `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`. 

## 3. Deploy

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/services.yaml
kubectl apply -f k8s/loan-services.yaml
```

## 4. Verify

```bash
kubectl -n loan-management get pods
kubectl -n loan-management get svc
kubectl -n loan-management get hpa
kubectl -n loan-management rollout status deployment/loan-service
```

The API gateway is exposed through a Kubernetes `LoadBalancer` service.

## AWS target architecture

For AWS, replace local infrastructure with managed services where appropriate:

- EKS for Kubernetes
- ECR for container images
- RDS MySQL for databases
- ElastiCache Redis for caching
- MSK for Kafka
- S3 for documents
- Secrets Manager for credentials
- CloudWatch for infrastructure/application logs and alarms

The repository intentionally keeps AWS resource provisioning separate from application manifests so cloud credentials and account-specific networking are not committed to source control.
