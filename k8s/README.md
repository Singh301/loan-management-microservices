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

Apply network policies before application workloads:

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/network-policies.yaml
kubectl apply -f k8s/services.yaml
kubectl apply -f k8s/loan-services.yaml
kubectl apply -f k8s/support-services.yaml
```

The network policy baseline denies all ingress and egress in the application namespace, then allows same-namespace traffic and DNS lookups. Add narrowly scoped policies when external dependencies or ingress controllers are introduced.

Development MySQL persistence is provided by the `mysql-data` PVC using the k3s `local-path` storage class. This is node-local development storage, not a production HA database. For AWS production, use RDS with automated backups, multi-AZ configuration as required, and managed failover rather than this manifest.

## 4. Verify

```bash
kubectl -n loan-management get pods
kubectl -n loan-management get svc
kubectl -n loan-management get hpa
kubectl -n loan-management rollout status deployment/loan-service
```

The API gateway is a `ClusterIP` service by default. Keep application services internal and add an ingress/load-balancer layer explicitly for environments that require external access.

### Autoscaling and metrics

The loan service HPA scales on CPU and memory. The API gateway HPA scales on CPU and memory. Both use conservative scale-up and scale-down behavior to reduce oscillation.

An HPA requires a Kubernetes resource-metrics provider. The low-memory k3s EC2 profile used for development disables `metrics-server` to reduce RAM usage, so HPA metrics are intentionally unavailable there. A normal shared or production cluster should enable a supported metrics provider before relying on these HPAs.

Application metrics are exposed by Spring Boot Actuator/Prometheus on the services that configure the `prometheus` endpoint. Use a monitoring stack such as Prometheus/Grafana or an equivalent managed platform in the target environment.

Servlet-based services use the shared `RequestIdFilter` to place `X-Request-Id` into the SLF4J MDC as `requestId` and return the same identifier in the response. This allows structured logs to be correlated across service boundaries. The API Gateway already propagates `X-Request-Id` on routed requests.

### Distributed tracing

The services use Micrometer Tracing with an OpenTelemetry bridge and W3C trace-context propagation. Trace sampling defaults to 10%. OTLP export is disabled by default to keep local development self-contained; enable it by setting `OTEL_TRACING_EXPORT_ENABLED=true` and providing `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` for an OpenTelemetry Collector or compatible backend.

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
