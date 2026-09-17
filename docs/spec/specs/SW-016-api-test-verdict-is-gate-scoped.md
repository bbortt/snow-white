# Only the gate's own criteria decide an API test, and the threshold is applied twice

<!-- markdownlint-disable MD036 -->

**Title**
Only the gate's own criteria decide an API test, and the threshold is applied twice

**Lens**: SW

**Status**: active

**Description**
A coverage response carries a result for every criterion the calculator produced, not only the
ones the report's quality gate selected.
When those results are attached to an API test:

- Every result is **persisted**, and each is flagged included or not according to whether the
  gate's criteria set names it.
  A criterion outside the gate is kept, visibly excluded — never discarded.
- Only **included** results decide the verdict.
  A result passes when its coverage reaches the gate's `minCoveragePercentage` (compared as a
  ratio); the API test passes when the **share of included results that passed**, expressed as a
  percentage, reaches that same `minCoveragePercentage`.
  Otherwise the API test fails.
- An API test whose included set is **empty** passes.
- A response carrying no results at all leaves the API test untouched — neither its results nor
  its status change.

**Rationale**
Keeping excluded results rather than dropping them is what lets a report explain itself: a reader
can see that a criterion was measured and deliberately not counted, which is also what
[SW-017](SW-017-junit-export-skips-excluded-fails-partial.md) renders as a skipped test
case rather than a missing one.
Reusing one configured number as both the per-criterion bar and the bar on the share of criteria
clearing it is the decision worth pinning: a gate at 80% means both "a criterion must be 80%
covered" and "80% of the gate's criteria must clear that", so a single knob tightens the gate at
two altitudes at once, and neither can be tuned without the other.
The empty-set pass is vacuous truth made explicit: a gate that selected no criterion this API
produced results for has nothing to object to, so the API test must not fail on absence of
evidence.

**Verification Description**
A test attaches a mixed result set to an API test under a gate selecting a subset of the criteria
and asserts: excluded results are persisted with the not-included flag, included results alone
drive the outcome, a set where the passing share meets the threshold yields `PASSED` and one below
it yields `FAILED`, an API test with no included results yields `PASSED`, and an empty result set
leaves the API test unchanged.
`ApiTestResultLinkerUnitTest` covers these cases.

## Relations

**Related**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the criteria-based evaluation whose results
  this rule scores
- [SYS-008](SYS-008-quality-gate-definitions.md) — the gate whose criteria set and threshold are
  read here
- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the bounded coverage
  ratio this comparison relies on
- [SW-015](SW-015-report-status-aggregates-with-sticky-terminal.md) — how these
  per-API-test verdicts roll up
- [SW-017](SW-017-junit-export-skips-excluded-fails-partial.md) — the export that
  renders the same results under a stricter bar

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
