# Tempo telemetry search returns only the required attribute keys, with no per-trace follow-up fetch

<!-- markdownlint-disable MD036 -->

**Title**
Tempo telemetry search returns only the required attribute keys, with no per-trace follow-up
fetch

**Lens**: SW

**Status**: active

**Description**
`TempoTelemetryServiceImpl.findOpenTelemetryTracingData` builds its TraceQL query with a
`select()` clause naming the given required attribute-key set (`SW-021`), and builds every
`OpenTelemetryData` it returns directly from the search response. `TempoQueryClient.getTraceById`
is no longer called, and `fetchFullSpans` is removed — a single `search()` round trip is
sufficient for the whole calculation, regardless of how many traces matched.

Each matched trace's spans are read from the response's `spanSets` array, falling back to the
deprecated singular `spanSet` field only when `spanSets` is absent or empty — never both, which
would count every span twice.
The number of spans each span-set may carry is bounded by the
explicit `spss` parameter `NF-008` requires, not by Tempo's own default of three.

**Rationale**
Tempo's TraceQL `select()` clause returns the attributes it names inline with each matched span in
the same search response — the only reason the previous implementation needed a per-trace
follow-up fetch was that the calculators' required keys were assumed unenumerable up front
(`STR-013`'s problem statement); `SW-021` establishes they are not.
Removing the follow-up
fetch removes the dominant cost of a calculation against a busy API (one HTTP round trip per
matched trace, previously — confirmed via a production stack trace — enough to exceed
`max.poll.interval.ms` and fence the Kafka Streams consumer out of its group, the root cause
`STR-012` exists to tolerate).

Dropping the follow-up fetch makes the search response the calculation's only source of spans, so
what that response omits is lost outright rather than recovered by the by-ID call.
That is why
`NF-008` is a prerequisite of this spec and not an independent convenience: Tempo returns three
spans per span-set unless told otherwise, and the previous implementation's habit of reading only
the deprecated singular `spanSet` inherited the same cap for the span ids it collected.
Reading
`spanSets` and sending `spss` explicitly closes a truncation this backend has silently carried all
along, rather than introducing a new one.

**Verification Description**
An integration test against a Tempo test double asserts: the search request's TraceQL query
contains a `select()` clause naming exactly the given required-key set; no request is made to the
trace-by-ID endpoint; and the resulting `OpenTelemetryData` set's attributes match what the search
response's `select()`-projected span data carried.
A second run seeds telemetry across enough
matched traces that the previous per-trace-fetch implementation would have made that many
additional requests, and asserts the total request count to the Tempo test double stays at one
regardless.
A third run returns a response carrying the same spans in both `spanSets` and the
deprecated `spanSet` field, and asserts each span appears exactly once in the resulting
`OpenTelemetryData` set.

## Relations

**Related**

- [SW-021](SW-021-required-attribute-key-set-derivation.md) — the key set this search
  request projects
- [NF-008](NF-008-tempo-search-returns-every-matched-span-per-trace.md) — the explicit
  per-span-set bound this spec depends on, having no follow-up fetch to recover truncated spans
- [ARCH-007](ARCH-007-required-attribute-keys-computed-once-by-caller.md) — how this
  backend receives the key set it projects
- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the pluggable-backend
  interface this implementation satisfies
- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the synchronous-within-Kafka-poll call
  path whose latency this spec shortens

## Changes

- **2026-09-19** — Set active: implementation of STR-013 began.
