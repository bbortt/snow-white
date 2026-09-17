# Calculation requests are dispatched only after the report's transaction commits

<!-- markdownlint-disable MD036 -->

**Title**
Calculation requests are dispatched only after the report's transaction commits

**Lens**: ARCH

**Status**: active

**Description**
Initializing a calculation persists the report and its API tests inside a transaction, and
dispatches the Kafka calculation requests **outside** it.
When the call runs inside an active, synchronization-capable transaction, the dispatch is
registered as an after-commit callback and fires only once that transaction has committed.
When there is no such transaction — the component invoked directly, as in a unit test — the
dispatch happens inline, so the behaviour degrades to the obvious one rather than being silently
skipped.

**Rationale**
Kafka is not part of the database transaction, so a record sent mid-transaction is already
consumable while the report that explains it may still roll back.
The failure this prevents is concrete and asymmetric: the calculator would answer for a
calculation id whose report does not exist, and the response handler would log an unknown
calculation and drop it
([SW-019](SW-019-final-delivery-attempt-absorbs-failure.md)) — the work is done and the
result is discarded, with nothing left to diagnose.
Deferring the send until after commit trades a send that may be lost after a durable write
(recoverable: the report exists and times out with a diagnosable status) for a send that can never
precede one (unrecoverable: an orphaned calculation).
The inline fallback keeps the component usable outside a transactional caller without a second
code path to maintain.

**Verification Description**
A test initializes a calculation within an active transaction and asserts that nothing is
dispatched before commit and that the records appear after it; and invokes the same operation with
no active transaction and asserts the dispatch happens inline.
`ReportServiceUnitTest` covers both branches, and `TransactionalUnitTest` guards the transactional
boundaries this relies on.

## Relations

**Related**

- [ARCH-004](ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — the dispatch
  whose timing this governs
- [SW-013](SW-013-calculation-trigger-is-all-or-nothing.md) — the trigger contract that
  returns only once the report is durable
- [SW-019](SW-019-final-delivery-attempt-absorbs-failure.md) — the unknown-report path
  this decision keeps calculations out of
- [CON-002](CON-002-tolerate-dependency-outages.md) — the no-data-loss property this ordering
  contributes to

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
