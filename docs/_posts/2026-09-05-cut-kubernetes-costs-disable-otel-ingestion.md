---
title: 'Release 1.9.0: bring your own tracing backend, cut Kubernetes costs'
excerpt: >
  Already running your own OpenTelemetry pipeline or Grafana Tempo? 1.9.0 lets
  you disable Snow-White's bundled ingestion stack entirely and point
  openapi-coverage-stream straight at your existing backend.
tags:
  - release
  - operators
  - kubernetes
  - opentelemetry
---

Snow-White ships with its own OTel Collector, an `otel-event-filter-stream` deployment, and (by
default) an InfluxDB instance to store traces for coverage calculation.
That's convenient for a
first install, but it's redundant weight if your organization already runs a tracing backend
that your applications export to — you end up paying for two ingestion pipelines instead of one.

[Release 1.9.0](https://github.com/bbortt/snow-white/releases/tag/v1.9.0) fixes that for
operators.

## `otelCollector.disableIngestion`

Set this to `true` in your Helm values and Snow-White stops deploying its own ingestion path
altogether: the OTel Collector's coverage-trace pipelines, the `otel-event-filter-stream`
Deployment, and the public `/v1/traces` route are no longer created.
Snow-White's own
self-observability (its microservices' internal telemetry) is unaffected — this only removes the
stack that exists to ingest _your applications'_ traces for coverage analysis.

```yaml
otelCollector:
  disableIngestion: true
```

## Query Grafana Tempo directly

1.9.0 also lets `openapi-coverage-stream` read traces straight from an externally operated
Grafana Tempo instance instead of Snow-White's bundled InfluxDB:

```yaml
snowWhite:
  openapiCoverageStream:
    tempo:
      endpoint: 'https://tempo.example.com'
      token: 'your-bearer-token'
```

Combine both settings and Snow-White no longer runs its own trace-ingestion pipeline or
datastore at all — it just queries the Tempo you're already operating.
Fewer pods, less CPU and
memory reserved in the cluster, one less system to keep patched.

One thing Helm can't infer for you: setting `disableIngestion: true` does **not** automatically
turn off the bundled InfluxDB subchart.
If you're switching to Tempo, set `influxdb2.enabled:
false` yourself, or you'll have an InfluxDB StatefulSet running unused.

See the [Deployment Guide](/deployment/) for the full set of `otelCollector` and
`openapiCoverageStream` options.
