# ADR-0002: A test's identity is an attribute on the span, read where the spans are held

- Status: Proposed
- Date: 2026-09-23
- Deciders: @bbortt

## Context

Acceptance criterion 3 of #1642 says a covered finding lists the traces that satisfied it, and that
"where the span carried a test identifier, the test name is shown; otherwise a truncated trace ID".

`ADR-0001` treated the first half of that sentence as a display concern and deferred it: findings
would persist `Set<String> traceIds`, a renderer would prefer a name where one happened to exist,
and a first-class test identifier on the span would arrive with #2011.
Review rejected that split, correctly.
Two facts make it untenable.

**The span exists in exactly one place.**
`openapi-coverage-stream`'s calculators are the only component that ever holds an
`OpenTelemetryData` next to the target it satisfied.
Downstream of `AbstractOpenApiCoverageCalculator`, a finding is a DTO field, then a row, then JSON;
the span is gone and the correlation that produced the verdict is not recoverable from any of them.
A renderer given a trace id has nothing to derive a name from — a trace id is not a name, and the
process that knew the name has exited.
Recovering it would mean a second telemetry query per evidencing trace, which `NF-006` bounds
against, and which cannot work at all when the attribute was never requested: `SW-021`'s
required-key set narrows what Tempo's `select()` and InfluxDB's projection return, so an attribute
outside that set does not reach the service in the first place.

**Establishing the convention is not what #2011 is about.**
#2011 ("Shift-Left Snow-White") is about using Snow-White earlier in the development lifecycle —
against classical Spring Boot integration tests rather than only a deployed black-box suite, moving
the correlation left from a blackbox level to a whitebox one.
That is a change to how Snow-White is used, not primarily a change to Snow-White.
A whitebox harness would _consume_ a test-identity convention; it is a poor place to invent one,
and inventing it there means reopening the matching logic of all 14 criteria a second time to thread
an attribute through — the expensive half of `STR-017` paid twice.

`STR-017` is also about to publish the shape.
`SW-031` widens the `ApiTestResult` component of `v1-report-api.yml` — a versioned contract that
consumers generate clients from, with this repo's own `toolkit/openapi-generator` among the
generators.
Adding a field to a published object is additive; changing `traceIds: string[]` into
something that can hold a name is not.
This is the same argument that put #1642 ahead of #2009 in the first place: take the rework where
it is internal, not where it is a contract.

What #1642 does _not_ have to own is producing the identity end to end in one sweep.

## Decision

**D1 — An evidence entry is a `(traceId, testCaseName)` pair, from the calculator to the JSON.**
A finding's evidence is not a set of trace ids but a list of entries carrying a required `traceId`
and a nullable `testCaseName`, uniform across the calculator's output, the `OpenApiTestResult`
event, the persisted evidence table and the `v1-report-api.yml` response (`ARCH-011`, `ARCH-012`,
`SW-031`).
Entries are distinct by the pair.
Over telemetry carrying no identity — every span today — the entry count equals the distinct trace
count, so the shape adds no rows and no cardinality before it is used.
`testCaseName` serialises as `null`, never omitted, so a consumer's fallback branch is exercisable
from the first release.

**D2 — The identity is the OpenTelemetry `test.case.name` attribute, adopted not invented.**
Snow-White reads the upstream semantic convention's `test.case.name`, documented in
`semantic-convention/` as an adopted convention, under an operator-configurable key defaulting to
`test.case.name` (`SW-032`).
The value is opaque: copied verbatim, never parsed into suite and case, never shortened.
`test.suite.name` and the two status attributes of the same upstream group are not read.

**D3 — It is never a correlation key.**
No verdict, ratio or report status depends on the attribute.
The same telemetry with and without it produces identical findings and an identical coverage ratio
for all 14 criteria; it changes evidence and nothing else.

**D4 — It reaches the span as baggage, and Snow-White reads it at the match.**
The test runner sets a `test.case.name` baggage entry, W3C `baggage` propagation carries it with the
trace context, and the system under test copies it onto its server span — for an OpenTelemetry Java
agent, via `otel.java.experimental.span-attributes.copy-from-baggage.include`, which is
configuration and no application code (`ARCH-013`, keeping `STK-003`).
The attribute joins `SW-021`'s required-key set so it is actually fetched, and the calculator reads
it from the span it already has, at the match (`ARCH-011`).

**D5 — The producing and consuming halves ship separately, the shape does not.**
The pair shape and the nullable column land with the findings model, before `SW-031` publishes it.
Emitting baggage from a test harness, configuring the copy, adding the key to the required set and
populating `testCaseName` land afterwards, as their own steps, against a contract that already has
room for them.

## Consequences

The shape decision is paid once and early: the column is nullable on an empty table and the JSON
field is null on every entry until something fills it, which costs a migration nothing and costs the
published contract nothing later.
Every consumer — the phase 2 drilldown, the CLI, anything generated from `v1-report-api.yml` —
writes its name-or-trace-id fallback once, against a field that exists, rather than being rewritten
when the name arrives.

The mechanism costs a consuming service one environment variable and no code, which is what keeps
`STK-003` true, and it is the same mechanism in both settings that matter: a black-box suite driving
a deployed service (`STK-002`) sets a header, and #2011's in-process whitebox harness sets a baggage
entry against an in-process SDK. #2011 is thereby narrowed to what it is actually about, and gets
this for free.

What becomes harder is honest to state.
The identity only appears where the consumer opts in on both ends — the runner sets baggage, the
service copies it — so `testCaseName` is nullable forever and every reader needs a fallback.
Snow-White cannot make a span carry a name nobody put there, and inferring one would produce
evidence that is plausible rather than true, which `ARCH-011` already rejects.
`otel.java.experimental.span-attributes.copy-from-baggage.include` is also an experimental agent
property; the attribute key being operator-configurable absorbs a rename, but the copy mechanism
itself should be verified against the pinned agent version (2.31.1 in
`examples/example-spring-boot`) before `ARCH-013`'s application test is written, and a
`BaggageSpanProcessor` from `opentelemetry-java-contrib` is the documented fallback if the property
has moved.
`test.case.name` is `Development`-stability upstream; that is acceptable for an attribute that is
read rather than written and correlated on by nothing (D3).

`SW-021` gains its first member that exists for evidence rather than for a verdict, which slightly
widens what `ARCH-007`'s accepted staleness risk covers — the enumeration now has to stay in sync
with what calculators _record_, not only with what they _judge_.

Reversibility is asymmetric, which is the point.
Dropping the mechanism later leaves a nullable field that is always null — harmless.
Shipping `traceIds: string[]` first and adding names later means a `v2` component or a parallel
array zipped by position.

## Relations

- Supplements [ADR-0001](ADR-0001-coverage-is-a-projection-of-per-target-findings.md) — amends its
  D2 and one of its "out of scope" boundaries
- Realized by
  [ARCH-013](../spec/specs/ARCH-013-test-identity-travels-as-baggage.md),
  [SW-032](../spec/specs/SW-032-test-identity-on-the-span.md)
- Amends [SW-021](../spec/specs/SW-021-required-attribute-key-set-derivation.md)
- Carried by [STR-017](../spec/stories/STR-017-api-test-findings-back-every-criterion-coverage.md)
