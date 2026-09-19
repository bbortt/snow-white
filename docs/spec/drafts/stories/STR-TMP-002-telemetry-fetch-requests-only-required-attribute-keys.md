# A coverage calculation's telemetry fetch requests only the attribute keys the calculators actually read

<!-- markdownlint-disable MD036 -->

**Title**
A coverage calculation's telemetry fetch requests only the attribute keys the calculators actually
read

**Status**: planned

**Business Value**
A quality-gate calculation must complete reliably and cheaply, without a telemetry backend that
returns more data than any calculator ever looks at.
Today, the Tempo backend answers one calculation with a search round trip plus one additional HTTP
round trip per matched trace, and the InfluxDB backend returns each matching span's entire
attribute blob regardless of how few of those attributes any calculator reads.
Both cost latency, memory, and — for Tempo, confirmed in production — availability: a slow
telemetry fetch is what fences `openapi-coverage-stream`'s Kafka Streams consumer out of its group
(`STR-TMP-001`), which is what makes the redelivery this system now has to tolerate happen in the
first place.

**Problem / Context**
`OpenTelemetryService.findOpenTelemetryTracingData` returns a `Set<OpenTelemetryData>`, each
carrying a full `attributes` JSON tree, to feed the 14 `OpenApiCoverageCriteria` calculators
(`ParameterCoverageCalculator`, `ResponseCodeCoverageCalculator`, `ContentTypeCoverageCalculator`,
and the rest under
`io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator`).
Between them, every calculator that reads an attribute reads one of a small, closed set of keys:
`http.request.method`, `url.path` (used by `OpenApiCoverageService` to group telemetry by
operation before any calculator runs), `http.response.status_code`, `url.query`, the fixed header
key `http.request.header.content-type`, the operator-configured
`OpenApiCoverageStreamProperties.operationIdAttribute`, and one `http.request.header.<paramName>`
per header parameter a given operation's own OpenAPI definition declares.
No calculator reads a
response body, and none reads any attribute outside this set — the full inventory was confirmed by
reading every calculator's attribute access in this session, not assumed.

Neither backend implementation reflects that today:

- `TempoTelemetryServiceImpl` fires a TraceQL `search()` that identifies matching (traceId,
  spanId) pairs, then calls `TempoQueryClient.getTraceById` once per matched trace and parses the
  complete native OTLP span — every attribute the instrumented service emitted, not just the ones
  any calculator reads.
  The class's own Javadoc states this is necessary because "downstream
  coverage calculators read attribute keys that can't be enumerated up front" — which this story's
  investigation found to be false: the key set is closed and fully enumerable before the query
  fires, from the fixed keys above plus the target `OpenAPI` spec's own header-parameter names
  (already parsed and available in `OpenApiTestContext.openAPI()` before
  `enrichWithOpenTelemetryData` runs).
- `InfluxDBTelemetryServiceImpl`'s Flux query stores each span's entire attribute set as one JSON
  blob per row (the `attributes` field), so every matching row returns that full blob regardless
  of which keys are actually needed.

For Tempo this is not just inefficient — it is the confirmed root cause of a production incident:
`TempoTelemetryServiceImpl.findOpenTelemetryTracingData` runs synchronously inside the Kafka
Streams record-processing call (`SW-008`), so its total latency, including one HTTP round trip per
matched trace, counts against `max.poll.interval.ms`.
A busy API with many matched traces can push
that past the poll interval, fencing the consumer out of its group
(`TaskMigratedException`/`CommitFailedException`, confirmed via a production stack trace) and
causing Kafka to redeliver the same calculation request — the exact scenario `STR-TMP-001` makes
`report-coordinator-api` tolerate rather than crash on.
Removing the per-trace round trip removes
the dominant cause of that latency spike, rather than only tolerating its consequence.

**Solution Approach**
Compute the calculation's required telemetry attribute-key set once, in `openapi-coverage-stream`'s
own service layer, from the fixed keys every calculator may read plus the header-parameter names
declared by the operations in the request's own `OpenAPI` spec — before the telemetry backend is
queried, using the `OpenAPI` object `OpenApiTestContext` already carries at that point.
Pass that
key set into `OpenTelemetryService.findOpenTelemetryTracingData` so either backend implementation
requests only those keys:

- Tempo: express the key set as a TraceQL `select()` clause on the existing `search()` call, and
  remove `fetchFullSpans`/`getTraceById` entirely — the search response alone becomes sufficient,
  eliminating the per-trace round trip.
- InfluxDB: narrow the Flux pipeline so the JSON object assigned to `_value` for each row contains
  only the required keys, instead of the full parsed attribute blob, before the final `keep()`.
  InfluxDB's storage still holds the full blob per span (that is a storage-schema question, not a
  query-shape one — see Out of scope), so this narrows the response payload and the in-process
  attribute tree size, not InfluxDB's own read I/O the way the Tempo change removes a round trip
  outright.

**Acceptance Criteria**

- For a calculation against a Tempo backend, `TempoQueryClient.getTraceById` is never called, and
  `TempoTelemetryServiceImpl`'s search request includes a `select()` clause naming exactly the
  fixed keys plus the header-parameter keys declared by the target `OpenAPI` spec's operations.
- For a calculation against an InfluxDB backend, the parsed `attributes` `JsonNode` on every
  `OpenTelemetryData` returned contains only keys from the same required-key set — no key outside
  it is present, even when the underlying span carried additional attributes.
- Every one of the 14 `OpenApiCoverageCriteria` calculators, exercised end to end against telemetry
  built from the narrowed key set, produces the same coverage result it produced before this
  change, for otherwise-identical input telemetry — the narrowing drops no data any calculator
  reads.
- The required-key computation is a unit-testable component on its own: given an `OpenAPI` spec
  with operations declaring header parameters, it returns the fixed keys plus exactly those
  header-parameter keys, and nothing else.

**Out of scope**

- Changing InfluxDB's storage schema so attributes are written as separate fields instead of one
  JSON blob per span, which is what would let InfluxDB's own read I/O shrink the way Tempo's round
  trip count does.
  Tracked as possible follow-up, not required for this story's payload-narrowing
  result.
- Bounding the _number_ of spans or traces a query can return (InfluxDB currently applies no such
  bound at all; Tempo already does via `TempoQueryClient.SEARCH_LIMIT`) — that is a result-_count_
  bound, the concern `NF-006` already tracks; this story narrows result _width_ (attributes per
  record), an independent axis. `STR-TMP-003` addresses making Tempo's existing count bound
  configurable; an InfluxDB count bound remains unaddressed follow-up.
- Extracting `OpenTelemetryData`'s InfluxDB-specific parsing (`parseOpenTelemetryData(FluxRecord)`,
  the `com.influxdb.query.FluxRecord` dependency) out of the shared DTO.
  This is a pure refactor
  with no observable-behavior change, so per this project's own spec-driven workflow it carries no
  spec of its own — it is done as implementation-time cleanup alongside this story's InfluxDB work,
  since that work touches the same construction path anyway.
- A future calculator reading an attribute key outside today's fixed set will need that key added
  to the required-key computation in the same change that adds the calculator — this story does
  not make that computation self-maintaining (for example by having each calculator declare its
  own required keys); `ARCH-TMP-001` pins this as the accepted trade-off, not an oversight to close
  later.

## Relations

**Realizes**

- [ARCH-TMP-001](../specs/ARCH-TMP-001-required-attribute-keys-computed-once-by-caller.md) — where
  the required-key computation lives and how it reaches either backend
- [SW-TMP-002](../specs/SW-TMP-002-required-attribute-key-set-derivation.md) — what the required
  key set contains and how it is derived
- [SW-TMP-003](../specs/SW-TMP-003-tempo-search-returns-only-required-keys.md) — the Tempo-specific
  observable behavior change
- [SW-TMP-004](../specs/SW-TMP-004-influxdb-query-narrows-attributes-to-required-keys.md) — the
  InfluxDB-specific observable behavior change

**Related**

- [STR-TMP-001](STR-TMP-001-redelivered-coverage-result-replaces-not-accumulates.md) — the
  redelivery-tolerance story whose root cause (Tempo per-trace fetch latency fencing the consumer)
  this story removes rather than only tolerates
- [ARCH-001](../specs/ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — documents
  `OpenTelemetryService`'s current signature, which this story extends with the required-key
  parameter; needs amending once this lands
- [NF-006](../specs/NF-006-bounded-telemetry-fetch-footprint.md) — bounds fetch _count_; this story
  bounds fetch _width_ (attributes per record), a distinct, complementary axis
- [SW-008](../specs/SW-008-kafka-as-async-calculation-driver.md) — the synchronous-within-Kafka-poll
  call path whose latency this story shortens for the Tempo backend
