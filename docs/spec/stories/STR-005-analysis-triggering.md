# Analysis triggering

<!-- markdownlint-disable MD036 -->

**Title**
Analysis triggering

**Status**: done

**Business Value**
Gives callers — a CI pipeline, an operator, the web UI — an explicit, scriptable way to start an
analysis, rather than requiring them to reach into Snow-White's internals.

**Problem / Context**
An analysis needs to run in two very different contexts: an automated CI pipeline (scriptable,
CLI-shaped) and interactive or system-to-system use (HTTP-shaped).
Both need the same guarantee: naming an API and a quality gate always resolves to a bounded,
terminal outcome, never an indefinite wait.

**Solution Approach**
Snow-White exposes a CLI and an HTTP API, both requiring the caller to name the exact API (service
name, API name, version) and quality gate.
Whichever entry point is used, coverage calculation is asynchronous internally (Kafka-mediated,
per `docs/spec/architecture.md`) but the trigger always resolves to a terminal state within a
bounded time, even under a downstream failure.

**Acceptance Criteria**

- Triggering through the CLI and through the HTTP API for the same API and gate produce
  equivalent, scoped analyses.
- A trigger made while the telemetry backend or the response topic is unavailable still resolves
  to a terminal (error) state within the documented bound.

**Out of scope**

- An implicit "analyze everything indexed" trigger — every trigger names an explicit API and gate.

## Relations

**Realizes**

- [SYS-010](../specs/SYS-010-analysis-triggering.md) — the two trigger entry points
- [SYS-011](../specs/SYS-011-bounded-time-resolution.md) — the bounded-time guarantee

**Related**

- [CON-002](../specs/CON-002-tolerate-dependency-outages.md) — the outage tolerance this
  bound relies on
