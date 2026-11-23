# Every coverage calculation records a bounded outcome on its span, so a degraded dependency is distinguishable from a failing gate

<!-- markdownlint-disable MD036 -->

**Title**
Every coverage calculation records a bounded outcome on its span, so a degraded dependency is
distinguishable from a failing gate

**Lens**: SW

**Status**: planned

**Description**
Each coverage calculation sets the bounded `snow.white.outcome` attribute on its span, and the
connector promotes that attribute to a dimension of the derived `snow.white.span.calls` counter.
There is no separate outcome counter: the derived call count, split by this attribute, _is_ the
outcome counter.
The value set is closed, and mirrors the branches
`OpenApiCoverageCalculationProcessor` already distinguishes by name:

- `ok` — coverage was computed and published.
- `telemetry_backend_unavailable`
- `no_application_telemetry` — the application never appeared in the lookback window.
- `no_telemetry_for_filters` — telemetry existed, but the configured attribute filters matched
  none of it.
- `unmatched_waiver`
- `not_indexed`
- `unparseable_specification`
- `other` — the fallback branch.

The success path sets the attribute to `ok` rather than leaving it unset, so the counter's
denominator is the calculation count and a ratio needs no second metric.

The attribute carries the **reason class only**.
The root cause message stays in the log line that already reports it and is never promoted to a
metric dimension ([CON-017](CON-017-metric-dimensions-are-bounded.md)).

**Rationale**
Every failure branch answers the request with a failure event rather than stalling the stream.
That is deliberate — it is what keeps a single bad request from blocking a partition — but it means
a systemic failure and a legitimately failing quality gate produce the same externally visible
result: a finished report carrying no coverage.
There is consequently no error rate anywhere in the system to read, and nothing distinguishes
"Tempo is down" from "this API is poorly covered" without opening a trace.

The taxonomy needs no invention.
The processor already catches `TelemetryBackendUnavailableException`,
`UnmatchedWaiverException`, and `NoApplicationTelemetryException` by name, and comments on why each
is worth separating; `OpenApiNotIndexedException` and `UnparseableOpenApiException` are declared on
the call, and the empty-filter-result case already returns its own event.
This spec records what the code already decided, in a form something other than a human reader can
aggregate.

Putting it on the span rather than in a Micrometer counter keeps one mechanism for the stream's
telemetry and yields the duration of each failure class at no extra cost — a timeout and a refusal
have very different latencies, and knowing which is slow is the next question after knowing which
is frequent.
Span status alone is insufficient: it offers `ok` and `error`, which collapses six distinct
operator responses into one.

`no_telemetry_for_filters` is separated from `no_application_telemetry` because the operator
actions diverge completely.
The first means the filters are wrong, which is a configuration fix; the second means the
application is not instrumented or was not running, which is not Snow-White's problem to fix at
all.

**Verification Description**
A unit test drives the processor through each branch and asserts the span carries the expected
outcome value, including `ok` on the success path.
A test asserts the attribute's value set is closed — an unexpected exception type yields `other`
and never a message-derived value.
A review confirms no root cause message reaches the attribute.
An integration check asserts the derived counter separates a telemetry backend outage from a
calculation that completed with criteria below threshold.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality this outcome counter
  serves
- [NF-005](NF-005-clear-failure-feedback.md) — the clear failure feedback this extends from the
  report to the operator

**Related**

- [SW-055](SW-055-coverage-phases-are-the-span-boundary.md) — the spans this attribute
  rides on
- [CON-017](CON-017-metric-dimensions-are-bounded.md) — the invariant that keeps the
  reason a class rather than a message
- [ARCH-027](ARCH-027-connector-reads-only-snow-whites-own-spans.md) — the connector that
  promotes this one attribute to a dimension
- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the stream whose failure handling keeps
  answering rather than stalling
- [CON-002](CON-002-tolerate-dependency-outages.md) — the outage tolerance that makes a degraded
  dependency look like ordinary operation
