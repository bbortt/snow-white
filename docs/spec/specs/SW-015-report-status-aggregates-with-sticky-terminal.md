# Report status is the worst of its API tests by fixed precedence, and a terminal status is never recomputed

<!-- markdownlint-disable MD036 -->

**Title**
Report status is the worst of its API tests by fixed precedence, and a terminal status is never
recomputed

**Lens**: SW

**Status**: active

**Description**
A report has no status of its own to set; it is derived from its API tests every time one of them
changes, by the first matching rule in this order:

1. The report's current status is already `FAILED`, `FINISHED_EXCEPTIONALLY`, or `TIMED_OUT` —
   it is returned unchanged, and no rule below is consulted.
2. The report has no API tests — `NOT_STARTED`.
3. Any API test is `IN_PROGRESS` — `IN_PROGRESS`.
4. Any API test is `FAILED` — `FAILED`.
5. Any API test is `FINISHED_EXCEPTIONALLY` — `FINISHED_EXCEPTIONALLY`.
6. Otherwise — `PASSED`.

A report and an API test are both born `IN_PROGRESS`, so rule 1 never blocks the first derivation.
The precedence is deliberate and not a severity ordering: `IN_PROGRESS` outranks every failure, so
a report with one failed and one still-running API test reads as running, not as failed.

**Rationale**
Sticky terminal statuses are what make the verdict stable: coverage responses arrive
asynchronously and out of order, and a late response must not revive a report that housekeeping
already timed out or that a failure already settled — a caller that read `FAILED` and failed its
build must not find `PASSED` on a later read.
Ranking `IN_PROGRESS` above the failure statuses keeps the report honest while it is incomplete:
reporting `FAILED` the moment the first API test fails would let a caller act on a verdict that
the remaining APIs have not yet contributed to, and the failure is not lost — it is re-derived
once the last test lands.

**Verification Description**
A test drives the calculator with a report holding no API tests, with mixed combinations covering
each precedence rung (one in-progress alongside one failed, one failed alongside one finished
exceptionally, all passed), and with a report already in each of the three terminal statuses,
asserting the exact status named by the first matching rule and that the terminal cases are
returned untouched.
`QualityGateStatusCalculatorUnitTest` covers every rung including the parameterised terminal case.

## Relations

**Related**

- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the retrieval contract that
  branches on this status
- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — how the per-API-test statuses
  this rule aggregates are decided
- [SW-018](SW-018-stale-reports-time-out-not-deleted.md) — the timeout that writes a
  terminal status rule 1 then protects
- [SW-019](SW-019-final-delivery-attempt-absorbs-failure.md) — the other writer of a
  terminal status
- [ARCH-006](ARCH-006-report-status-persisted-as-stable-code.md) — how the derived
  status is stored

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
