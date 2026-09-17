# Documented backend pluggability, Kafka messaging role, and telemetry-fetch footprint for openapi-coverage-stream

<!-- markdownlint-disable MD036 -->

**Title**
Documented backend pluggability, Kafka messaging role, and telemetry-fetch footprint for
`openapi-coverage-stream`

**Status**: done

**Business Value**
`openapi-coverage-stream` is the only service that talks to a telemetry backend and the only one
with no synchronous entry point of its own — an engineer working on it, or an operator diagnosing
it, currently has to read the source to learn that it supports two mutually exclusive backends, that
Kafka (not HTTP) drives it end to end, or that it can be asked to correlate thousands of spans
against a backend that may itself be resource-constrained.
None of that is pinned down anywhere in the spec corpus today, even though it governs real
deployment decisions (which backend to run, how much memory to give this service and its backend).

**Problem / Context**
`docs/spec/specs/SYS-004-telemetry-ingestion-and-correlation.md` names InfluxDB and Grafana Tempo
in passing, as a rationale clause, but no spec pins the selection mechanism, its mutual exclusivity,
or the shared interface both implement.
No spec describes Kafka's specific role for this service — the calculation-request /
calculation-response topic pair, the `ApiType.OPENAPI` filter on an otherwise format-agnostic topic,
or the guarantee that every request resolves to a published response even on failure.
And no spec addresses how much telemetry a single calculation is allowed to pull into memory: a
source read of both backend implementations found Tempo's search already bounded to 1,000 matches
per query before per-trace detail is fetched, while InfluxDB's query has no result-size bound at
all — an asymmetry that matters once a calculation can correlate against thousands of spans on a
service meant to run with a small memory footprint.

**Solution Approach**
Add one architecture spec pinning the dual-backend selection mechanism and its mutual exclusivity,
one software spec pinning Kafka's specific request/response role for this service, and one
non-functional spec requiring the telemetry fetch itself — not just what is retained afterward — to
stay bounded regardless of how much data an analysis window could otherwise match, documenting the
current asymmetry between the two backends as a named, tracked gap rather than an unstated one.

**Acceptance Criteria**

- A spec states that `openapi-coverage-stream` selects exactly one of InfluxDB or Grafana Tempo at
  startup by configured connection properties, that the two are mutually exclusive, and that both
  implement one interface the rest of the service depends on without knowing which is active.
- A spec states Kafka's specific role for this service: the calculation-request /
  calculation-response topic pair, the `ApiType.OPENAPI` filter, key preservation from request to
  response, and that every request resolves to a published response even when processing fails.
- A spec requires the telemetry fetched for a single calculation to stay bounded by an explicit
  result-size or pagination limit, not only by the request's scope, on both backends — and records
  that this currently holds for Tempo but not for InfluxDB.
- The intent to add a throughput/load test verifying bounded memory usage under high span volume is
  recorded against the non-functional spec, as future work rather than a precondition of it.

**Out of scope**

- Actually adding the InfluxDB result-size bound, or building the throughput/load test — both are
  named as follow-up work by the specs this story adds, not implemented here.
- Extending this documentation to `otel-event-filter-stream`, the other Kafka Streams-driven
  service, which is out of scope for this pass.

## Relations

**Realizes**

- [ARCH-001](../specs/ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the
  dual-backend selection mechanism
- [SW-008](../specs/SW-008-kafka-as-async-calculation-driver.md) — Kafka's request/response
  role for this service
- [NF-006](../specs/NF-006-bounded-telemetry-fetch-footprint.md) — the bounded-fetch
  requirement and the current InfluxDB/Tempo asymmetry

**Related**

- [SYS-004](../specs/SYS-004-telemetry-ingestion-and-correlation.md) — names both backends in
  passing; this story's specs pin the mechanism itself
- [SYS-005](../specs/SYS-005-scoped-telemetry-ingestion.md) — the request-scope bound this
  story's non-functional spec adds a result-size bound alongside
- [NF-003](../specs/NF-003-scalability-without-redesign.md) — the low-resource deployment target
  this story's non-functional spec protects
- [STR-007](STR-007-coverage-criteria-calculator-correctness.md) — the sibling story
  documenting this same service's calculation correctness
