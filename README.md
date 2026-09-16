# Loan Management System – Production-Grade Microservices

Complete conversion of the original modular monolith  
(https://github.com/Singh301/loan-management-system) into a **production-oriented microservices architecture**.

**All original functionality is preserved** across bounded services.

---

## Architecture

```
                    ┌──────────────────────┐
                    │     API Gateway      │ :8080
                    │  (Spring Cloud GW)   │
                    └──────────┬───────────┘
                               │
     ┌─────────────┬───────────┼───────────┬─────────────┐
     ▼             ▼           ▼           ▼             ▼
 Auth:8081   Customer:8082  Loan:8083  Repayment:8084  Document:8085
     │             │           │           │             │
     ▼             ▼           ▼           ▼             ▼
Notify:8086   Audit:8087  Dashboard:8088
                               │
                    ┌──────────┴───────────┐
                    │ Eureka :8761         │
                    │ MySQL / Redis / Kafka│
                    └──────────────────────┘
```

### Services

| Service | Port | Features |
|---------|------|----------|
| discovery-server | 8761 | Eureka registry |
| api-gateway | 8080 | Routing, rate-limit, correlation-id |
| auth-service | 8081 | JWT + Refresh + Blacklist + Lockout |
| customer-service | 8082 | Customer CRUD + KYC |
| loan-service | 8083 | Apply, Multi-level Approval, State Machine, Disburse, Outbox |
| repayment-service | 8084 | EMI schedule, Pay EMI, Foreclosure, Overdue job |
| document-service | 8085 | Secure upload/download (PDF/JPEG/PNG/WEBP ≤5MB) |
| notification-service | 8086 | Kafka → In-app notifications |
| audit-service | 8087 | Kafka → Immutable audit log |
| dashboard-service | 8088 | Analytics (extensible via Feign) |

---

## Tech Stack

- **Java 21** + Spring Boot **3.5.x** + Spring Cloud **2025.0.x**
- Spring Security + JJWT 0.12
- Spring Data JPA + Flyway + MySQL 8 (database-per-service)
- Redis (blacklist + rate limiting)
- Apache Kafka (domain events + transactional outbox)
- Resilience4j ready, Micrometer + Prometheus, SpringDoc OpenAPI
- Docker + Docker Compose

---

## Quick Start

### Prerequisites
- JDK 21, Maven 3.9+, Docker

### 1. Infrastructure
```bash
docker compose up -d mysql redis zookeeper kafka
```

### 2. Build
```bash
chmod +x scripts/build-all.sh
./scripts/build-all.sh
# or
./mvnw clean package -DskipTests
```

### 3. Run (order)
```bash
java -jar discovery-server/target/*.jar &
java -jar api-gateway/target/*.jar &
java -jar auth-service/target/*.jar &
java -jar customer-service/target/*.jar &
java -jar loan-service/target/*.jar &
java -jar repayment-service/target/*.jar &
java -jar document-service/target/*.jar &
java -jar notification-service/target/*.jar &
java -jar audit-service/target/*.jar &
java -jar dashboard-service/target/*.jar &
```

### Default credentials
| Username   | Password     | Role     |
|------------|--------------|----------|
| admin      | Password@123 | ADMIN    |
| manager    | Password@123 | MANAGER  |
| customer1  | Password@123 | CUSTOMER |

### Swagger
- Gateway: http://localhost:8080/swagger-ui.html  
- Individual services also expose `/swagger-ui.html`

---

## Core Flows Preserved

1. **Login** → JWT + Refresh token  
2. **Apply Loan** → PENDING  
3. **Manager L1 approval** → still PENDING  
4. **Admin L2 approval** → APPROVED + EMI calculated  
5. **Disburse** (Idempotency-Key) → DISBURSED → ACTIVE + outbox event  
6. **Generate EMI schedule** (repayment-service)  
7. **Pay EMI / Foreclosure**  
8. **Overdue scheduler** (daily 1 AM)  
9. **Notifications + Audit** via Kafka  
10. **Document upload** with validation  

---

## Design Highlights

- Explicit **LoanStateMachine** (illegal transitions blocked)
- **Transactional Outbox** → Kafka publisher
- Optimistic locking + Soft delete
- Multi-level approval
- Idempotent disbursement
- Account lockout + Token blacklist (Redis)
- Correlation ID (`X-Request-Id`)
- Database-per-service
- Event-driven side effects (Notification + Audit)

---

## License

Portfolio / learning project – converted from original monolith for production microservices patterns.
