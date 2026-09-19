# A stale report is timed out and kept, never deleted

<!-- markdownlint-disable MD036 -->

**Title**
A stale report is timed out and kept, never deleted

**Lens**: SW

**Status**: active

**Description**
Housekeeping sweeps reports that never reached a verdict.
A report whose creation instant lies further in the past than the configured cutoff **and** whose
status is still `NOT_STARTED` or `IN_PROGRESS` is transitioned to `TIMED_OUT` in a single bulk
update.
The report itself, its API tests, and its results are **retained** — nothing is removed, and the
job's own logging says so at both ends: it announces the reports it is about to time out, and
reports how many it did.
A report in any other status is left alone, so a sweep never disturbs a settled verdict.

The sweep runs on a configurable schedule and is additionally triggerable on demand through a REST
endpoint, which accepts the request and returns immediately; jobs are dispatched to a virtual-thread
executor rather than run inline.
The cutoff is measured against an injected `Clock`, not the system clock read directly.

**Rationale**
`SYS-011` requires a triggered analysis to reach a terminal state in bounded time, and a
calculation whose coverage response never arrives — a crashed calculator, a dropped Kafka message
— would otherwise leave its report `IN_PROGRESS` forever, and a polling caller
([SW-014](SW-014-in-progress-report-answers-accepted.md)) waiting on a `202` that will
never turn into a `200`.
Timing out rather than deleting is what makes that bounded state _diagnosable_: a caller that
polls a vanished report cannot tell a timeout from a bad id, while `TIMED_OUT` says exactly what
happened, and the retained API tests show how far the calculation got.
Restricting the update to the two non-terminal statuses, rather than to age alone, keeps the sweep
from reopening what
[SW-015](SW-015-report-status-aggregates-with-sticky-terminal.md) already settled.
The injected `Clock` is what makes the cutoff testable at all: a directly read system clock would
leave the boundary verifiable only by waiting for it.

**Verification Description**
A test runs the housekeeper against a fixed `Clock` with reports either side of the cutoff and in
each status, asserting that only those older than the cutoff and in `NOT_STARTED` or `IN_PROGRESS`
become `TIMED_OUT`, that no report is removed, and that reports in terminal statuses are untouched.
`QualityGateReportHousekeeperUnitTest` covers the cutoff and status filtering,
`HousekeepingServiceUnitTest`/`HousekeepingServiceIT` the dispatch of jobs, and
`ReportCoordinatorApiAppTest`'s stale-report case observes a real report reaching `TIMED_OUT`
against a running service.

## Relations

**Related**

- [SYS-011](SYS-011-bounded-time-resolution.md) — the bounded-time guarantee this sweep upholds
- [NF-002](NF-002-unattended-operation.md) — the unattended operation the scheduled sweep
  supports
- [SW-015](SW-015-report-status-aggregates-with-sticky-terminal.md) — the terminal
  status this sweep writes and then respects
- [SW-019](SW-019-final-delivery-attempt-absorbs-failure.md) — the faster path to a
  terminal state that keeps most reports away from this sweep
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the polling contract this
  sweep releases a caller from

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
- **2026-09-18** — The job's debug log announced a deletion it never performed; it now names the
  timeout it actually applies.
  Logging only — the sweep's behavior is unchanged.
