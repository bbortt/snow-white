# Architecture

This document is the narrative framing for the project — it explains the shape.
The full
narrative (diagrams, port mappings, additional Docker Compose services) lives in
[`pages/_pages/architecture.md`](../../pages/_pages/architecture.md), the project's published docs site
page; this file adds the dependency-direction and enforcement framing clew's method expects,
without duplicating that page's content.

The atomic, enforceable architecture rules are authored as ARCH and CON specs; those specs are
what the code is checked against.

## Shape

Snow-White is an **event-driven microservices system**, built as a Maven multi-module reactor
(Java/Spring Boot backend, Kafka for async work, a React/TypeScript frontend hosted by the
gateway).
See `pages/_pages/architecture.md#microservices` for the full module table; in short:

- `api-gateway` — routes incoming HTTP to internal services, hosts the web UI.
- `api-index-api`, `quality-gate-api`, `report-coordinator-api` — synchronous REST services
  reached through the gateway.
- `api-sync-job` — periodic sync of external API specs.
- `otel-event-filter-stream`, `openapi-coverage-stream` — Kafka Streams processors.
- `internal/commons` — shared DTOs/events/utilities.
- `toolkit/spring-web-autoconfiguration`, `toolkit/openapi-generator` — cross-cutting libraries
  consumed by the services above, not services themselves.
- `toolkit/cli` — a standalone client, outside the Kafka/Spring runtime entirely.

Each Spring Boot service enforces its own layered internal architecture (`Config`/`Web or
Kafka`/`Service`/`Persistence`/`Domain`, varying by service — REST services carry a
`Persistence` layer that stream-only services don't) via an ArchUnit
`TechnicalStructureUnitTest`; see `005-testing-contract.md`.

## Dependency direction

- **Synchronous UI path**: web UI → API Gateway → target service (API Index API, Quality-Gate
  API, or Report Coordinator API) → response back through the gateway.
  No polling.
- **Asynchronous coverage calculation** (deliberate, not incidental): Report Coordinator API
  publishes to the `snow-white-calculation-request` Kafka topic and returns immediately;
  `openapi-coverage-stream` consumes it, queries InfluxDB/Tempo telemetry, and publishes the
  result to `snow-white-openapi-calculation-response`, which the coordinator reads to finalize
  the report.
  This keeps long-running coverage evaluation off the synchronous request path.
  `otel-event-filter-stream` applies the same asynchronous-decoupling principle, pre-filtering
  inbound telemetry before it reaches the coverage stream.
- Services never call each other's REST APIs synchronously to perform coverage work — that path
  is Kafka-only, by design (see above).
- `internal/commons` is depended on by the microservices; it must not depend back on any of them.
- `toolkit/*` libraries are depended on by the services that use them; they must not depend on any
  specific microservice.
- Within each service, dependencies point inward per its own `TechnicalStructureUnitTest`
  (e.g. `Domain` is never depended on by `Config`-only concerns reaching outward) — see that
  service's test for its exact layer rules.

## Enforceable rules

The rules above are authored as ARCH and CON specs as the code that they constrain is built —
checked at that point.
