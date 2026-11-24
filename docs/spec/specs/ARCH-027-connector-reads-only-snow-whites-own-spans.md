# The metrics connector reads only Snow-White's own spans, on a pipeline that does not depend on trace export

<!-- markdownlint-disable MD036 -->

**Title**
The metrics connector reads only Snow-White's own spans, on a pipeline that does not depend on
trace export

**Lens**: ARCH

**Status**: planned

**Description**
Both collector configurations — the development stack's `dev/otel-collector/config.yaml` and the
Helm chart's collector ConfigMap — run the `span_metrics` connector on a traces pipeline dedicated
to it.
The two files share the admission filters — `filter/all-external-services` and
`filter/snow-white-microservices` are identical in both — but nothing else about their pipelines
is interchangeable: the chart adds `k8s_attributes`, the development config adds
`groupbyattrs/service` and exports through an `otlp_grpc/grafana` the chart does not define.
This spec therefore states what each configuration must contain, not that one can be copied into
the other.

That pipeline is carved out explicitly rather than hung off an existing one: a `traces/span-metrics`
pipeline that receives from `otlp` and exports to the connector alone.
Its processors are `memory_limiter`, as every pipeline in both files begins with, and the existing
`filter/all-external-services`, which admits Snow-White's own `service.name` values and nothing
else.
That filter is load-bearing rather than defensive, and it must not be read as redundant because the
pipeline has a single receiver: `otlp` is the same receiver that feeds `traces/otel-event-filter`,
so it carries every span of every customer application under test.
The filter is the pipeline's only admission control, and removing it admits one series per customer
service and endpoint — the cardinality blowup [CON-017](CON-017-metric-dimensions-are-bounded.md)
forbids.

That processor's list names all eight microservices, `api-index-api` and `mcp-api` included.
Both were absent from it, so their telemetry was discarded as a customer application's and the
connector would have derived no metric at all for either — a dashboard missing two services while
looking complete, which is the failure this spec exists to prevent.
`filter/snow-white-microservices` is the complement of that list, and the two name the same eight
services in both configurations: a name added to one belongs in the other.
The pipeline carries no `groupbytrace`, which is what **ahead of `groupbytrace`** means here: the
connector sees spans before anything buffers them.

In the chart, the pipeline is **not** gated on
`otelCollector.connectToExternalOtelCollector.exportTraces`.
Its derived metrics join the chart's existing metrics pipeline, so a deployment that forwards
metrics receives them whether or not it forwards traces.

It is gated on `exportMetrics`, and has to be: a connector whose metrics no pipeline receives makes
the collector refuse to start, and `metrics/infra` is the only consumer the chart has.
Creating the connector unconditionally while its consumer is conditional would put the bundled
collector in a crash loop and take the ingestion pipelines down with it, for a deployment that had
merely declined to forward metrics.
Gating both together costs nothing, because a deployment that forwards no metrics has nowhere for
the derived ones to go either.

Three span sources are excluded, and the exclusion is the point of this spec:

- The ingestion pipelines `traces/otel-event-filter` and `traces/snow-white`, which exist in both
  configurations.
  These carry the spans of the customer applications under test.
- The whole of `otel-collector/config.yaml`, the third configuration, which the root
  `docker-compose.yaml` mounts.
  It gains no connector at all.
  Despite the similar path it is not the development stack: it replays recorded spans from
  `otlp_json_file` through a `traces/replay` pipeline **and** serves a live stack, every service in
  the root compose file pointing its `OTEL_EXPORTER_OTLP_ENDPOINT` at it.
  Its `traces/infra`, `metrics/infra` and `logs/infra` pipelines apply no service filter at all, so
  Snow-White's spans and its customers' arrive on the same pipelines undifferentiated.
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

The root configuration is excluded for a subtler reason, and its path is the trap.
`otel-collector/config.yaml` sits at the repository root and reads as the obvious development
collector, and because it also serves a live stack the temptation is to treat it as one.
But `transform/replay` rewrites a recorded span's start and end timestamps to the moment it is read
while **preserving its duration**, so replayed spans would enter a duration histogram carrying
real-looking latencies at a fabricated rate — mixed, on the same unfiltered infra pipelines, with
live spans from Snow-White and its customers alike.
A dashboard that is plausible and wrong is worse than one that is empty.
Giving that configuration a trustworthy connector would mean filtering it and separating replay
from live first, which is a larger change than this spec, and the root stack is not where anyone
reads operational dashboards.

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
A review of `dev/otel-collector/config.yaml` and the chart's ConfigMap confirms each carries a
dedicated `traces/span-metrics` pipeline whose processors are `filter/all-external-services` and no
`groupbytrace`, and that the connector is absent from the ingestion pipelines.
A review confirms `filter/all-external-services` names every Snow-White service, `api-index-api`
and `mcp-api` included, and names them identically in both configurations.
A review confirms `otel-collector/config.yaml` carries no connector.
A Helm template test renders the chart with `connectToExternalOtelCollector.exportTraces` disabled
and `exportMetrics` enabled, and asserts the connector and its pipeline are still present and
wired to the metrics exporters.
A second rendering with `exportMetrics` disabled asserts the connector and its pipeline are absent
together, so no rendering leaves a connector without a consuming pipeline.
A third rendering with the bundled ingestion disabled asserts the derivation is unaffected.
An integration check asserts a derived duration metric appears for a Snow-White span, and that no
series exists for a service name outside Snow-White's own set.
The development stack has no pull reader to scrape, so the check reads the derived metrics where it
can see them — a collector run with the connector's metrics routed to a debug or Prometheus
exporter is enough, and is how the connector's behaviour was established for this spec.
The connector emits on its own `metrics_flush_interval`, 60 seconds by default, so the check either
lowers that interval or waits it out — a scrape taken immediately after the span returns nothing,
which is not a failure.

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
