# Tempo telemetry search returns only the required attribute keys, with no per-trace follow-up fetch

<!-- markdownlint-disable MD036 -->

**Title**
Tempo telemetry search returns only the required attribute keys, with no per-trace follow-up
fetch

**Lens**: SW

**Status**: planned

**Description**
`TempoTelemetryServiceImpl.findOpenTelemetryTracingData` builds its TraceQL query with a
`select()` clause naming the given required attribute-key set (`SW-TMP-002`), and builds every
`OpenTelemetryData` it returns directly from the search response. `TempoQueryClient.getTraceById`
is no longer called, and `fetchFullSpans` is removed — a single `search()` round trip is
sufficient for the whole calculation, regardless of how many traces matched.

**Rationale**
Tempo's TraceQL `select()` clause returns exactly the attributes it names, for every matched span,
in the same search response — the only reason the previous implementation needed a per-trace
follow-up fetch was that the calculators' required keys were assumed unenumerable up front
(`STR-TMP-002`'s problem statement); `SW-TMP-002` establishes they are not.
Removing the follow-up
fetch removes the dominant cost of a calculation against a busy API (one HTTP round trip per
matched trace, previously — confirmed via a production stack trace — enough to exceed
`max.poll.interval.ms` and fence the Kafka Streams consumer out of its group, the root cause
`STR-TMP-001` exists to tolerate).

**Verification Description**
An integration test against a Tempo test double asserts: the search request's TraceQL query
contains a `select()` clause naming exactly the given required-key set; no request is made to the
trace-by-ID endpoint; and the resulting `OpenTelemetryData` set's attributes match what the search
response's `select()`-projected span data carried.
A second run seeds telemetry across enough
matched traces that the previous per-trace-fetch implementation would have made that many
additional requests, and asserts the total request count to the Tempo test double stays at one
regardless.

## Relations

**Related**

- [SW-TMP-002](SW-TMP-002-required-attribute-key-set-derivation.md) — the key set this search
  request projects
- [ARCH-TMP-001](ARCH-TMP-001-required-attribute-keys-computed-once-by-caller.md) — how this
  backend receives the key set it projects
- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the pluggable-backend
  interface this implementation satisfies
- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the synchronous-within-Kafka-poll call
  path whose latency this spec shortens
