# Ingestion of OpenTelemetry tracing data, correlated to indexed API specifications via shared semantic attributes

<!-- markdownlint-disable MD036 -->

**Title**
Ingestion of OpenTelemetry tracing data, correlated to indexed API specifications via shared
semantic attributes

**Lens**: SYS

**Status**: active

**Description**
Snow-White ingests OpenTelemetry tracing data as its runtime telemetry source, and correlates each
trace to an indexed API specification through semantic attributes shared between the two — service
name, API name, and API version, carried on spans.
Trace formats other than OpenTelemetry are out of scope.

**Rationale**
OpenTelemetry is the vendor-neutral tracing standard; anchoring correlation to a small, shared set
of semantic attributes rather than parsing raw HTTP details from spans keeps the correlation
mechanism stable across the two supported telemetry backends (InfluxDB, Grafana Tempo).
The attributes are fixed as a semantic convention (`semantic-convention/openapi.md`: `api.name`,
`api.version`, `openapi.operation.id`) rather than left implicit.
That way, a span producer outside the toolkit can still be correlated correctly.

**Verification Description**
An integration test emits spans carrying the shared attributes for a known API, runs a correlation
pass, and asserts the resulting telemetry data set is attributed to the correct API specification.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — the telemetry side of the
  correlation

**Related**

- [SYS-002](SYS-002-indexed-spec-lookup.md) — the spec side the attributes correlate against
- [SYS-005](SYS-005-scoped-telemetry-ingestion.md) — how much telemetry is actually pulled in
- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the selection mechanism
  and mutual exclusivity behind "the two supported telemetry backends" named above
- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the Kafka-driven processor that performs
  this ingestion for OpenAPI-typed requests

## Changes

- **2026-09-14** — Added `ARCH-001` and `SW-008` as related specs.
  They pin the backend-selection mechanism and the Kafka-driven processor this spec's rationale
  only named informally.
