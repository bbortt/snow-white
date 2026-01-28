# Architecture Overview

**Snow-White** follows an event-driven architecture.
The below diagram includes all microservices and their connections.

[![Architecture Overview](./architecture.png)](./architecture.puml)

## Microservices

Each microservice completes a specific task.

<!-- markdownlint-disable markdownlint-sentences-per-line -->

| Microservice                                                       | Intent                                                                                                                                                                                |
| ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [API Gateway](./microservices/api-gateway)                         | Handles incoming HTTP requests and routes them to internal services.                                                                                                                  |
| [API Index](./microservices/api-index-api)                         | Manages indexed API specifications.                                                                                                                                                   |
| [API Sync Job](./microservices/api-sync-job)                       | Periodically fetches API definitions from various sources and stores metadata for reference.                                                                                          |
| [OTEL Event Filter](./microservices/otel-event-filter-stream)      | An optional service, in addition to the Open-Telemetry collector. Filters telemetry events from Kafka topics based on whether they're applicable for processing by Snow-White or not. |
| [OpenAPI Coverage Stream](./microservices/openapi-coverage-stream) | Analyzes the coverage of actual API usage against declared OpenAPI specifications                                                                                                     |
| [Quality-Gate API](./microservices/quality-gate-api)               | Handles quality gate evaluations and criteria management.                                                                                                                             |
| [Report Coordinator API](./microservices/report-coordinator-api)   | Coordinates data aggregation and reporting logic across the application.                                                                                                              |

<!-- markdownlint-enable markdownlint-sentences-per-line -->

## Mapped Ports

| Service                                                                | Spring Boot (`dev` profile) | Docker (or Podman) Compose (`docker-compose.yaml`) |
| ---------------------------------------------------------------------- | --------------------------- | -------------------------------------------------- |
| [`example-application`](./example-application)                         | `8080`                      | `8080`                                             |
| [`api-gateway`](./microservices/api-gateway)                           | `9080`                      | `80`                                               |
| [`api-index-api`](./microservices/api-index-api)                       | `8085`                      | `8085`                                             |
| [`api-sync-job`](./microservices/api-sync-job)                         | -                           | -                                                  |
| [`otel-event-filter-stream`](./microservices/otel-event-filter-stream) | -                           | -                                                  |
| [`openapi-coverage-stream`](./microservices/openapi-coverage-stream)   | -                           | -                                                  |
| [`quality-gate-api`](./microservices/quality-gate-api)                 | `8081`                      | `8081`                                             |
| [`report-coordinator-api`](./microservices/report-coordinator-api)     | `8084`                      | `8084`                                             |

The UI Development Server (used by `api-gateway`) is available on port `9001`.

## Additional Services

These services are only available inside Docker (or Podman) Compose:

| Service                      | Ports                                                    |
| ---------------------------- | -------------------------------------------------------- |
| OTEL Collector               | `1888`, `8888`, `8889`, `13133`, `4317`, `4318`, `55679` |
| Kafka                        | `9092`, `9094`                                           |
| Kafka UI                     | `8090`                                                   |
| InfluxDB                     | `8086`                                                   |
| PostgreSQL                   | `5432`                                                   |
| Service Interface Repository | `3000`                                                   |
