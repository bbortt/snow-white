# The fan-out backlog is measured by the age of its oldest unfinished report, not by how many are open

<!-- markdownlint-disable MD036 -->

**Title**
The fan-out backlog is measured by the age of its oldest unfinished report, not by how many are
open

**Lens**: SW

**Status**: planned

**Description**
`report-coordinator-api` exposes two meters over the state of its calculation fan-out.

`snow.white.report.backlog.age` is a **gauge** reporting the age, in seconds, of the oldest report
still in `NOT_STARTED` or `IN_PROGRESS`.
With no such report, the gauge reads zero.

`snow.white.report.timeouts` is a **counter** of the reports transitioned to `TIMED_OUT`, fed by the
count `QualityGateReportHousekeeper.run` already computes and logs.

The gauge is deliberately **not** a count of open reports.
A plain in-flight count is not exposed.

Neither meter is dimensioned by report id, quality gate, or API identity
([CON-017](CON-017-metric-dimensions-are-bounded.md)).

**Rationale**
A count of open reports conflates the two conditions an operator needs to tell apart.
Twenty open calculations on a busy CI morning and twenty open calculations whose results stopped
arriving produce the same number, and the healthy reading is whichever the deployment happens to
see more often.
Age separates them: a backlog that is merely busy stays young because reports complete and leave,
while a stuck fan-out has an oldest member whose age climbs monotonically.
The threshold is also obvious rather than tuned — the housekeeping cutoff is already the
deployment's own statement of how long is too long, so the alert is "approaching the cutoff" and
needs no second opinion.

The oldest, rather than an average or a percentile, because the question is whether _anything_ is
stuck.
An average over a healthy majority hides one report that will never complete, which is exactly the
case worth paging about.

The two meters are one spec because they are the leading and lagging views of a single condition.
The gauge rises while the fan-out is failing; the counter increments once housekeeping has given
up on it ([SW-018](SW-018-stale-reports-time-out-not-deleted.md)).
An operator seeing the counter move without ever having seen the gauge rise learns something
specific: the sweep is firing faster than the scrape interval, so the cutoff or the interval is
mis-sized.

The counter reuses the housekeeper's existing count rather than observing status transitions
elsewhere, because that method is already the single writer of the transition
and already has the number in hand.

**Verification Description**
An integration test creates reports with known `createdAt` values in `NOT_STARTED` and
`IN_PROGRESS`, and asserts the gauge reports the oldest one's age; a further test asserts it reads
zero once all are terminal.
A test asserts reports in terminal statuses are excluded from the gauge.
A unit test asserts the housekeeper increments the counter by the number of rows it updated,
including zero on a run that sweeps nothing.
A test asserts both meters' tag keys, confirming no per-request identifier is carried.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality this backlog view
  serves
- [NF-002](NF-002-unattended-operation.md) — the unattended operation that needs a stuck fan-out
  to announce itself

**Related**

- [SW-018](SW-018-stale-reports-time-out-not-deleted.md) — the sweep whose count this counter
  reports, and whose cutoff the gauge is read against
- [ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md) — the decision
  that a state gauge is instrumented in code, having no span to derive from
- [CON-017](CON-017-metric-dimensions-are-bounded.md) — the invariant both meters satisfy
- [SW-015](SW-015-report-status-aggregates-with-sticky-terminal.md) — the status aggregation that
  decides when a report leaves the gauge's population
- [ARCH-006](ARCH-006-report-status-persisted-as-stable-code.md) — the persisted status codes the
  gauge's query filters on
- [ARCH-004](ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — the fan-out whose
  incompleteness this measures
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the in-progress contract a caller
  sees while the gauge counts the same report
