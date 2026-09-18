# Loan Management System – Production-Oriented Microservices

Conversion of the original loan-management monolith into a production-oriented microservices architecture.

## Architecture

```text
API Gateway :8080
      |
      +-- Auth :8081
      +-- Customer :8082
      +-- Loan :8083
      +-- Repayment :8084
      +-- Document :8085
      +-- Notification :8086
      +-- Audit :8087
      +-- Dashboard :8088
      |
      +-- Eureka :8761
      +-- MySQL / Redis / Kafka
```

## Services

| Service | Port | Main responsibility |
|---|---:|---|
| discovery-server | 8761 | Eureka service discovery |
| api-gateway | 8080 | Routing, rate limiting, correlation ID |
| auth-service | 8081 | JWT access/refresh tokens, blacklist, lockout |
| customer-service | 8082 | Customer and KYC management |
| loan-service | 8083 | Loan application, approvals, state machine, disbursement, outbox |
| repayment-service | 8084 | EMI schedules, repayments, foreclosure, overdue processing |
| document-service | 8085 | Document upload/download and validation |
| notification-service | 8086 | Kafka-driven notifications |
| audit-service | 8087 | Kafka-driven audit logging |
| dashboard-service | 8088 | Dashboard/analytics APIs |

## Technology

- Java 21
- Spring Boot 3.5.x
- Spring Cloud 2025.0.x
- Spring Security + JJWT 0.12.x
- Spring Data JPA + Flyway + MySQL 8
- Redis
- Apache Kafka + transactional outbox
- Resilience4j
- Micrometer + Prometheus + Grafana
- Docker / Docker Compose
- Kubernetes
- Jenkins + GitHub Actions
- SpringDoc OpenAPI
- Testcontainers

## Local Docker Compose

### Prerequisites

- JDK 21
- Maven 3.9+
- Docker Desktop

### Configure secrets

```bash
copy .env.example .env
```

Edit `.env` and replace every `change-me` value. Do not commit `.env`.

### Build and start the complete stack

```bash
mvn clean verify

docker compose up -d --build
```

### Check the stack

```bash
docker compose ps
docker compose logs -f discovery-server
```

Useful local endpoints:

- Gateway: `http://localhost:8080`
- Eureka: `http://localhost:8761`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Zipkin: `http://localhost:9411`
- Kafka UI: `http://localhost:8090`

## Maven verification

Normal build and tests:

```bash
mvn -B clean verify
```

Quality/security profile:

```bash
mvn -B verify -Pquality
```

The quality profile includes Checkstyle, SpotBugs and OWASP Dependency-Check. The loan service also contains a Testcontainers MySQL integration test.

## Kubernetes

The manifests are under `k8s/`.

Application deployments are provided for all ten services, with readiness/liveness probes, resource requests/limits, and HPAs for the gateway and loan service.

Create a real Kubernetes secret from your deployment environment; do not deploy the committed template with real credentials:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl create secret generic loan-management-secrets \
  -n loan-management \
  --from-literal=DB_USERNAME='<db-user>' \
  --from-literal=DB_PASSWORD='<db-password>' \
  --from-literal=JWT_SECRET='<long-random-secret>'

kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/services.yaml
kubectl apply -f k8s/loan-services.yaml
kubectl apply -f k8s/support-services.yaml
```

For a development Kubernetes environment, `k8s/dev-infrastructure.yaml` supplies MySQL, Redis, ZooKeeper and Kafka. MySQL uses a local-path PVC to preserve data across pod recreation:

```bash
kubectl apply -f k8s/dev-infrastructure.yaml
```

Use that file only for development. For AWS production, map the application configuration to managed RDS, ElastiCache and MSK instead of using the disposable stateful manifests.

## Jenkins

`Jenkinsfile` performs:

1. Maven build and tests
2. Quality/security checks
3. Docker image builds for all ten services
4. GHCR authentication and image push
5. Kubernetes deployment
6. Immutable image-tag rollout using the Jenkins build number
7. Deployment rollout checks and smoke inspection

Required Jenkins setup:

- Java 21
- Maven
- Docker CLI with permission to access the Docker daemon
- `kubectl` configured for the target cluster
- Jenkins credential named `ghcr-credentials` containing a GHCR username and token

## GitHub Actions

`.github/workflows/ci.yml` runs `mvn clean verify -Pquality -DskipTests=false` for pushes and pull requests targeting `master`. The workflow also cancels stale concurrent runs and grants read-only repository permissions.

## Core business flow

1. Login → access + refresh JWT
2. Apply loan → PENDING
3. Manager L1 approval
4. Admin L2 approval → APPROVED + EMI calculation
5. Disbursement with idempotency protection
6. EMI schedule generation through the loan-approved event
7. EMI repayment / foreclosure
8. Scheduled overdue processing with distributed locking
9. Notification and audit events through Kafka
10. Document upload with validation

## Document storage

The document service supports two storage backends:

- `local` (default): suitable for local Docker development and tests.
- `s3`: durable object storage for production Kubernetes deployments.

To enable S3, configure:
- `DOCUMENT_STORAGE_TYPE=s3`
- `DOCUMENT_S3_BUCKET=<bucket>`
- `DOCUMENT_S3_REGION=<region>`
- optional `DOCUMENT_S3_KEY_PREFIX`
- optional `DOCUMENT_S3_ENDPOINT` for S3-compatible development services
- optional `DOCUMENT_S3_PATH_STYLE_ACCESS=true` for S3-compatible endpoints

On AWS, prefer workload IAM credentials (for example EKS pod identity/IRSA) rather than static access keys. The application uses the AWS SDK default credentials provider chain.

Local filesystem storage is not shared across replicas and should not be used for production document durability.

The least-privilege S3 permissions template is in `deploy/aws/document-service-s3-policy.json`. Production should bind those permissions to the document-service workload identity and keep the bucket private.

## Production hardening included

- JWT validation and role-based authorization
- Customer loan ownership enforcement using JWT `userId`
- Idempotency keys and request-hash protection
- Transactional outbox with retry/backoff
- Kafka retry/DLT handling
- Event consumer idempotency
- Optimistic/pessimistic locking where appropriate
- Resilience4j retry/circuit-breaker/timeout patterns
- Correlation ID propagation with bounded request IDs and MDC cleanup
- Standardized API error responses with correlation IDs; unexpected server errors are logged internally without exposing stack traces
- Refresh-token hashing at rest and scheduled cleanup
- Atomic Kafka event idempotency for audit, notifications and EMI schedule generation
- Outbox processing lease, bounded retries, and backlog metrics
- Prometheus alert rules for service availability, HTTP 5xx rate, Hikari saturation and outbox backlog
- Jenkins Trivy image vulnerability scanning before registry push
- Container/image build reproducibility through pinned runtime digests
- KYC identifier masking in customer API responses
- JWT secret strength validation (minimum 32-byte secret)
- Testcontainers-based database migration regression coverage
- Concurrency-safe disbursement idempotency ledger
- Redis caching
- Actuator health probes
- Prometheus metrics
- Grafana provisioning
- Testcontainers integration coverage
- Checkstyle / SpotBugs / OWASP Dependency-Check
- Non-root application containers
- Kubernetes resource limits, probes and autoscaling

## Security note

No real credentials or production secrets belong in Git. Use environment variables, Kubernetes Secrets, Jenkins credentials, or a cloud secret manager for deployment secrets.

This repository is a portfolio/learning project demonstrating production microservice patterns; infrastructure such as managed databases, Kafka clusters, object storage, ingress, TLS, IAM and cloud networking must be configured in the target environment.

## Observability alerts

Prometheus loads `prometheus/alerts/loan-management.yml`. The rules cover service availability, HTTP 5xx rate, Hikari connection-pool saturation, and loan outbox backlog/failed events. In production, connect Prometheus to an Alertmanager or managed alerting service for notification delivery.
