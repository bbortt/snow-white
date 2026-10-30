---
title: 'Architecture'
permalink: /architecture/
toc: true
toc_sticky: true
---

Snow-White follows an **event-driven microservices architecture** built on Apache Kafka and OpenTelemetry.

It can work with both [InfluxDB](https://www.influxdata.com) or [Grafana Tempo](https://grafana.com/docs/tempo) telemetry backends.

Users interact with Snow-White through the web UI, the [CLI](./cli.md), or — when `snowWhite.mcpApi.enabled` is set — an AI agent connected to the MCP API.

## Installation Modes

Snow-White's own microservices are always the same.
What differs is whether the infrastructure around them — PostgreSQL, Kafka, the OTel Collector and the telemetry store — is bundled with Snow-White or already operated elsewhere.
Each component can be swapped independently, so mixed setups (e.g. an existing PostgreSQL but the bundled Kafka) are just as valid.
See [Deployment — Replacing Bundled Infrastructure](./deployment.md#replacing-bundled-infrastructure) for the exact Helm values.

### Bundled Infrastructure (Default)

The Helm chart installs everything Snow-White needs alongside it.
Applications **send** their traces to Snow-White's own OTel Collector, which filters them through Kafka and persists them in the bundled InfluxDB.

[![Architecture Overview]({{ "/architecture.png" | relative_url }})]({{ "/architecture.png" | relative_url }})

### Existing Infrastructure

Snow-White connects to infrastructure you already run.
Applications send **nothing** to Snow-White: they keep reporting to your existing OTel Collector and telemetry backend (InfluxDB or Grafana Tempo), and the OpenAPI Coverage Stream only **queries** that backend, read-only.
The bundled OTel Collector pipelines, the OTel Event Filter Stream, and the public `/v1/traces` route are not deployed (`otelCollector.disableIngestion: true`).

[![Existing Infrastructure]({{ "/architecture-external.png" | relative_url }})]({{ "/architecture-external.png" | relative_url }})

Because nothing filters the telemetry on its way in, the backend must already hold the spans Snow-White correlates — carrying the service name, API name and API version attributes (see [Onboarding](./onboarding.md)).

## Detailed Architecture

The detailed view shows the bundled installation mode.

[![Detailed Architecture]({{ "/architecture-detailed.png" | relative_url }})]({{ "/architecture-detailed.png" | relative_url }})

### Synchronous UI Communication

All API calls initiated from the web UI travel synchronously through the API Gateway to the target microservice and back.
The gateway routes each REST request to the appropriate backend service — API Index API, Quality-Gate API, or Report Coordinator API — and the response is returned directly to the caller.
This keeps UI interactions simple and predictable: a request in, a response out, no polling required.

### Asynchronous Coverage Calculation

Coverage calculation is deliberately asynchronous.
When the Report Coordinator API receives a calculation request it publishes a message to the `snow-white-calculation-request` Kafka topic and returns immediately.
The OpenAPI Coverage Stream picks that message up, queries the telemetry backend (InfluxDB or Grafana Tempo) for the relevant data, and publishes the result to the `snow-white-openapi-calculation-response` topic, from which the coordinator reads to finalize the report.

This decoupling is an explicit architectural decision: evaluating coverage over large datasets or long lookback windows can take considerable time.
Keeping that work off the synchronous request path prevents gateway timeouts, allows the stream processor to scale independently, and lets the coordinator handle many concurrent calculation requests without blocking.
The same principle applies to the OTel Event Filter Stream, which pre-filters inbound telemetry asynchronously before it reaches the coverage processor.
It only exists in the bundled installation mode — with existing infrastructure, there is no inbound telemetry to filter.

### MCP API

The optional MCP API exposes Snow-White to AI agents over the [Model Context Protocol](https://modelcontextprotocol.io).
It is a read-only façade: every tool answers with the same value the API Index API, Quality-Gate API or Report Coordinator API serves, so it holds no state of its own.
A hosted installation serves it through the API Gateway at `/mcp`; the same artifact can also run locally over standard streams, launched by the agent client itself.

## Microservices

Each microservice fulfils a single, well-defined responsibility.

| Microservice                                                                                                    | Intent                                                                                                                       |
| --------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| [API Gateway](https://github.com/bbortt/snow-white/tree/main/microservices/api-gateway)                         | Handles incoming HTTP requests and routes them to internal services.                                                         |
| [API Index](https://github.com/bbortt/snow-white/tree/main/microservices/api-index-api)                         | Manages indexed API specifications.                                                                                          |
| [API Sync Job](https://github.com/bbortt/snow-white/tree/main/microservices/api-sync-job)                       | Periodically fetches API definitions from external sources and stores metadata for reference.                                |
| [MCP API](https://github.com/bbortt/snow-white/tree/main/microservices/mcp-api)                                 | Optional read-only MCP server — lets AI agents query API index, quality gates and reports.                                   |
| [OTel Event Filter](https://github.com/bbortt/snow-white/tree/main/microservices/otel-event-filter-stream)      | Optional Kafka stream filter — discards telemetry events not applicable to Snow-White before they reach the coverage stream. |
| [OpenAPI Coverage Stream](https://github.com/bbortt/snow-white/tree/main/microservices/openapi-coverage-stream) | Analyzes coverage of actual API usage against declared OpenAPI specifications.                                               |
| [Quality-Gate API](https://github.com/bbortt/snow-white/tree/main/microservices/quality-gate-api)               | Handles quality gate evaluations and criteria management.                                                                    |
| [Report Coordinator API](https://github.com/bbortt/snow-white/tree/main/microservices/report-coordinator-api)   | Coordinates data aggregation and reporting logic across the application.                                                     |

## Mapped Ports

| Service                    | Spring Boot (`dev` profile) | Docker / Podman Compose |
| -------------------------- | --------------------------- | ----------------------- |
| `example-application`      | `8080`                      | `8080`                  |
| `api-gateway`              | `9080`                      | `80`                    |
| `api-index-api`            | `8085`                      | `8085`                  |
| `api-sync-job`             | —                           | —                       |
| `mcp-api`                  | —                           | `8087`                  |
| `otel-event-filter-stream` | —                           | —                       |
| `openapi-coverage-stream`  | —                           | —                       |
| `quality-gate-api`         | `8081`                      | `8081`                  |
| `report-coordinator-api`   | `8084`                      | `8084`                  |

The UI Development Server (used by `api-gateway`) runs on port `9001`.

## Additional Services (Docker Compose Only)

| Service                      | Ports                    |
| ---------------------------- | ------------------------ |
| OTel Collector               | `13133`, `4317`, `4318`, |
| Kafka                        | `9092`, `9094`           |
| Kafka UI                     | `8090`                   |
| InfluxDB                     | `8086`                   |
| PostgreSQL                   | `5432`                   |
| Service Interface Repository | `3000`                   |
