# The metrics connector reads only Snow-White's own spans, on a pipeline that does not depend on trace export

<!-- markdownlint-disable MD036 -->

**Title**
The metrics connector reads only Snow-White's own spans, on a pipeline that does not depend on
trace export

**Lens**: ARCH

**Status**: planned

**Description**
Both collector configurations — the development stack's `otel-collector/config.yaml` and the Helm
chart's collector ConfigMap — run the `spanmetrics` connector on a traces pipeline dedicated to
it.

That pipeline admits spans from Snow-White's own services only, and the connector sits **ahead of
`groupbytrace`**.

In the chart, the pipeline is created unconditionally with the bundled collector.
It is **not** gated on `otelCollector.connectToExternalOtelCollector.exportTraces`.
Its derived metrics join the chart's existing metrics pipeline, so a deployment that forwards
metrics receives them whether or not it forwards traces.

Three span sources are excluded, and the exclusion is the point of this spec:

- The chart's ingestion pipelines, `traces/otel-event-filter` and `traces/snow-white`.
  These carry the spans of the customer applications under test.
- The development stack's `traces/replay` pipeline, which reads recorded spans from
  `/data/*.json`.
- Any pipeline downstream of `groupbytrace`.

**Rationale**
The chart already separates the two span populations, and in the right direction: the
`filter/all-external-services` processor on the infra pipelines admits only Snow-White's own
`service.name` values, while `filter/snow-white-microservices` keeps Snow-White's own spans out of
the ingestion path.
The connector belongs on the first population.
Deriving metrics from the second would produce one series per customer service and endpoint, which
is both unbounded ([CON-017](CON-017-metric-dimensions-are-bounded.md)) and meaningless —
the applications under test are the subject of Snow-White's analysis, not of its own operation.

The replay pipeline is excluded for a subtler reason.
`transform/replay` rewrites a recorded span's start and end timestamps to the moment it is read
while **preserving its duration**, so replayed spans would enter a duration histogram carrying
real-looking latencies at a fabricated rate.
A dashboard that is plausible and wrong is worse than one that is empty.

Placing the connector ahead of `groupbytrace` keeps the derivation independent of that processor's
buffering.
`groupbytrace` holds traces with a `wait_duration` and a `num_traces` bound, and anything that
evicts or flushes early under load would bias exactly the counts an operator consults under load.

Not gating on `exportTraces` is what makes the metrics reach customers for free.
A deployment may reasonably forward metrics and not traces — traces are the higher-volume signal
and the likelier one to be declined on cost.
Hanging the derivation off the trace pipeline would make the metrics vanish for that deployment
with no error and no mention in the values file, which is a support question rather than a
configuration choice.

**Verification Description**
A review of both collector configurations confirms the connector's input pipeline lists only
Snow-White's own services, sits ahead of `groupbytrace`, and is absent from the ingestion and
replay pipelines.
A Helm template test renders the chart with `connectToExternalOtelCollector.exportTraces` disabled
and `exportMetrics` enabled, and asserts the connector and its pipeline are still present and
wired to the metrics exporters.
A second rendering with the bundled ingestion disabled asserts the derivation is unaffected.
An integration check against the development stack asserts a derived duration metric appears for a
Snow-White span, and that no series exists for a service name outside Snow-White's own set.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality the derived durations
  serve

**Related**

- [ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md) — the division
  of labour this spec places in the collector
- [CON-017](CON-017-metric-dimensions-are-bounded.md) — the cardinality invariant that
  rules out the customer-span population
- [CON-018](CON-018-own-traces-are-never-head-sampled.md) — the sampling precondition the
  connector's accuracy rests on
- [SYS-005](SYS-005-scoped-telemetry-ingestion.md) — the ingestion scoping whose pipelines this
  spec keeps the connector away from
- [SW-055](SW-055-coverage-phases-are-the-span-boundary.md) — the spans whose durations
  this pipeline turns into metrics
