# Architecture

```mermaid
flowchart TB
    Client[Client / Postman / Browser] --> Gateway[API Gateway :8080]
    Gateway --> Auth[Auth Service :8081]
    Gateway --> Customer[Customer Service :8082]
    Gateway --> Loan[Loan Service :8083]
    Gateway --> Repayment[Repayment Service :8084]
    Gateway --> Document[Document Service :8085]
    Gateway --> Notification[Notification Service :8086]
    Gateway --> Audit[Audit Service :8087]
    Gateway --> Dashboard[Dashboard Service :8088]
    Auth -.-> Eureka[Eureka :8761]
    Customer -.-> Eureka
    Loan -.-> Eureka
    Repayment -.-> Eureka
    Document -.-> Eureka
    Notification -.-> Eureka
    Audit -.-> Eureka
    Dashboard -.-> Eureka
    Gateway -.-> Eureka
    Auth --> MySQL[(MySQL)]
    Customer --> MySQL
    Loan --> MySQL
    Repayment --> MySQL
    Document --> MySQL
    Notification --> MySQL
    Audit --> MySQL
    Dashboard --> MySQL
    Gateway --> Redis[(Redis)]
    Loan --> Redis
    Repayment --> Redis
    Loan --> Kafka[(Kafka)]
    Kafka --> Notification
    Kafka --> Audit
    Kafka --> Repayment
    Gateway --> Metrics[Prometheus]
    Metrics --> Grafana[Grafana]
    Gateway -. tracing .-> Zipkin[Zipkin]
```

## Runtime layers

- **Edge:** API Gateway, JWT validation, routing, rate limiting and correlation IDs.
- **Business services:** auth, customer, loan, repayment and document.
- **Event-driven services:** notification and audit consume Kafka events; loan uses an outbox for reliable event publication.
- **Platform:** Eureka, MySQL, Redis, Kafka, Prometheus, Grafana and Zipkin.
- **Delivery:** GitHub Actions validates the repository; Jenkins builds, scans, publishes immutable images and deploys to Kubernetes.

## Kubernetes

The k8s directory contains namespace, configuration, service/deployment manifests, probes, resource limits, HPAs, PDBs, network policies and development-only infrastructure.
For production, replace development MySQL/Redis/Kafka workloads with managed services and provide secrets through the deployment environment.
