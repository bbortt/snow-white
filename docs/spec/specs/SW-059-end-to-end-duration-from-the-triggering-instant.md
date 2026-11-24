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

The start is the `initiatedAt` value the `QualityGateCalculationRequestEvent` already carries.
It is not the start of any span, and not the Kafka record timestamp.

The histogram is recorded once per report, when the report leaves the non-terminal statuses, and is
dimensioned by `snow.white.report.status` — the terminal status it reached.
Non-terminal means `NOT_STARTED` or `IN_PROGRESS`, which is the pair the housekeeper sweeps and the
pair [SW-058](SW-058-fan-out-backlog-measured-by-age.md)'s gauge counts; a report that never
started is as much a timed-out calculation as one that stalled half way.

A report swept to `TIMED_OUT` is recorded too, so the slowest outcomes are present rather than
censored.
This requires the housekeeper to know the ages of the rows it sweeps, which its current bulk
`UPDATE ... WHERE createdAt < cutoff AND status IN (NOT_STARTED, IN_PROGRESS)` does not surface.
The housekeeper therefore reads the `createdAt` values it is about to transition, within the same
transaction, and records one observation per swept report.

**Rationale**
No single span covers this duration, which is why it is the one duration instrumented in code
rather than derived ([ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md)).
The HTTP span that triggers a calculation ends as soon as the report is accepted as `IN_PROGRESS`
([SW-014](SW-014-in-progress-report-answers-accepted.md)), long before any result arrives, and the
spans that follow belong to the fan-out's individual API tests rather than to the whole.

`initiatedAt` is the right clock because it is the only one every participant already agrees on.
It travels on every dispatched record by design, and
[SW-043](SW-043-expiry-judged-against-the-triggering-instant.md) already makes it the instant a
waiver's expiry is judged against — for the same reason that applies here.
The Kafka record timestamp is rejected on the grounds that spec names explicitly: its meaning is a
broker configuration rather than a property of the request.
A consumer-side clock is rejected because the measurement would then vary with pod clock skew
across a duration that spans two services.

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
A test asserts exactly one observation per report, including a report whose results arrive in
several batches.
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
