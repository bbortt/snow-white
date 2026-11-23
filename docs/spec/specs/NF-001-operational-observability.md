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
Alongside the JVM and system metrics the OpenTelemetry Java agent provides by default, Snow-White
emits domain metrics of its own: durations derived in the collector from its own spans, and counts
and gauges instrumented in application code where no span can carry them
([ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md)).
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
For metrics specifically, that diagnosis is what the verifications of
[ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md),
[ARCH-027](ARCH-027-connector-reads-only-snow-whites-own-spans.md),
[SW-055](SW-055-coverage-phases-are-the-span-boundary.md) through
[SW-059](SW-059-end-to-end-duration-from-the-triggering-instant.md),
[CON-017](CON-017-metric-dimensions-are-bounded.md) and
[CON-018](CON-018-own-traces-are-never-head-sampled.md) establish.

## Relations

**Related**

- [SYS-004](SYS-004-telemetry-ingestion-and-correlation.md) — one of the stages this
  covers

## Changes

- **2026-11-23** — Amended by `STR-030`: metrics are no longer absent.
  Two sentences stated that absence as current fact and both became false with that increment.
  The Description had recorded that metrics were limited to what the agent provides by default and
  that no custom, domain-specific metric was emitted yet; it now states what is emitted, and defers
  the division between the collector-derived and code-instrumented halves to `ARCH-026` rather than
  restating it.
  The Verification Description had deferred metric verification until such metrics existed; it now
  points at the nine specs that carry it.
  Nothing about the quality this spec requires changed — only the statement of how far it had been
  reached.
  The Relations section is deliberately untouched: the nine new specs realize this one and name it
  themselves, which is the direction relations run in this corpus, and listing them here would
  invert it for one spec alone.
