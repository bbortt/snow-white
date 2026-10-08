# InfluxDB and Grafana Tempo are mutually exclusive, pluggable telemetry backends behind one interface

<!-- markdownlint-disable MD036 -->

**Title**
InfluxDB and Grafana Tempo are mutually exclusive, pluggable telemetry backends behind one
interface

**Lens**: ARCH

**Status**: active

**Description**
`openapi-coverage-stream` integrates with exactly one telemetry backend at a time — InfluxDB or
Grafana Tempo — selected at startup by which backend's connection properties are configured, never
by a request-time choice.
`InfluxDBTelemetryServiceImpl` activates when `influxdb.url`, `influxdb.token`, `influxdb.org`, and
`influxdb.bucket` are all set.
`TempoTelemetryServiceImpl` activates under `TempoConfiguredCondition`: `tempo.url` together with
either `tempo.token` or both `tempo.username` and `tempo.password`.
Both implement the single `OpenTelemetryService` interface
(`findOpenTelemetryTracingData(ApiInformation, long, String, Set<AttributeFilter>)`), and every
consumer — the calculators, the calculation service, the Kafka Streams processor — depends on that
interface alone, injected as a single non-collection field; none of them know or care which backend
answered a query.
Configuring both backends' properties at once, or neither, is a deployment-configuration error: with
a single-valued constructor dependency on `OpenTelemetryService`, Spring context startup fails
either way — two matching beans when both are configured, none when neither is.

**Rationale**
Operators adopting Snow-White typically already run a tracing backend of their own choosing; an
organization standardized on Tempo should not need to stand up InfluxDB just for this service, and
vice versa.
Expressing the choice as two mutually exclusive `@Conditional` beans behind one interface, rather
than a runtime backend-selection branch, keeps every other part of the service backend-agnostic —
adding a third backend later means adding one more `OpenTelemetryService` implementation and its
`@Conditional`, not touching the calculation or messaging code.
Failing context startup on a misconfiguration (both or neither backend configured) surfaces the
mistake at deploy time rather than as a runtime `NoSuchBeanDefinitionException` mid-request.

**Verification Description**
A Spring context test starts the application with only InfluxDB properties set and asserts
`InfluxDBTelemetryServiceImpl` is the sole `OpenTelemetryService` bean; a second run with only Tempo
properties set asserts the reverse; a third run with both, and a fourth with neither, each assert
context startup fails.

## Relations

**Related**

- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the component that depends on this
  interface without knowing which backend implements it
- [NF-006](NF-006-bounded-telemetry-fetch-footprint.md) — a constraint both backend
  implementations must independently satisfy
- [SYS-004](SYS-004-telemetry-ingestion-and-correlation.md) — names both backends in
  passing; this spec pins the selection mechanism and exclusivity itself
- [ARCH-007](ARCH-007-required-attribute-keys-computed-once-by-caller.md) — adds a required
  attribute-key-set parameter to the `OpenTelemetryService` interface this spec documents; the
  signature quoted above is pre-`ARCH-007` and needs updating once that spec's implementation lands

## Changes

- **2026-09-19** — Added `ARCH-007` as a related spec.
  It extends the `OpenTelemetryService` interface signature this spec documents with a new
  parameter; the signature itself is left unchanged here until `ARCH-007`'s implementation lands.
