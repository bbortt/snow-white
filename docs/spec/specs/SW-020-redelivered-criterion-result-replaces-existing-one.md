# A redelivered criterion result replaces the existing one for that (criterion, API test) pair

<!-- markdownlint-disable MD036 -->

**Title**
A redelivered criterion result replaces the existing one for that (criterion, API test) pair

**Lens**: SW

**Status**: active

**Description**
`ApiTestResultLinker.addApiTestResultsToApiTest` folds the `ApiTestResult`s carried by an
`OpenApiCoverageResponseEvent` into the target `ApiTest`.
An `ApiTestResult`'s identity is the pair `(apiTestCriteria, apiTest)`.
When the incoming set contains
a result for a criterion the `ApiTest` already holds a result for, the new result **replaces** the
existing one; it does not sit beside it.
A criterion the incoming set does not mention is left
untouched.
After the fold, the `ApiTest`'s result set contains at most one `ApiTestResult` per
`apiTestCriteria` — never two entries mapping to the same `(apiTestCriteria, apiTest)` pair.
This
holds equally whether the incoming results are the first ever received for that `ApiTest` or a
later, redelivered set for criteria already scored.

Replacement is wholesale, and reaches the result's children.
A replaced `ApiTestResult` takes its findings (`ARCH-012`) with it: after the fold, no finding
belonging to a superseded delivery of that criterion remains, and the surviving result's findings
are exactly those the incoming result carried.
A partial merge — keeping a finding whose target the new delivery no longer judges — would leave
the enumerated target space of `SW-030` describing two different calculations at once and break
`CON-009` for that result.

**Rationale**
`OpenApiCoverageResponseEvent` delivery is at-least-once: `openapi-coverage-stream`'s Kafka
Streams consumer can be fenced out of its group mid-calculation (confirmed via a production
`TaskMigratedException`/`CommitFailedException`) when telemetry-fetch latency exceeds
`max.poll.interval.ms`, and Kafka then reprocesses the same calculation request, producing a
second event for the same `ApiTest` with results for the same criteria as the first.
Before this
decision, `ApiTestResult` carried no `equals()`/`hashCode()` for its identity pair, so a blind
`addAll` into the `Set<ApiTestResult>` treated the redelivered results as new entries; Hibernate
then found two managed representations of the same database row on flush and failed the merge,
crashing the listener and leaving the report stuck.
Replacing by identity is the only option of the
three considered (replace, skip, reject) that both survives redelivery and stays correct: skip-
and-keep-first would let a result computed against a transient telemetry gap outlive a later,
complete one, and rejecting the whole event would throw away criteria in the same event that were
not previously scored.
Replace-by-identity treats the latest measurement for a criterion as the
authoritative one, consistent with `openapi-coverage-stream` recomputing coverage fresh on every
request rather than incrementally.

**Verification Description**
An integration test delivers two `OpenApiCoverageResponseEvent`s for the same `ApiTest`, the
second overlapping the first in one criterion and introducing one new criterion, and asserts:
processing the second event does not throw; the persisted result set contains exactly one entry
for the overlapping criterion, with the second event's coverage value; the first event's
non-overlapping criterion is still present and unchanged; and the new criterion from the second
event is present.
A unit test on `ApiTestResult` asserts two instances built with the same
`apiTestCriteria` and `apiTest` are equal and hash identically, and that changing either field
breaks equality.
The same integration test asserts, for the overlapping criterion, that the persisted findings are
exactly the second event's — none of the first event's findings survives, including one whose
target the second delivery no longer judges.

## Relations

**Related**

- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — the pass-rate verdict this
  deduplicated result set feeds
- [SW-019](SW-019-final-delivery-attempt-absorbs-failure.md) — the sibling redelivery
  handling on the exceptional-response path, which this spec does not change
- [ARCH-004](ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — the fan-out shape
  that makes a redelivered event target the same `ApiTest`
- [SYS-009](SYS-009-machine-consumable-gate-result.md) — the machine-consumable result this
  correctness guarantee protects
- [CON-002](CON-002-tolerate-dependency-outages.md) — the outage-tolerance invariant this spec
  is the concrete instance of for `report-coordinator-api`'s own inbound response processing,
  mirroring `SW-008`'s role for `openapi-coverage-stream`
- [ARCH-012](ARCH-012-findings-on-the-event-coverage-as-cache.md) — the child collection the
  replace guarantee now has to reach
- [CON-009](CON-009-coverage-agrees-with-findings.md) — the invariant a partial child merge would
  break

## Changes

- **2026-09-19** — Set active: implementation of STR-012 began.
- **2026-09-22** — Extended replace-by-identity to the result's findings: a superseded delivery
  leaves none behind. `STR-017` gives `ApiTestResult` a child collection (`ARCH-012`), and the
  guarantee as written only spoke of the result row; a surviving orphan finding would contradict
  `SW-030`'s complete-enumeration reading and `CON-009`'s cache invariant.
