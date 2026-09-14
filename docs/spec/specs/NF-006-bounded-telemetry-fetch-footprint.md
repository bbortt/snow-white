# Telemetry fetched from InfluxDB or Tempo per calculation is bounded, not fetched in full

<!-- markdownlint-disable MD036 -->

**Title**
Telemetry fetched from InfluxDB or Tempo per calculation is bounded, not fetched in full

**Lens**: NF

**Status**: planned

**Description**
A single coverage calculation can correlate against thousands of spans (a busy API over a long
lookback window), and `openapi-coverage-stream` must be able to complete that calculation while
running with a small memory footprint, without requiring its InfluxDB or Tempo dependency to be
provisioned for large working sets either.
Whichever backend is configured (see
[ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md)), the query issued for
a calculation must stay bounded — by the request's own scope (service, API name, version, lookback
window, and any configured attribute filters — [SYS-005](SYS-005-scoped-telemetry-ingestion.md))
and by an explicit result-size or pagination bound — rather than pulling an unbounded result set into
process memory in one round trip.

This is only partially true of the current implementation: the Tempo backend bounds its search to
1,000 matching traces per query (`TempoQueryClient.SEARCH_LIMIT`) before fetching each matched
trace's full span data individually; the InfluxDB backend narrows its query by the request's scope
and attribute filters but applies no result-size or pagination bound at all, so a sufficiently large
matching window can return an unbounded number of spans in a single query response.
Closing that gap on the InfluxDB side is tracked as follow-up work, not resolved by this spec.

**Rationale**
InfluxDB and Tempo are dependencies this service does not control the provisioning of; a query
pattern that assumes either can hold an arbitrarily large result set in memory to serve one
calculation makes both dependencies, and this service itself, a resource-exhaustion risk under a
low-memory deployment — exactly the deployment target this service is meant to support
([NF-003](NF-003-scalability-without-redesign.md)).
Bounding the fetch, rather than bounding only what is retained afterward, is what actually protects
memory footprint: an unbounded query still peaks at the full result size in memory even if most of
it is discarded immediately after.

**Verification Description**
A throughput/load test seeds a telemetry backend with a large span volume (on the order of
thousands of spans), runs a calculation against it under a constrained memory limit, and asserts
peak memory usage stays bounded regardless of the seeded volume, for both backends.
No such test exists yet — building it is near-term follow-up work tracked outside this draft, not a
precondition of promoting this spec.

## Relations

**Related**

- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the two backend
  implementations this constraint applies to
- [SYS-005](SYS-005-scoped-telemetry-ingestion.md) — bounds ingestion by request scope;
  this spec additionally bounds it by result size within that scope
- [NF-003](NF-003-scalability-without-redesign.md) — the low-resource-footprint
  deployment target this constraint protects
