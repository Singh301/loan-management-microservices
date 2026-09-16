# Functionality Matrix: Monolith → Microservices

## Implemented & Verified

| Feature | Monolith | Microservices | Service |
|---------|----------|---------------|---------|
| JWT Login / Refresh / Logout | Yes | Yes | auth-service |
| Account lockout (5 fails) | Yes | Yes | auth-service |
| Token blacklist (Redis) | Yes | Yes | auth-service |
| Customer CRUD + soft delete | Yes | Yes | customer-service |
| Loan apply | Yes | Yes | loan-service |
| Multi-level approval L1/L2 | Yes | Yes | loan-service |
| Explicit state machine | Yes | Yes | loan-service |
| Idempotent disbursement | Yes | Yes | loan-service |
| EMI calculation | Yes | Yes | loan-service |
| EMI schedule generation | Yes | Yes | repayment-service |
| Record repayment / pay EMI | Yes | Yes | repayment-service |
| Foreclosure amount | Yes | Yes | loan + repayment |
| Overdue scheduler | Yes | Yes | repayment-service |
| Loan products CRUD | Yes | Yes | loan-service |
| Collateral add/list/delete | Yes | Yes | loan-service |
| Loan list / search / by status | Yes | Yes | loan-service |
| Loan statistics | Yes | Yes | loan-service |
| Loan statement | Yes | Yes | loan-service |
| Close loan | Yes | Yes | loan-service |
| Document upload/download | Yes | Yes | document-service |
| Notifications (Kafka) | Yes | Yes | notification-service |
| Audit log (Kafka) | Yes | Yes | audit-service |
| Dashboard summary | Yes | Yes | dashboard-service |
| Eureka + API Gateway | — | Yes | discovery + gateway |
| Transactional Outbox → Kafka | Yes | Yes | loan-service |
| Soft delete | Yes | Yes | customer, loan |
| Optimistic locking | Yes | Yes | loan |

## Remaining gaps (lower priority / can extend)

| Feature | Notes |
|---------|-------|
| User admin CRUD (beyond seed) | Auth has seed users; full UserController from monolith not fully ported |
| my-loans (customer self-service filter) | Can filter by customerId already |
| Monthly report / rich analytics DTO | Statistics map available; chart-ready DTOs can be added |
| Document verification workflow | Upload works; verification status enum can be extended |
| S3 storage backend | Local filesystem; S3 config present in monolith pattern |
| Full foreclosure payment → close saga | Foreclosure calc + close endpoint exist; wire as single flow |
| Rate limit per-user beyond IP | Gateway has IP rate limit |

## API map (microservices)

- Auth: `/api/v1/auth/**` → :8081
- Customers: `/api/v1/customers/**` → :8082
- Loans: `/api/v1/loans/**` → :8083
- Loan Products: `/api/v1/loan-products/**` → :8083
- Collaterals: `/api/v1/collaterals/**` → :8083
- EMI: `/api/v1/emi-schedules/**` → :8084
- Repayments: `/api/v1/repayments/**` → :8084
- Documents: `/api/v1/documents/**` → :8085
- Notifications: `/api/v1/notifications/**` → :8086
- Audits: `/api/v1/audits/**` → :8087
- Dashboard: `/api/v1/dashboard/**` → :8088
