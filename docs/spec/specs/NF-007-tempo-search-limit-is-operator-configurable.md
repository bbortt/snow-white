# Tempo's per-query trace-match limit is an operator-set property, defaulting to today's constant

<!-- markdownlint-disable MD036 -->

**Title**
Tempo's per-query trace-match limit is an operator-set property, defaulting to today's constant

**Lens**: NF

**Status**: active

**Description**
The maximum number of traces a single Tempo `search()` call may match (`tempo.search-limit`) is
read from configuration at startup, with a default of `1000` matching the value
`TempoQueryClient.SEARCH_LIMIT` fixes today.
An operator may raise or lower it per deployment
without a code change; a configured value that is not a positive integer fails application context
startup rather than being silently ignored or applied at query time.

**Rationale**
`NF-006` anchors this limit as the mechanism that bounds the Tempo backend's fetch size; a fixed
constant forces that bound to the same value for every deployment regardless of traffic volume or
Tempo capacity, which is a poor fit for a service explicitly meant to run across very different
deployment sizes (`NF-003`).
Failing at startup rather than at query time keeps this consistent
with how `ARCH-001` already treats other backend misconfiguration — surfaced at deploy time, not as
a runtime failure mid-calculation.

**Verification Description**
A Spring context test asserts the default `search()` request uses `limit=1000` when
`tempo.search-limit` is unset; a second run sets the property to a different positive value and
asserts the `search()` request's `limit` parameter reflects it; a third run sets it to zero or a
negative value and asserts context startup fails.

## Relations

**Related**

- [NF-006](NF-006-bounded-telemetry-fetch-footprint.md) — the bounded-fetch constraint this
  configurable value continues to satisfy
- [NF-008](NF-008-tempo-search-returns-every-matched-span-per-trace.md) — the per-trace
  matched-span bound this trace-count bound complements; the two together bound one search
- [NF-003](NF-003-scalability-without-redesign.md) — the varied-deployment-size target this
  configurability serves
- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the fail-fast-at-startup
  precedent this spec's misconfiguration handling follows

## Changes

- **2026-09-19** — Set active: implementation of STR-014 began.
