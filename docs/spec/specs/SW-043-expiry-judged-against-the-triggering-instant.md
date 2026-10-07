# A waiver's expiry is judged against the instant the calculation was triggered, carried on the request

<!-- markdownlint-disable MD036 -->

**Title**
A waiver's expiry is judged against the instant the calculation was triggered, carried on the request

**Lens**: SW

**Status**: planned

**Description**
A waiver suppresses nothing once it has expired.
Expiry is decided by comparing the waiver's `expires` against **the instant the calculation was
triggered** — the report's own `createdAt`, the value already served as `initiatedAt` — and never
against the clock of whatever component happens to be running.

That instant is pinned at trigger time and carried explicitly on
`QualityGateCalculationRequestEvent`, alongside the waivers it governs.
The coverage stream reads it from the event.
It does not read the Kafka record timestamp, the system clock of the calculating pod, or the time the
report is later read.

An expired waiver is not silently dropped.
The criterion judges its target normally — the finding stays `COVERED` or `UNCOVERED` on the evidence
— and the result's additional information names the waiver and the date it expired, so a build that
turns red says why.
An expired waiver still has to match a finding (`CON-011`): expiry is a verdict about a waiver
that was placed, not an excuse for one that was not.

Re-deriving a report from the same specification, telemetry and request therefore reproduces its
waiver verdicts however much later it is run.

**Rationale**
`CON-001` promises identical results from identical inputs, and an expiry is the first thing in this
system that could have broken that promise.
Compared against wall-clock time, a waiver's effect changes at midnight on its expiry date with no
input having changed — the same request answering differently, which is precisely the failure
`CON-001` exists to exclude.

The fix is not to weaken `CON-001` but to name the input that was always there.
The calculation already consumes an instant explicitly: the telemetry window is bounded by the
timestamp handed to the pipeline, so "what time is it" has never been ambient inside a calculation —
it has been a parameter.
`CON-001` gains a clarifying amendment saying so, the same move `SW-039` made for the threshold:
there the bar was already pinned on the report and merely unpublished, and naming it as an input
turned an apparently non-deterministic verdict into a reproducible one.

Pinning the instant at trigger time rather than letting the stream stamp it is what makes the
pinning real.
The report's `createdAt` is `NOT NULL` and non-updatable and is already published on every read, so
the value a consumer can see is the value expiry was judged against — an auditable pair rather than
two numbers that happen to be close.
It also survives the retry: a record that is reprocessed, or a consumer that lags a day behind,
judges the waiver as of the trigger rather than as of the catch-up.

Declining the Kafka record timestamp is a deliberate rejection of a value that looks free.
`TracingProcessor` already hands it down the pipeline, but its meaning is a broker setting —
`message.timestamp.type` chooses between the producer's clock and the broker's — so an expiry judged
against it would be an expiry whose semantics a deployment could change.
That is ambient state wearing an input's clothes.
The record timestamp stays what it is used for today, and the pinned instant is carried as its own
field.

Enforcing expiry in the CLI before submission is the other rejected alternative, and the issue's own
lean.
It is cheap and it fails in one direction that matters: it moves the only enforcement point outside
the system that publishes the verdict.
A waiver the submitter checks is a waiver the submitter can skip — by an older CLI, a direct REST
call, or any other client — and the service would accept it and publish a pass with no record that
anything was overridden.
Client-side expiry remains useful as fast feedback, but it cannot be the enforcement.

Reporting an expired waiver rather than ignoring it closes the loop the expiry exists for.
The point of a date is to force the question back into view; a criterion that simply starts failing
with no mention of the waiver makes the team debug their telemetry instead of reading their waiver
file.

**Verification Description**
A test calculates a report with a waiver whose `expires` is after the pinned instant and asserts the
target is waived.
A test calculates with `expires` before the pinned instant and asserts the target is judged normally
and the additional information names the waiver and its expiry date.
A test asserts a waiver expiring exactly on the trigger date is still in force on that date.
A test backdates a report's pinned instant, replays the same calculation request, and asserts the
waiver verdict matches the original rather than today's — including for a waiver that has since
expired.
A test asserts the stream reads the instant from the event, not from the record timestamp: a record
produced with a deliberately divergent timestamp yields the event's verdict.
A test asserts no calculator or waiver code path calls a system clock.

## Relations

**Related**

- [CON-001](CON-001-deterministic-analysis-results.md) — amended: the calculation instant is an
  input alongside the specification and the telemetry
- [SW-040](SW-040-waiver-enters-only-on-the-calculation-request.md) — the `expires` field
  this judges
- [CON-011](CON-011-waiver-matching-no-finding-fails-the-calculation.md) — the separate
  match obligation an expired waiver still has
- [ARCH-004](ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — amended: the pinned instant
  travels on every dispatched record
- [SW-039](SW-039-report-publishes-its-pinned-threshold.md) — the precedent: an input pinned on the
  report, published, and thereby made reproducible
- [SYS-011](SYS-011-bounded-time-resolution.md) — the other time input the calculation already
  consumes explicitly
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the read that already publishes the
  pinned instant as `initiatedAt`
- [ARCH-013](ARCH-013-test-identity-travels-as-baggage.md) — the sibling decision to carry a value
  explicitly rather than infer it in transit
