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

The current Kubernetes application manifests expect `mysql`, `redis`, and `kafka` to be reachable in the cluster. For AWS production, map these endpoints to managed services such as RDS, ElastiCache and MSK rather than treating the application manifests as stateful-infrastructure manifests.

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

`.github/workflows/ci.yml` runs `mvn clean verify` for pushes and pull requests targeting `master`.

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

## Production hardening included

- JWT validation and role-based authorization
- Customer loan ownership enforcement using JWT `userId`
- Idempotency keys and request-hash protection
- Transactional outbox with retry/backoff
- Kafka retry/DLT handling
- Event consumer idempotency
- Optimistic/pessimistic locking where appropriate
- Resilience4j retry/circuit-breaker/timeout patterns
- Correlation ID propagation
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
