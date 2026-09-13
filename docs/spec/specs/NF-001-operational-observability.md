# Operational telemetry sufficient to diagnose ingestion, correlation, and analysis behavior

<!-- markdownlint-disable MD036 -->

**Title**
Operational telemetry sufficient to diagnose ingestion, correlation, and analysis behavior

**Lens**: NF

**Status**: active

**Description**
Snow-White exposes its own operational telemetry — logs, metrics, traces of its internal
processing — covering spec ingestion, telemetry correlation, and analysis, so an operator can
diagnose a stuck or incorrect run without reading source code.
Metrics today are limited to the JVM and system metrics the OpenTelemetry Java agent's
auto-instrumentation provides by default; no custom, domain-specific metric is emitted yet.
Logs and traces already cover the internal processing stages.

**Rationale**
A system built to observe other services' behavior would be self-undermining if its own failures
were opaque; the same OpenTelemetry-first posture Snow-White asks of its consumers applies to
Snow-White's own operation.

**Verification Description**
A review confirms each microservice emits structured logs and OpenTelemetry spans for its
processing stages.
A deliberately induced failure (e.g. an unreachable telemetry backend) must be diagnosable from
those signals alone.
Verifying this for custom metrics specifically is deferred until such metrics exist.

## Relations

**Related**

- [SYS-004](SYS-004-telemetry-ingestion-and-correlation.md) — one of the stages this
  covers
