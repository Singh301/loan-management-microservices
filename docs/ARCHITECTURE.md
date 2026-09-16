# Architecture – Loan Management Microservices

## Bounded Contexts

| Service | Responsibility |
|---------|----------------|
| auth-service | Identity, JWT, Refresh, Lockout, Blacklist |
| customer-service | Customer profile & KYC |
| loan-service | Application, Approval, State Machine, Disbursement, Products, Collateral, Outbox |
| repayment-service | EMI schedules, Payments, Foreclosure, Overdue scheduler |
| document-service | Secure file upload/download |
| notification-service | In-app notifications (Kafka consumer) |
| audit-service | Immutable audit trail (Kafka consumer) |
| dashboard-service | Aggregated analytics |

## Communication

- **Sync**: REST via API Gateway / OpenFeign
- **Async**: Kafka topic `loan.events` (Outbox pattern in loan-service)
- **Discovery**: Eureka
- **Config**: Environment variables / future Config Server

## Loan State Machine

```
PENDING ──► APPROVED ──► DISBURSED ──► ACTIVE ──► OVERDUE ──► NPA
   │            │                         │          │
   └── REJECTED │                         ├── CLOSED │
                └── REJECTED              └── WRITTEN_OFF
```

Multi-level approval:
1. Manager → Level-1 (status stays PENDING)
2. Admin → Level-2 → APPROVED + EMI calculated
3. Admin → Disburse (Idempotency-Key) → DISBURSED → ACTIVE

## Security

- Stateless JWT (access 15 min + refresh 7 days)
- Redis token blacklist on logout
- Account lockout after 5 failed logins (15 min)
- Rate limiting on `/api/v1/auth/**` at Gateway
- Method-level `@PreAuthorize`
- Hardened uploads (type + size + path traversal protection)
