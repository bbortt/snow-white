# Runtime telemetry ingestion and correlation

<!-- markdownlint-disable MD036 -->

**Title**
Runtime telemetry ingestion and correlation

**Status**: done

**Business Value**
The specification alone cannot say whether an API is actually exercised; correlating it with real
runtime telemetry is what turns "the API is documented" into "the API is tested and working as
documented."

**Problem / Context**
Runtime behavior lives in OpenTelemetry traces, produced either by production traffic or by
black-box test runs.
Those traces have no inherent notion of "this OpenAPI operation" — they carry generic HTTP span
data unless something ties them back to the specification.

**Solution Approach**
Snow-White ingests OpenTelemetry tracing data and correlates it to an indexed specification through
a small set of shared semantic attributes (service name, API name, API version) carried on spans —
optionally enriched by `toolkit/spring-web-autoconfiguration`'s `@SnowWhiteInformation`.
The attributes themselves are a documented semantic convention (`semantic-convention/openapi.md`).
Any span producer — the toolkit or a team's own instrumentation — can emit them without depending
on Snow-White code.
Ingestion is scoped strictly to what a specific analysis request needs; Snow-White is a consumer
of telemetry, not a long-term store for it.

**Acceptance Criteria**

- Spans carrying the shared attributes for a known API correlate to the correct indexed
  specification.
- A telemetry query is bounded by the requested API and lookback window; no telemetry is retained
  beyond that scope.

**Out of scope**

- Acting as a general-purpose observability backend or long-term telemetry store.
- Trace formats other than OpenTelemetry.

## Relations

**Realizes**

- [SYS-004](../specs/SYS-004-telemetry-ingestion-and-correlation.md) — the ingestion and
  correlation mechanism
- [SYS-005](../specs/SYS-005-scoped-telemetry-ingestion.md) — the scoping constraint

**Related**

- [SYS-002](../specs/SYS-002-indexed-spec-lookup.md) — the specification side this
  correlates against
