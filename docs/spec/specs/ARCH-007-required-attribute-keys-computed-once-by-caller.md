# The required telemetry attribute-key set is computed once by the caller, not derived independently by each backend

<!-- markdownlint-disable MD036 -->

**Title**
The required telemetry attribute-key set is computed once by the caller, not derived
independently by each backend

**Lens**: ARCH

**Status**: planned

**Description**
`OpenApiCoverageCalculationServiceImpl` — the sole caller of
`OpenTelemetryService.findOpenTelemetryTracingData` — computes the calculation's required
telemetry attribute-key set (`SW-021`) once, before invoking whichever backend implementation
is active, and passes it as a new parameter on the interface method.
Neither
`TempoTelemetryServiceImpl` nor `InfluxDBTelemetryServiceImpl` derives this set itself; each only
consumes the key set it is given to shape its own query (a TraceQL `select()` clause for Tempo, a
narrowed Flux `_value` object for InfluxDB — `SW-022`, `SW-023`).

**Rationale**
The required-key computation depends only on the fixed calculator inputs and the target
`OpenAPI` spec's operations — neither backend-specific.
Deriving it once, in the layer that
already holds the parsed `OpenAPI` spec (`OpenApiTestContext.openAPI()`, available before
`enrichWithOpenTelemetryData` runs), keeps `ARCH-001`'s pluggable-backend shape intact: adding a
third backend later means implementing the query-shaping half of this concern for that backend,
not re-deriving what the required keys are.
Deriving it independently per backend would let the
two implementations drift — for example if one backend's derivation is updated for a new
calculator's attribute need and the other is not, only one backend would start passing telemetry
with the new key attached, and the divergence would surface as a silent coverage gap on the
lagging backend rather than a visible failure.

This is a deliberate, accepted trade-off, not an oversight: the required-key set itself remains a
fixed enumeration (`SW-021`) rather than something each calculator declares for itself.
A
calculator added later that reads an attribute key outside today's fixed set must have that key
added to the enumeration in the same change — nothing enforces this automatically.
Making the
computation self-maintaining (each calculator contributing its own required keys, aggregated by
the coordinator) was considered and rejected for this decision: it would touch every existing
calculator for a set of 14 that has been stable, trading a small, explicit, auditable enumeration
for broader, harder-to-review surface area, for a staleness risk that a code-review checklist item
already covers cheaply enough.

**Verification Description**
A unit test on `OpenApiCoverageCalculationServiceImpl.enrichWithOpenTelemetryData` asserts the
required-key set is computed from the `OpenApiTestContext`'s `OpenAPI` spec and passed through to
`OpenTelemetryService.findOpenTelemetryTracingData` (verified via a mock backend capturing the
argument), rather than either backend implementation computing it independently — confirmed by
neither implementation class referencing the `OpenAPI` spec's operations directly for this
purpose.

## Relations

**Related**

- [SW-021](SW-021-required-attribute-key-set-derivation.md) — what the computed key set
  contains
- [SW-022](SW-022-tempo-search-returns-only-required-keys.md) — the Tempo consumer of the
  computed key set
- [SW-023](SW-023-influxdb-query-narrows-attributes-to-required-keys.md) — the InfluxDB
  consumer of the computed key set
- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the pluggable-backend
  shape this decision preserves; its documented `OpenTelemetryService` signature needs amending to
  include the new parameter once this lands
