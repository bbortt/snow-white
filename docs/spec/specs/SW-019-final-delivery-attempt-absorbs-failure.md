# A coverage response that keeps failing is folded into the report on its final delivery attempt

<!-- markdownlint-disable MD036 -->

**Title**
A coverage response that keeps failing is folded into the report on its final delivery attempt

**Lens**: SW

**Status**: active

**Description**
Coverage responses arrive on Kafka, keyed by calculation id, and are folded into the matching
report.
Three failure modes are handled differently:

- **The report is unknown** — a response whose calculation id matches no report is logged and
  acknowledged.
  There is nothing to retry into.
- **The response names an API the report does not contain** — logged and acknowledged, not
  retried.
  Redelivery cannot change the mismatch, so retrying would only block the partition behind a
  message that can never succeed.
- **Processing fails for any other reason** — the exception is rethrown so Kafka redelivers the
  record under the configured retry count and backoff.
  On the **final** delivery attempt, and only then, the failure is first recorded on the report:
  the affected API test is marked `FINISHED_EXCEPTIONALLY` with the exception message as its stack
  trace, and the report's status is re-derived — after which the exception is still rethrown.

A response carrying an error message from the calculator itself takes the same recording path
directly, without any retry.

**Rationale**
Retrying is right for a transient fault and useless for a permanent one, and the last delivery
attempt is the only point where the two become distinguishable — so that is where the outcome is
made durable.
Without this, a permanently failing response would exhaust its retries silently and leave the
report `IN_PROGRESS` until the housekeeping sweep
([SW-018](SW-018-stale-reports-time-out-not-deleted.md)) expired it minutes later, with
`TIMED_OUT` telling the caller only that nothing arrived — losing the exception message that says
_why_.
Recording before the rethrow, rather than instead of it, keeps the error-handling contract with
Kafka intact: the broker still sees a failed delivery and can route the record onward.
Acknowledging the two unmatchable cases instead of retrying them is head-of-line protection — a
poison record must not stall every later response on the same partition.

**Verification Description**
A test delivers a coverage response for an unknown calculation id, one naming an API absent from
the report, and one whose processing throws — the last both on a non-final and on a final delivery
attempt — asserting that the first two are acknowledged without a report change, that the non-final
throwing case rethrows without touching the report, and that the final attempt marks the API test
`FINISHED_EXCEPTIONALLY` with the exception message before rethrowing.
`OpenApiResultListenerUnitTest` covers the delivery-attempt branching and
`OpenApiResultListenerIT` the redelivery behaviour against a broker.

## Relations

**Related**

- [SYS-011](SYS-011-bounded-time-resolution.md) — the bounded-time guarantee this shortens the
  path to
- [CON-002](CON-002-tolerate-dependency-outages.md) — the transient-fault tolerance the retry
  before this point provides
- [NF-005](NF-005-clear-failure-feedback.md) — the failure feedback the preserved exception
  message carries
- [SW-015](SW-015-report-status-aggregates-with-sticky-terminal.md) — the status
  re-derivation this triggers
- [SW-018](SW-018-stale-reports-time-out-not-deleted.md) — the slower fallback this
  pre-empts
- [ARCH-004](ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — the fan-out
  whose responses this absorbs

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
- **2026-09-18** — Reviewed after the retrace raised the last-attempt absorption as a finding, and
  confirmed as intended: three failed deliveries mean something is wrong rather than briefly
  absent, so resolving the report as `FINISHED_EXCEPTIONALLY` with the exception message is the
  right outcome — leaving it to the housekeeping timeout would discard the reason.
  No change.
