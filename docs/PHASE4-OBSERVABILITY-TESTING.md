# Phase 4 — Observability, Testing and Quality

## Observability

- Spring Boot Actuator exposes health, metrics and Prometheus endpoints where Actuator is enabled.
- Loan Service exposes `/actuator/prometheus` and `/actuator/metrics`.
- Prometheus is available on port `9090` in Docker Compose.
- Grafana is available on port `3000` and is provisioned with Prometheus as its default datasource.
- Zipkin remains available on port `9411` for distributed tracing infrastructure.
- `X-Correlation-Id` is generated when absent, returned in the response and placed into SLF4J MDC.
- Shared Logback configuration includes the correlation ID in every log line.

## Testing

Run the full Maven test suite:

```bash
mvn clean test
```

Loan Service contains a Testcontainers MySQL smoke test. It requires Docker:

```bash
mvn -pl loan-service -am test -Dtest=MySqlContainerTest
```

## Quality and security scanning

The root `quality` profile adds:

- Checkstyle
- SpotBugs
- OWASP Dependency-Check

Run:

```bash
mvn clean verify -Pquality
```

Dependency-Check is configured to fail the build for vulnerabilities with CVSS >= 7.

## Local observability

Start the stack with:

```bash
docker compose up -d --build
```

Then inspect:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Zipkin: `http://localhost:9411`
- Loan metrics: `http://localhost:8083/actuator/prometheus`
