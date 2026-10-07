# Tempo's per-trace matched-span limit is an operator-set property, not Tempo's silent default of three

<!-- markdownlint-disable MD036 -->

**Title**
Tempo's per-trace matched-span limit is an operator-set property, not Tempo's silent default of
three

**Lens**: NF

**Status**: active

**Description**
Every Tempo `search()` call sends an explicit `spss` (spans per span-set) query parameter, read
from configuration at startup as `tempo.spans-per-trace-limit` with a default of `100`.
The value
is never left unset: Tempo's own default is `3`, which caps how many matched spans a trace
contributes to a calculation without reporting that it did so.
A configured value that is not a
positive integer fails application context startup rather than being silently ignored or applied
at query time.

**Rationale**
Tempo's `/api/search` documents `spss` as "Optional.
Limit the number of spans per span-set.
Default value is 3."
A trace whose matched spans exceed that limit is returned truncated, with no
error and no indication in the response that spans were dropped — so a calculation reading only
the search response under-reports coverage for exactly the busy traces most worth measuring.
Sending the parameter explicitly turns an invisible backend default into a deployment decision.

`100` is the default because Tempo's server-side `max_spans_per_span_set` also defaults to `100`:
it is the largest value a stock Tempo accepts, and requesting more than the server's configured
maximum is rejected rather than clamped.
Operators who have lowered `max_spans_per_span_set` must
lower this property to match; operators who have set it to `0` (no server-side ceiling) may set
this property to `0` for unlimited spans per span-set.

This is the per-trace counterpart to `NF-007`'s per-query trace-count bound: `NF-007` bounds how
many traces one search may match, this spec bounds how many matched spans each of those traces
contributes.
Both remain bounds — `NF-006`'s bounded-fetch constraint continues to hold on both
axes — and both become operator-set rather than decided by a constant this project compiled in or
a default Tempo applied on its own.

**Verification Description**
A Spring context test asserts the `search()` request carries `spss=100` when
`tempo.spans-per-trace-limit` is unset; a second run sets the property to a different non-negative
value and asserts the request's `spss` parameter reflects it; a third run sets it to a negative
value and asserts context startup fails.
A further test seeds a Tempo test double with a trace
whose span-set carries more matched spans than Tempo's own default of three, and asserts every one
of them reaches the returned telemetry data.

## Relations

**Related**

- [NF-007](NF-007-tempo-search-limit-is-operator-configurable.md) — the per-query trace-count
  bound this spec's per-trace span bound complements
- [NF-006](NF-006-bounded-telemetry-fetch-footprint.md) — the bounded-fetch constraint both
  configurable values continue to satisfy
- [NF-003](NF-003-scalability-without-redesign.md) — the varied-deployment-size target this
  configurability serves
- [SW-022](SW-022-tempo-search-returns-only-required-keys.md) — the search-only Tempo
  implementation whose correctness depends on this limit being set explicitly, since it has no
  per-trace follow-up fetch to recover spans the search response truncated
- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the fail-fast-at-startup
  precedent this spec's misconfiguration handling follows

## Changes

- **2026-09-19** — Set active: implementation of STR-014 began.
