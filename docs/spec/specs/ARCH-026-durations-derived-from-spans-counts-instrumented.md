# Durations are derived from spans in the collector; only counts and gauges are instrumented in application code

<!-- markdownlint-disable MD036 -->

**Title**
Durations are derived from spans in the collector; only counts and gauges are instrumented in
application code

**Lens**: ARCH

**Status**: planned

**Description**
Snow-White's domain metrics come from two mechanisms, and which one a metric uses is decided by
what it measures rather than by preference.

A **duration a span already covers** is never instrumented in application code.
It is derived in the OpenTelemetry Collector from the spans Snow-White already emits, by the
`span_metrics` connector, which also yields the call count of the same span.
Where a duration needs a span boundary that does not exist, the increment adds the span
annotation, not a timer.

One duration has no span to add the annotation to, and is the named exception this spec carves out.
The end-to-end report duration of
[SW-059](SW-059-end-to-end-duration-from-the-triggering-instant.md) starts when a calculation is
triggered and ends when its last result is linked, spanning two services and an HTTP span that has
long since returned, so it is recorded in application code from the instant the report carries to
the moment it reaches a terminal status.
That is a recorded elapsed value and not a timer wrapped around a processing stage, which is what
the exclusion below forbids.

A **count of something inside a batch**, and a **gauge of state**, are instrumented in application
code as Micrometer meters.
The OpenTelemetry Java agent's `micrometer-1.5` instrumentation bridges those meters to OTLP, so
no metrics exporter, registry dependency, or scrape endpoint is added.

This excludes three alternatives:

- A Micrometer `Timer` or manual stopwatch around any processing stage.
- A `micrometer-registry-prometheus` registry with a scrape endpoint on the management surface
  ([ARCH-018](ARCH-018-management-surface-without-a-business-one.md)).
  Metrics leave by OTLP, like every other signal.
- Deriving batch counts from spans.

**Metric names.**
Every metric Snow-White emits is namespaced `snow.white.`, and so is every attribute key this
increment introduces.
That namespace is not chosen here; it is the one Snow-White's own span attributes already use —
`snow.white.test.case.name` and `snow.white.operation.id` — and metrics join it rather than opening
a second one.
Segments are dot-separated throughout, with no underscores, to match those keys.

The reverse-DNS form `io.github.bbortt.snow.white` is deliberately not used.
It appears exactly once in the repository, as the collector's `service.namespace` resource
attribute, and that is the right place for publisher identity: it is already attached to every
signal and can be grouped by, whereas the same twenty characters in a metric name would repeat it
where nothing can query it.

Names are fixed here because they are the operator-visible contract: the agent's Micrometer bridge
runs with `otel.instrumentation.micrometer.prometheus-mode.enabled` at its default of `false`, so a
meter's registered name is the name that arrives, unrenamed.

The derived pair is named once, through the connector's `namespace`, because one connector emits
one call counter and one duration histogram for every span it reads — the stages are **series of
that histogram keyed by span name**, not metrics of their own:

| Metric                           | Kind      | Mechanism | Unit              |
| -------------------------------- | --------- | --------- | ----------------- |
| `snow.white.span.calls`          | counter   | derived   | `{span}`          |
| `snow.white.span.duration`       | histogram | derived   | `s`               |
| `snow.white.sync.specifications` | counter   | code      | `{specification}` |
| `snow.white.report.backlog.age`  | gauge     | code      | `s`               |
| `snow.white.report.timeouts`     | counter   | code      | `{report}`        |
| `snow.white.report.duration`     | histogram | code      | `s`               |

Durations are in **seconds** on both sides.
The agent's Micrometer bridge already uses seconds as its base time unit, and the connector's
histogram unit is set to match rather than left at its millisecond default, so a dashboard never
mixes the two scales.

**Rationale**
A span ends on the exception path as well as the success path, carrying its duration and status
either way.
A derived histogram therefore includes the slow failures by construction.
A hand-written timer around a `try` block does not: it records only the branch it wraps, and the
slowest case in the coverage stream is precisely a failure — an unreachable telemetry backend
timing out.
Getting that right by hand means instrumenting every `catch` branch and remembering to do so again
for the next one added, which is a durable correctness cost for no gain over the derivation.

The split is forced, not chosen, for the other direction.
The connector counts **spans**, not values inside them.
A sync cycle emits one span carrying a map of ten status counts, and a housekeeping run emits one
span carrying the number of reports it swept; derived counters would report one cycle and one run.
A backlog gauge has no span to derive from at all, because it describes state between requests
rather than a request.

Micrometer rather than the OpenTelemetry Metrics API directly, for the meters that remain.
Exactly two modules need one in this increment: `report-coordinator-api`, which already carries
`spring-boot-starter-actuator` and therefore both `micrometer-core` and an auto-configured
`MeterRegistry`, and `api-sync-job`, which carries neither actuator nor the OpenTelemetry starter
and is the one module where a dependency is added deliberately.

Which registry that is cannot be left to Spring there.
`micrometer-core` on its own supplies no registry bean — that auto-configuration arrives with
actuator, in `spring-boot-actuator-autoconfigure` — and the agent's `micrometer-1.5`
instrumentation bridges `Metrics.globalRegistry` rather than whichever registry a bean happens to
hold.
`api-sync-job` therefore exposes `Metrics.globalRegistry` as its `MeterRegistry` bean, which is one
line and makes both things true at once: the meter reaches the agent, and the class registering it
still takes a registry as a constructor argument.
A registry the agent does not bridge is the failure this forecloses — it satisfies every local
assertion and exports nothing.

That constructor argument is the reason for Micrometer over the OpenTelemetry Metrics API: a unit
test can pass a `SimpleMeterRegistry` with no agent attached, which an agent-bridged OpenTelemetry
`Meter` cannot offer as cheaply.

The other actuator-carrying modules — `api-gateway`, `api-index-api`, `openapi-coverage-stream`,
`quality-gate-api`, and `mcp-api` — need no meter at all here.
`openapi-coverage-stream` is worth naming: it is the most heavily instrumented service in this
increment and every one of its metrics is derived, so it gains no registry interaction.

**Verification Description**
A review confirms no production source registers a `Timer`, `LongTaskTimer`, or manual elapsed-time
measurement for a processing stage, and that no module declares a `micrometer-registry-*`
dependency.
A review confirms each metric fixed by a sibling spec is implemented by the mechanism this spec
assigns to its kind.
An integration or application test asserts the code-instrumented counters and gauges are
registered and move, driven through a `MeterRegistry` rather than through an exported metric.
A review confirms every module registering a meter does so on a registry the agent bridges, which
for `api-sync-job` means its `MeterRegistry` bean is `Metrics.globalRegistry`.
The derived durations are verified where they are produced, by
[ARCH-027](ARCH-027-connector-reads-only-snow-whites-own-spans.md).

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality this division of
  labour serves

**Related**

- [ARCH-027](ARCH-027-connector-reads-only-snow-whites-own-spans.md) — where the
  derivation runs, and which spans may feed it
- [CON-017](CON-017-metric-dimensions-are-bounded.md) — the cardinality invariant both
  mechanisms must hold
- [CON-018](CON-018-own-traces-are-never-head-sampled.md) — the precondition that makes a
  derived duration a measurement rather than an estimate
- [ARCH-018](ARCH-018-management-surface-without-a-business-one.md) — the management surface these
  metrics deliberately do not join
- [NF-005](NF-005-clear-failure-feedback.md) — the failure feedback a derived histogram keeps
  honest by including the failures
