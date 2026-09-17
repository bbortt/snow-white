# The JUnit export skips excluded criteria and fails anything short of full coverage

<!-- markdownlint-disable MD036 -->

**Title**
The JUnit export skips excluded criteria and fails anything short of full coverage

**Lens**: SW

**Status**: active

**Description**
A completed report is also served as a JUnit XML document, following the Common JUnit XML Format,
under a fixed three-level mapping:

- The **report** becomes the `testsuites` root, named after its quality gate, timestamped with the
  report's creation instant, and carrying the calculation id as a property.
- Each **API test** becomes a `testsuite`, named `service: api version [type]`, carrying service
  name, API name, and API version as properties.
- Each **criterion result** becomes a `testcase`, named with the criterion's human-readable label,
  described by the criterion's description, and timed with that criterion's own duration.

The verdict per test case is not the report's verdict:

- A result **excluded** by the gate is emitted as `skipped`, with a message naming the gate — it
  is present in the document, not omitted.
- An **included** result whose coverage is below **1** (full coverage) is emitted as a `failure`
  of type `AssertionError`, carrying the result's additional information as its message.
  This bar is stricter than the gate's `minCoveragePercentage`, so a report that
  [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) scores as `PASSED` can — and
  routinely does — contain JUnit failures.
- Everything else is a plain passing test case.

Counts are aggregated bottom-up (`assertions` being tests minus skipped, durations summed), and
suites and cases are ordered by name so the same report always serialises to the same document.

**Rationale**
A build server reads this document, and its only vocabulary is pass, fail, and skip.
`skipped` is the honest rendering of a criterion the gate deliberately excluded: dropping it would
make the gate's scope invisible in the one artifact most consumers actually look at, and marking
it passed would claim a check that never ran.
The full-coverage bar is a deliberate divergence: the gate threshold answers "is this good enough
to ship", while the JUnit document is a diagnostic surface, and a developer reading a failing test
case wants to be shown every criterion with a gap — not only the ones that dragged the gate down.
Name-ordering makes the document diffable across runs, which a timestamp-ordered or
hash-ordered serialisation would not be.

**Verification Description**
A test transforms reports into JUnit XML and compares against fixed expected documents: one with
fully covered results, one with partially covered results, one excluded by the gate, and one
mixing fully and partially covered results across two API tests — asserting `skipped` for the
excluded case, `failure` for every included result below full coverage, the aggregated counts and
durations, and name-stable ordering.
`JUnitReporterUnitTest` compares against committed expected XML for each case, and
`ReportCoordinatorApiAppTest` retrieves the document from a running service.

## Relations

**Related**

- [SYS-009](SYS-009-machine-consumable-gate-result.md) — the machine-consumable result this
  document is
- [SYS-012](SYS-012-result-consumption.md) — the consumption capability it serves
- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — the gate-scoped verdict this
  export deliberately diverges from
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the retrieval contract that
  withholds this document while the report is running
- [CON-001](CON-001-deterministic-analysis-results.md) — the determinism the fixed ordering
  extends to the serialised form

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
