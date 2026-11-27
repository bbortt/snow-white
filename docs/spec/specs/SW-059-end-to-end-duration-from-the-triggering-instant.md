# End-to-end calculation duration is measured from the triggering instant the request event carries, not from a span or a record timestamp

<!-- markdownlint-disable MD036 -->

**Title**
End-to-end calculation duration is measured from the triggering instant the request event carries,
not from a span or a record timestamp

**Lens**: SW

**Status**: planned

**Description**
`report-coordinator-api` records `snow.white.report.duration`, a histogram in seconds, measuring the
time from a calculation's triggering instant to the moment its last outstanding result is linked
and the report reaches a terminal status.

The start is the `initiatedAt` value the `QualityGateCalculationRequestEvent` already carries, which
is the report row's own `createdAt`.
`ReportService.dispatchCalculationRequest` passes `report.getCreatedAt()` as `initiatedAt`, and its
javadoc states why: every record of one calculation then names the same instant, and it is the same
instant the report serves back.
There is one clock read, in one service, and the event and the row carry its value rather than two
readings of it.
It is not the start of any span, and not the Kafka record timestamp.

The histogram is recorded once per report, and is dimensioned by `snow.white.report.status` — the
status the report held when its observation was taken.
The observation is taken when the last outstanding API test result is linked, or when the
housekeeper sweeps the report to `TIMED_OUT`; the housekeeper's pair is `NOT_STARTED` and
`IN_PROGRESS`, the pair [SW-058](SW-058-fan-out-backlog-measured-by-age.md)'s gauge also counts,
because a report that never started is as much a timed-out calculation as one that stalled half
way.

The edge is that linking rather than "the report's status became terminal", because `PASSED` is
not sticky: rule 1 of
[SW-015](SW-015-report-status-aggregates-with-sticky-terminal.md) protects `FAILED`,
`FINISHED_EXCEPTIONALLY`, and `TIMED_OUT` only.
Every API test row exists from dispatch, born `IN_PROGRESS`, and that same precedence ranks
`IN_PROGRESS` above every failure, so on the normal path the two edges coincide — the report
cannot read `PASSED` while a result is outstanding.
At most one observation exists per report, taken at whichever edge comes first and never again.
Both orders occur: a re-delivered or corrected result can flip an already-`PASSED` report to
`FAILED` after the linking edge, and a late coverage response can be linked after the housekeeper
has already swept the report to `TIMED_OUT`.
Rule 1 leaves the swept status untouched in the second case, and in both the report keeps the
duration and the status its one observation carried.

A report swept to `TIMED_OUT` is recorded too, so the slowest outcomes are present rather than
censored.
This requires the housekeeper to know the ages of the rows it sweeps, which its current bulk
`UPDATE ... WHERE createdAt < cutoff AND status IN (NOT_STARTED, IN_PROGRESS)` does not surface.
The sweep therefore records one observation for exactly the rows it transitioned, carrying their
`createdAt` values — the same instants the dispatched records carried.
The ages it records and the rows it transitions must come from the same decision: either the
statement that transitions them also returns their `createdAt`, or a locked read in the same
transaction holds the candidates until the update commits.
An unlocked read of the rows the sweep is about to transition is not sufficient, because a report
that reaches a terminal status between that read and the update is excluded by the update's status
filter, and would be recorded as `TIMED_OUT` carrying the duration of a calculation that in fact
completed.
The observations are published once the sweep's transaction commits, so a rolled-back sweep leaves
its rows to the next one rather than observing them twice.

**Rationale**
No single span covers this duration, which is why it is the one duration instrumented in code
rather than derived ([ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md)).
The HTTP span that triggers a calculation ends as soon as the report is accepted as `IN_PROGRESS`
([SW-014](SW-014-in-progress-report-answers-accepted.md)), long before any result arrives, and the
spans that follow belong to the fan-out's individual API tests rather than to the whole.

`initiatedAt` is the right value because it is the one instant every participant already agrees on.
It travels on every dispatched record by design, and
[SW-043](SW-043-expiry-judged-against-the-triggering-instant.md) already makes it the instant a
waiver's expiry is judged against — for the same reason that applies here.
The Kafka record timestamp is rejected on the grounds that spec names explicitly: its meaning is a
broker configuration rather than a property of the request.
A consumer-side clock is rejected because it is the one option that would cross two of them:
`report-coordinator-api` both writes `createdAt` and links the last result, so taking either end
from a fan-out consumer trades a measurement on one clock for a difference between two, and makes
the number vary with pod clock skew.

Dimensioning by terminal status is what makes the histogram readable.
A `PASSED` calculation and one swept to `TIMED_OUT` describe different populations, and mixing them
makes the upper percentiles a function of how many calculations failed rather than of how long work
takes.
Including the timed-out reports rather than dropping them is the same argument that puts failures
in the derived histograms: excluding the slowest cases makes the metric most optimistic exactly
when the system is worst.

Subtracting the three phase durations
([SW-055](SW-055-coverage-phases-are-the-span-boundary.md)) from this one leaves the time
spent in transit and scheduling, which is the third owner neither metric isolates alone.

**Verification Description**
An integration test triggers a calculation with a known `initiatedAt`, completes its results, and
asserts the histogram records a duration measured from that instant and not from the dispatch or
the record timestamp.
A test asserts a report swept to `TIMED_OUT` is recorded with that terminal status.
A test asserts the sweep records observations for exactly the rows it transitioned, and that a sweep
whose transaction rolls back records none.
A test asserts exactly one observation per report, including a report whose results arrive in
several batches, a report observed as `PASSED` that a later result flips to `FAILED`, and a report
whose last result is linked after the housekeeper swept it to `TIMED_OUT`.
A test asserts the histogram's tag keys are exactly the terminal status.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality this end-to-end
  duration serves
- [NF-004](NF-004-cicd-suitable-performance.md) — the performance quality this is the measurement
  of

**Related**

- [SW-043](SW-043-expiry-judged-against-the-triggering-instant.md) — the existing decision to
  judge against the triggering instant, for the same reason
- [ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md) — the division
  of labour this is the named exception to
- [SW-055](SW-055-coverage-phases-are-the-span-boundary.md) — the phase durations
  subtracted from this one to isolate transit time
- [SW-058](SW-058-fan-out-backlog-measured-by-age.md) — the backlog view of the same
  fan-out, measured while it is still open
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the early-returning trigger that
  leaves no span covering this span of time
- [SW-015](SW-015-report-status-aggregates-with-sticky-terminal.md) — the aggregation that decides
  when the report is terminal and the observation is taken
- [ARCH-004](ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — the fan-out whose
  completion this measures
- [ARCH-005](ARCH-005-dispatch-after-transaction-commit.md) — the dispatch boundary that sits
  between the triggering instant and the first record
