# The three coverage calculation phases are the span boundary, and the individual calculators are not

<!-- markdownlint-disable MD036 -->

**Title**
The three coverage calculation phases are the span boundary, and the individual calculators are
not

**Lens**: SW

**Status**: planned

**Description**
`OpenApiCoverageCalculationService`'s stages each produce one span, so the derived
`snow.white.span.duration` histogram separates them as three of its series, keyed by span name —
not as three metrics, since one connector emits one histogram for every span it reads:

- `fetchOpenApiSpecification` — retrieving the specification from `api-index-api` and parsing it.
- `enrichWithOpenTelemetryData` — querying the configured telemetry backend.
- `calculateCoverage` — computing the criteria results.

The names above are the methods; the span names are the agent's default `Class.method`, because no
annotation here sets a value.
The third phase is therefore `OpenApiCoverageCalculationServiceImpl.calculateCoverage`, distinct
from the `OpenApiCoverageService.calculateCoverage` nested below it.

`fetchOpenApiSpecification` and `calculateCoverage` already carry `@WithSpan`.
`enrichWithOpenTelemetryData` gains it, and is the only annotation this spec adds.
`buildResponseEvent` gains none: it maps fields and would contribute a span and a metric series
measuring nothing.

The individual coverage calculators are explicitly **not** annotated, and this spec adds no
annotation below the three stages above.

Six methods below those stages already carry `@WithSpan`, and this spec neither extends that set
nor retires it: `ApiIndexCachingService.fetchApiSourceUrl` and `OpenApiService.findAndParseOpenApi`
under the fetch phase, `InfluxDBTelemetryServiceImpl.findOpenTelemetryTracingData` and its Tempo
counterpart under the telemetry-query phase, and `OpenApiCoverageCalculationCoordinator.calculate`
and `OpenApiCoverageService.calculateCoverage` under the coverage phase.
The histogram therefore carries nine series per deployment and not three, since one telemetry
backend is configured at a time
([ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md)) and the processor's own span
([SW-056](SW-056-calculation-outcome-rides-on-the-span.md)) is the ninth.
Those spans are kept because each is a distinct dependency or a distinct failure site a trace is
read for, and because the set is fixed by the code rather than by traffic, which is the sense in
which [CON-017](CON-017-metric-dimensions-are-bounded.md) requires span names to be bounded.
What the three phases buy over them is attribution an operator can read without knowing the call
tree.

The phase boundary is the service interface, not the processor.
`OpenApiCoverageCalculationProcessor` wraps the three calls in a `try` whose `catch` branches
answer the request, so a span at the processor would measure the three phases plus the failure
handling as one number.
The processor does carry a span, because the outcome is decided in those `catch` branches and an
attribute needs an open span to ride on
([SW-056](SW-056-calculation-outcome-rides-on-the-span.md)); it is read as the outcome's carrier and
as the one span per calculation, never as a phase.

**Rationale**
The three phases have different owners and different fixes, and a single duration cannot tell them
apart.
`enrichWithOpenTelemetryData` is pure I/O against InfluxDB or Tempo
([ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md)) and is the phase most
likely to dominate; `calculateCoverage` is pure computation — no `RestClient`, InfluxDB, or Tempo
client is reachable from it, so a rise there means a specification grew or a calculator regressed,
never that a dependency slowed down.
`fetchOpenApiSpecification` mixes an HTTP call with parsing and is the phase whose attribution is
genuinely blended.

Without the middle annotation the most expensive phase is the unmeasured one, which is why two of
three spans existing today is not partial progress toward this spec but the specific gap it
closes.

Against the phases, the three durations also separate what happens **outside** them: subtracting
their sum from the end-to-end duration
([SW-059](SW-059-end-to-end-duration-from-the-triggering-instant.md)) leaves queue and
scheduling time, which is a third owner again.

Excluding the calculators is a cost decision with a real number behind it.
There are roughly fifteen of them per calculation, so annotating them would multiply span volume
per calculation by that factor and, through the connector, produce a metric series per calculator.
A calculator's individual cost is a profiling question asked rarely, not a dashboard question
asked continuously, and a trace already shows the shape of a slow calculation.

`requiredAttributeKeys` is computed as an argument to the enrichment call, so a specification walk
bills to the telemetry-query phase.
It is hoisted into the fetch phase, where
[ARCH-007](ARCH-007-required-attribute-keys-computed-once-by-caller.md) already places the
caller's responsibility, so the query phase measures the query.

**Verification Description**
A unit test asserts each of the three service methods produces exactly one span, using the
OpenTelemetry testing SDK, and that `buildResponseEvent` produces none.
A review confirms no calculator carries `@WithSpan`, that the annotated methods below the three
stages are the six this spec names and no others, and that `requiredAttributeKeys` is evaluated
before the enrichment call rather than within it.
An integration check against the development stack asserts three distinct phase durations are
derived for one calculation, and that a deliberately slowed telemetry backend raises only the
telemetry-query phase.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality these phase durations
  serve

**Related**

- [ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md) — the decision
  that these spans, not timers, are where durations come from
- [ARCH-027](ARCH-027-connector-reads-only-snow-whites-own-spans.md) — the pipeline that
  turns these spans into metrics
- [SW-056](SW-056-calculation-outcome-rides-on-the-span.md) — the outcome attribute these
  spans also carry
- [SW-059](SW-059-end-to-end-duration-from-the-triggering-instant.md) — the duration these
  three are subtracted from to isolate queue time
- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the backend whose
  latency the middle phase isolates
- [ARCH-007](ARCH-007-required-attribute-keys-computed-once-by-caller.md) — the caller
  responsibility the hoist aligns with
- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the topology these phases run inside
- [CON-015](CON-015-harness-span-never-moves-a-coverage-verdict.md) — the separation between
  Snow-White's own spans and the harness spans it analyses
