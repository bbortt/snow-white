---
title: 'Architecture'
permalink: /architecture/
toc: true
toc_sticky: true
---

Snow-White follows an **event-driven microservices architecture** built on Apache Kafka and OpenTelemetry.

[![Architecture Overview]({{ "/architecture.png" | relative_url }})]({{ "/architecture.png" | relative_url }})

---

## Microservices

Each microservice fulfils a single, well-defined responsibility.

| Microservice                                                                                                    | Intent                                                                                                                       |
| --------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| [API Gateway](https://github.com/bbortt/snow-white/tree/main/microservices/api-gateway)                         | Handles incoming HTTP requests and routes them to internal services.                                                         |
| [API Index](https://github.com/bbortt/snow-white/tree/main/microservices/api-index-api)                         | Manages indexed API specifications.                                                                                          |
| [API Sync Job](https://github.com/bbortt/snow-white/tree/main/microservices/api-sync-job)                       | Periodically fetches API definitions from external sources and stores metadata for reference.                                |
| [OTEL Event Filter](https://github.com/bbortt/snow-white/tree/main/microservices/otel-event-filter-stream)      | Optional Kafka stream filter — discards telemetry events not applicable to Snow-White before they reach the coverage stream. |
| [OpenAPI Coverage Stream](https://github.com/bbortt/snow-white/tree/main/microservices/openapi-coverage-stream) | Analyzes coverage of actual API usage against declared OpenAPI specifications.                                               |
| [Quality-Gate API](https://github.com/bbortt/snow-white/tree/main/microservices/quality-gate-api)               | Handles quality gate evaluations and criteria management.                                                                    |
| [Report Coordinator API](https://github.com/bbortt/snow-white/tree/main/microservices/report-coordinator-api)   | Coordinates data aggregation and reporting logic across the application.                                                     |

---

## Mapped Ports

| Service                    | Spring Boot (`dev` profile) | Docker / Podman Compose |
| -------------------------- | --------------------------- | ----------------------- |
| `example-application`      | `8080`                      | `8080`                  |
| `api-gateway`              | `9080`                      | `80`                    |
| `api-index-api`            | `8085`                      | `8085`                  |
| `api-sync-job`             | —                           | —                       |
| `otel-event-filter-stream` | —                           | —                       |
| `openapi-coverage-stream`  | —                           | —                       |
| `quality-gate-api`         | `8081`                      | `8081`                  |
| `report-coordinator-api`   | `8084`                      | `8084`                  |

The UI Development Server (used by `api-gateway`) runs on port `9001`.

---

## Additional Services (Docker Compose Only)

| Service                      | Ports                                                    |
| ---------------------------- | -------------------------------------------------------- |
| OTEL Collector               | `1888`, `8888`, `8889`, `13133`, `4317`, `4318`, `55679` |
| Kafka                        | `9092`, `9094`                                           |
| Kafka UI                     | `8090`                                                   |
| InfluxDB                     | `8086`                                                   |
| PostgreSQL                   | `5432`                                                   |
| Service Interface Repository | `3000`                                                   |
