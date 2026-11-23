# Snow-White emits domain metrics, deriving durations from its own spans and counting what spans cannot

<!-- markdownlint-disable MD036 -->

**Title**
Snow-White emits domain metrics, deriving durations from its own spans and counting what spans
cannot

**Status**: planned

**Business Value**
An operator can put Snow-White's own behaviour on a dashboard and alert on a threshold.
Today diagnosing a stuck sync cycle, correlation pass, or coverage calculation means reading
traces and logs one request at a time, which does not scale to a running deployment and cannot
page anyone.

The metrics also reach customers without extra configuration.
The bundled collector derives and exports them wherever a deployment already forwards its
telemetry, so a customer gains the signals by upgrading rather than by instrumenting anything.

**Problem / Context**
[NF-001](../specs/NF-001-operational-observability.md) requires telemetry sufficient to diagnose
ingestion, correlation, and analysis.
Its Description records that metrics are limited to what the OpenTelemetry Java agent provides by
default — JVM and system metrics — and that no custom, domain-specific metric exists.
Logs and spans already cover the processing stages.
This increment closes that gap, and rewrites the two sentences in NF-001 that state the absence as
current fact.

Three blind spots make the gap concrete.

Every failure branch in the coverage stream answers the request with a failure event rather than
stalling the stream.
A telemetry backend outage, a specification the index does not hold, and an application that never
appeared in the lookback window all produce a finished report carrying no coverage.
From outside, each reads as a quality gate failing rather than as infrastructure degrading.

A sync cycle that published nothing because the index was unreachable looks like a cycle that had
nothing to publish.
The distinction already exists in the code as the `PUBLISH_DEFERRED` load status, and it is
already reasoned about in prose, but it reaches no aggregate an operator can watch.

A calculation whose fan-out never completes is invisible until housekeeping sweeps it to
`TIMED_OUT`.
That sweep is the lagging indicator; nothing reports the backlog while it is still forming.

**Solution Approach**
Two mechanisms, split by what each can actually measure.

Durations are derived in the OpenTelemetry Collector from spans Snow-White already emits, using
the `spanmetrics` connector.
A span records its duration on the failure path as well as the success path, so a derived
histogram cannot omit the slow failures — which a hand-written timer around a `try` block easily
does.
Where a duration needs a span boundary that does not exist yet, the increment adds the annotation
rather than a timer.

Counts and gauges are instrumented in application code, because the collector cannot derive them.
A sync cycle emits one span carrying a map of ten status counts, and a housekeeping run emits one
span carrying the number of reports it swept; a span-derived counter would count cycles and runs,
not specifications and reports.
A backlog gauge has no span at all.

Both collector configurations are changed: the development stack and the Helm chart.
In the chart the derivation runs on a pipeline of its own, filtered to Snow-White's own services,
so the metrics do not depend on a customer having opted into trace export.

Metric dimensions stay bounded.
API identity already travels on spans, and putting it on a metric would grow one series per API
version ever synced.

**Amending `NF-001` is part of this increment, not a follow-up.**
Two sentences in it state the absence of custom metrics as current fact, and both become false the
moment this increment ships.

In its Description, "Metrics today are limited to the JVM and system metrics the OpenTelemetry
Java agent's auto-instrumentation provides by default; no custom, domain-specific metric is emitted
yet." is replaced by a statement that Snow-White emits domain metrics, naming the two mechanisms
and citing `ARCH-026` for the division between them.

In its Verification Description, "Verifying this for custom metrics specifically is deferred until
such metrics exist." is replaced by a pointer to the verifications the specs below now carry.

`NF-001` also gains a `## Changes` entry dated to this increment, following the convention 78 of
the 124 existing specs already use; it has no such section today, so the amendment adds one.

What the amendment deliberately does **not** do is add back-links.
`NF-001` lists only `SYS-004` as Related, while five specs already realize it without being named
there, so relations in this corpus run one way — from the realizing spec upward.
The nine specs below follow that, and `NF-001`'s Relations section is left alone.

**Acceptance Criteria**

- Each metric named in the specs below is observable in a deployment of the development stack,
  with the dimensions its spec fixes and no others.
- A coverage calculation that fails because the telemetry backend is unreachable is
  distinguishable, from metrics alone, from one that fails because a gate's criteria were not met.
- A sync cycle during an `api-index-api` outage is distinguishable, from metrics alone, from a
  cycle that found nothing new to publish.
- A fan-out that stops receiving results raises the backlog gauge before housekeeping sweeps it,
  and raises the swept counter when it does.
- The three coverage phases — specification fetch, telemetry query, and coverage computation —
  carry separate durations, so queue time, backend time, and compute time are separable.
- A customer deployment that forwards metrics but not traces still receives the derived duration
  metrics.
- No metric carries a service name, API name, or API version as a dimension.
- `NF-001`'s Description no longer contains the sentence beginning "Metrics today are limited to
  the JVM and system metrics", and its Verification Description no longer contains the sentence
  beginning "Verifying this for custom metrics specifically is deferred".
- `NF-001` carries a `## Changes` entry for this increment, and its Relations section is unchanged.

**Out of scope**

- Dashboards and alert rules built on top of the metrics.
  Having the signals is a prerequisite, not the same work.
- Counters for `api-index-api` — indexed API totals and prerelease overrides.
  Both answer capacity and audit questions rather than diagnostic ones, and the database and logs
  already answer them.
- Spans, and therefore derived durations, for the individual coverage calculators.
  Annotating all of them would multiply span volume per calculation and produce a metric series
  per calculator for little diagnostic gain.
- Metrics duplicating what the agent's Kafka client instrumentation already emits.
  What that covers is established before any stream metric is added, so the increment does not
  reimplement consumer lag.
- A code-instrumented counter for `otel-event-filter-stream` — events admitted, filtered, or
  dropped.
  It gains the derived span durations along with every other service, and a count of what the
  filter rejects is close enough to the agent's Kafka instrumentation that establishing what that
  already provides has to come first.
  This is the one service named in issue #2035's candidate scope that this increment leaves without
  a metric of its own.
- Changing the sampling configuration.
  The increment depends on Snow-White's own traces staying unsampled and fixes that as an
  invariant, but introduces no sampler and no sampling control.

## Relations

**Realizes**

- [ARCH-026](../specs/ARCH-026-durations-derived-from-spans-counts-instrumented.md) — the
  division of labour between the collector and the application code
- [ARCH-027](../specs/ARCH-027-connector-reads-only-snow-whites-own-spans.md) — where the
  derivation runs, in both collector configurations
- [SW-055](../specs/SW-055-coverage-phases-are-the-span-boundary.md) — the span boundary
  that makes the three phase durations exist
- [SW-056](../specs/SW-056-calculation-outcome-rides-on-the-span.md) — how a failure
  reason becomes a dimension rather than a log line
- [SW-057](../specs/SW-057-sync-cycle-counts-by-load-status.md) — the cycle counter that
  separates an index outage from an empty cycle
- [SW-058](../specs/SW-058-fan-out-backlog-measured-by-age.md) — the backlog gauge and
  the swept counter
- [SW-059](../specs/SW-059-end-to-end-duration-from-the-triggering-instant.md) — the one
  duration no single span covers
- [CON-017](../specs/CON-017-metric-dimensions-are-bounded.md) — the cardinality
  invariant
- [CON-018](../specs/CON-018-own-traces-are-never-head-sampled.md) — the precondition
  that keeps a derived metric a measurement rather than an estimate

**Related**

- [NF-001](../specs/NF-001-operational-observability.md) — the quality this increment finally
  makes true for metrics, and the spec whose statement of the gap it rewrites
- [SYS-004](../specs/SYS-004-telemetry-ingestion-and-correlation.md) — the stage whose own
  behaviour becomes measurable
- [SW-045](../specs/SW-045-stream-state-decides-the-health-verdict.md) — the existing health
  verdict, which answers whether the topology runs but not whether it progresses
- [ARCH-018](../specs/ARCH-018-management-surface-without-a-business-one.md) — the narrowly scoped
  management surface these metrics deliberately do not join, travelling by OTLP instead of a
  scrape endpoint
- [ARCH-001](../specs/ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the backend
  whose latency the telemetry-query phase isolates
- [NF-009](../specs/NF-009-bounded-sync-fan-out-with-backpressure.md) — the bounded fan-out whose
  per-cycle outcome the sync counter reports
- [SW-035](../specs/SW-035-index-outage-defers-to-next-cycle.md) — the deferral this increment
  makes visible
- [SW-018](../specs/SW-018-stale-reports-time-out-not-deleted.md) — the sweep the swept counter
  reports
- [CON-002](../specs/CON-002-tolerate-dependency-outages.md) — the outage tolerance that makes a
  degraded dependency look like ordinary operation, and therefore needs a metric to surface
