# Every coverage calculation records a bounded outcome on its span, so a degraded dependency is distinguishable from a failing gate

<!-- markdownlint-disable MD036 -->

**Title**
Every coverage calculation records a bounded outcome on its span, so a degraded dependency is
distinguishable from a failing gate

**Lens**: SW

**Status**: planned

**Description**
Each coverage calculation sets the bounded `snow.white.outcome` attribute on its span, and the
connector promotes that attribute to a dimension of the metrics it derives.
The connector's `dimensions` list is global rather than per-metric, so the attribute reaches both:
the `snow.white.span.calls` counter, which is what this spec is for, and the
`snow.white.span.duration` histogram, which gains a split by outcome at no extra cost and answers
whether the failures are also the slow ones.
There is no separate outcome counter: the derived call count, split by this attribute, _is_ the
outcome counter.
The value set is closed, and each value names a branch the coverage stream takes:

- `ok` — coverage was computed and published.
- `telemetry_backend_unavailable`
- `no_telemetry_in_window` — the backend returned nothing for the lookback window and the
  configured filters.
  One value rather than two: the fetch is a single query, so no branch can separate "the
  application never appeared" from "the filters matched none of what did".
- `only_harness_telemetry` — telemetry arrived and matched, and every matched span is the test
  harness's own.
  This is what `NoApplicationTelemetryException` reports, thrown where the grouping first makes a
  harness span distinguishable from the application's.
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

Six of the eight values need no invention.
The processor already catches `TelemetryBackendUnavailableException`,
`UnmatchedWaiverException`, and `NoApplicationTelemetryException` by name, and comments on why each
is worth separating; the empty-telemetry case already returns its own event; `ok` is the path that
returns a computed response; and `other` is the fallback the processor already has.

`not_indexed` and `unparseable_specification` are the two this spec adds.
`OpenApiNotIndexedException` and `UnparseableOpenApiException` are declared on
`processOpenApiCoverageRequest` but reach no branch of their own — both land in the
`catch (Exception)` fallback — so each needs its own catch branch before it can carry anything but
`other`.
Recording them as `other` instead is the cheaper option and is rejected: they are the two failures
whose owner is unambiguous, one pointing at the index and one at the specification's author.

Putting it on the span rather than in a Micrometer counter keeps one mechanism for the stream's
telemetry and yields the duration of each failure class at no extra cost — a timeout and a refusal
have very different latencies, and knowing which is slow is the next question after knowing which
is frequent.
Span status alone is insufficient: it offers `ok` and `error`, which collapses the seven failure
values above into one.

`only_harness_telemetry` is separated from `no_telemetry_in_window` because the operator actions
diverge completely.
The first means the window, the filters, and the ingestion all worked, and the application under
test is not instrumented — which is not Snow-White's problem to fix at all.
The second means nothing arrived, and the lookback window and the filters are the first two things
to check.
Collapsing them would put the one case Snow-White cannot fix behind the same value as the one it
usually can.

**Verification Description**
A unit test drives the processor through each branch and asserts the span carries the expected
outcome value, including `ok` on the success path.
A test asserts an empty telemetry result yields `no_telemetry_in_window` and a window carrying
only harness spans yields `only_harness_telemetry`, since those two are the pair most easily
wired to each other's value.
A test asserts `OpenApiNotIndexedException` and `UnparseableOpenApiException` yield their own
values rather than `other`, which is what the two added catch branches exist for.
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
