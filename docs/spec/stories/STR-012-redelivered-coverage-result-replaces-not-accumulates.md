# A redelivered coverage result replaces the prior one instead of accumulating beside it

<!-- markdownlint-disable MD036 -->

**Title**
A redelivered coverage result replaces the prior one instead of accumulating beside it

**Status**: done

**Business Value**
A quality-gate report must reflect the true, current coverage for every criterion — never a
crash, and never a count inflated by infrastructure retrying work it already did.
Today, a
redelivered coverage response for an API test that was already scored takes down the report
entirely, so the caller polling for a result gets neither a report nor an explanation, only a
failed Kafka listener.

**Problem / Context**
`report-coordinator-api` receives one `OpenApiCoverageResponseEvent` per `ApiTest` from
`openapi-coverage-stream` and folds its criteria results in via
`ApiTestResultLinker.addApiTestResultsToApiTest`, which `addAll`s the new `ApiTestResult`s
straight into the `ApiTest`'s result set.
`ApiTestResult`'s identity is the pair
`(apiTestCriteria, apiTest)`, but the Java class carries no `equals()`/`hashCode()` for that pair,
so the in-memory `Set` treats every new result as distinct from any prior one with the same
criteria — even one for the exact same `ApiTest` and criterion.
On the first delivery for an
`ApiTest` this is harmless, because the set starts empty.
It stops being harmless the moment the
same `ApiTest` is scored a second time: Hibernate then finds two managed Java objects mapped to
the same database row and fails the merge with "Multiple representations of the same entity",
crashing the Kafka listener outright.

A second delivery for the same `ApiTest` is not a hypothetical.
`openapi-coverage-stream`'s Kafka
Streams consumer can be fenced out of its group mid-calculation — confirmed via the
`TaskMigratedException`/`CommitFailedException` pair in a production stack trace — when
telemetry-fetch latency exceeds `max.poll.interval.ms`, most concretely on the Grafana Tempo
backend, whose current query pattern issues one HTTP round trip per matched trace on top of the
initial search.
Kafka's at-least-once delivery then reprocesses the same calculation request and
produces a second `OpenApiCoverageResponseEvent` for the same `ApiTest`, with results for the same
criteria as the first.
Nothing downstream of the event was ever designed to handle that case.

**Solution Approach**
Give `ApiTestResult` identity-aware equality on `(apiTestCriteria, apiTest)`, and change
`ApiTestResultLinker.addApiTestResultsToApiTest` to replace any existing result for a criterion
already present on the `ApiTest` with the newly delivered one, rather than adding beside it.
The
verdict is then always recomputed from a result set that holds at most one entry per criterion,
so a redelivered response updates the report in place instead of crashing it or double-counting a
criterion in the pass-rate calculation.

**Acceptance Criteria**

- A second `OpenApiCoverageResponseEvent` for an `ApiTest` that already has a persisted result for
  one or more of the same criteria does not throw, and the report remains in a terminal or
  in-progress status consistent with the data received.
- After such a redelivery, the `ApiTest`'s result set contains exactly one `ApiTestResult` per
  `apiTestCriteria` — the most recently delivered one — never two.
- The API test's verdict (`SW-016`'s pass-rate calculation) is computed from that deduplicated
  result set, so a redelivered criterion is counted once, not twice, toward the pass rate.
- A criterion present in the first delivery but absent from the second is retained unchanged — a
  redelivery only replaces the criteria it actually reports, it does not clear the rest.

**Out of scope**

- Reducing the likelihood of redelivery itself — the `openapi-coverage-stream` telemetry-fetch
  latency behind the consumer fencing (Grafana Tempo's per-trace round trips) is a separate
  concern, tracked outside this story.
- Deduplicating or making idempotent any other event type in the system — this covers only
  `OpenApiCoverageResponseEvent` results folded in by `ApiTestResultLinker`.
- Detecting or logging that a redelivery occurred as opposed to a first delivery — the fix makes
  both cases converge on the same correct end state without needing to distinguish them.

## Relations

**Realizes**

- [SW-020](../specs/SW-020-redelivered-criterion-result-replaces-existing-one.md) — the
  replace-not-accumulate behavior for a redelivered criterion result

**Related**

- [SW-016](../specs/SW-016-api-test-verdict-is-gate-scoped.md) — the verdict calculation this
  story's deduplication feeds
- [SW-019](../specs/SW-019-final-delivery-attempt-absorbs-failure.md) — the sibling handling for a
  redelivered _failure_ response, on the exceptional-response path this story does not change
- [STR-011](STR-011-analysis-coordination-and-report-lifecycle.md) — the report
  lifecycle story this redelivery behavior sits alongside
