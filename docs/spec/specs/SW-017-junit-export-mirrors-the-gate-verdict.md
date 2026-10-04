# The JUnit export skips excluded criteria and judges the rest by the gate's own threshold

<!-- markdownlint-disable MD036 -->

**Title**
The JUnit export skips excluded criteria and judges the rest by the gate's own threshold

**Lens**: SW

**Status**: active

**Description**
A completed report is also served as a JUnit XML document, following the Common JUnit XML Format,
under a fixed three-level mapping:

- The **report** becomes the `testsuites` root, named after its quality gate, timestamped with the
  report's creation instant, and carrying the calculation id and the gate's
  `minCoveragePercentage` as properties.
- Each **API test** becomes a `testsuite`, named `service: api version [type]`, carrying service
  name, API name, and API version as properties.
- Each **criterion result** becomes a `testcase`, named with the criterion's human-readable label,
  described by the criterion's description, and timed with that criterion's own duration.

The bar applied per test case is the gate's `minCoveragePercentage` — the same number
[SW-016](SW-016-api-test-verdict-is-gate-scoped.md) scores the API test with, so the document
never contradicts the verdict it accompanies:

- A result **excluded** by the gate is emitted as `skipped`, with a message naming the gate — it
  is present in the document, not omitted.
- An **included** result whose coverage is **below the threshold** is emitted as a `failure` of
  type `AssertionError`, carrying the result's additional information as its message.
- An **included** result that **reaches the threshold but not full coverage** is a passing test
  case carrying a `system-out` comment: the coverage reached, the threshold it cleared, the gate
  that set it, and the result's additional information when it has any.
- Everything else — an included result at full coverage, holding no waived target — is a plain
  passing test case.

An included result that holds waived targets adds to whichever of those shapes it takes: its
`system-out` names each waived target with its reason and expiry, and the case carries
`judgedTargetCount` and `waivedTargetCount` properties beside a `waivedTargets` property naming
them, so this document states the same two numbers the JSON read does (`SW-044`).
A result reaching full coverage only because targets were waived is therefore never a plain passing
test case — it is a passing test case that says why — which is what `CON-012` requires of every
published shape.
The `testsuites` root carries the document's waived total beside the threshold.

The threshold is read from the report, which pins the gate's `minCoveragePercentage` at creation
time; a later edit to the gate therefore cannot change how an already calculated report exports.

Counts are aggregated bottom-up (`assertions` being tests minus skipped, durations summed), and
suites and cases are ordered by name so the same report always serialises to the same document.

**Rationale**
A build server reads this document, and its only vocabulary is pass, fail, and skip.
The gate is the master: a criterion the gate is willing to ship must not be red in the artifact a
build server blocks on, because a red test case that nothing can be done about trains its readers
to ignore all of them.
An earlier revision failed anything below full coverage, which made a `PASSED` report routinely
export failures — the two surfaces disagreed by design, and the XML was the one consumers acted
on.
The diagnostic need that divergence served is real, though, and `system-out` is where the format
keeps it: a criterion between the threshold and full coverage still says how big its gap is, on a
test case a build server counts as green.
`skipped` is the honest rendering of a criterion the gate deliberately excluded: dropping it would
make the gate's scope invisible in the one artifact most consumers actually look at, and marking
it passed would claim a check that never ran.
Publishing the threshold as a property on the root states the bar the whole document was judged
against, so a reader never has to infer it from which cases failed.
Reading that threshold from the report rather than from the live gate keeps an export reproducible:
a document regenerated after someone tightened the gate must still be the document that report's
verdict was reached under.
Name-ordering makes the document diffable across runs, which a timestamp-ordered or
hash-ordered serialisation would not be.

**Verification Description**
A test transforms reports into JUnit XML and compares against fixed expected documents: one with
fully covered results, one with results below the gate's threshold, one excluded by the gate, one
mixing fully and partially covered results across two API tests, and one under a gate below 100%
holding a result in the band between that threshold and full coverage — asserting `skipped` for
the excluded case, `failure` for every included result under the threshold, a passing case with a
`system-out` comment for every included result in the band, the threshold property on the root,
and the aggregated counts, durations and name-stable ordering.
Two further expected documents cover waived targets — one result reaching full coverage only by
waiver, one holding both a coverage gap and a waiver — asserting that each is a passing test case
whose `system-out` names every waived target with its reason and expiry, that each carries
`judgedTargetCount`, `waivedTargetCount` and `waivedTargets` properties agreeing with the same
report's JSON read, that the waived total appears on the root beside the threshold, and that
`skipped` is still emitted for gate exclusion alone.
`JUnitReporterUnitTest` compares against committed expected XML for each case, and
`ReportCoordinatorApiAppTest` retrieves the document from a running service.

## Relations

**Related**

- [SYS-009](SYS-009-machine-consumable-gate-result.md) — the machine-consumable result this
  document is
- [SYS-012](SYS-012-result-consumption.md) — the consumption capability it serves
- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — the gate-scoped verdict this
  export now agrees with, and whose threshold it reuses
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the retrieval contract that
  withholds this document while the report is running
- [CON-001](CON-001-deterministic-analysis-results.md) — the determinism the fixed ordering
  extends to the serialised form
- [SW-039](SW-039-report-publishes-its-pinned-threshold.md) — the JSON reads that now publish the
  same pinned number this document carries as a suite property
- [CON-012](CON-012-criterion-emptied-by-waivers-is-never-a-coverage-pass.md) — the invariant this
  export has to carry in its own vocabulary
- [SW-044](SW-044-report-publishes-judged-and-waived-target-counts.md) — the counts this document
  reports as properties, agreeing with the JSON reads

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
- **2026-09-18** — The export's bar changed from full coverage to the gate's own
  `minCoveragePercentage`, resolving the deliberate divergence this spec previously described: a
  result between the threshold and full coverage is now a pass carrying a `system-out` comment
  rather than a failure.
  The threshold is pinned onto the report at creation time and published as a property on the
  `testsuites` root.
- **2026-10-03** — Records `SW-039` (`STR-022`) as the JSON counterpart of the suite property this
  export already publishes.
  Nothing about the document changes.
  Until now this artifact was the only surface that stated the bar, which is why it could say
  "failure" while the JSON reads could not — a consumer can now reproduce this `<failure>` set from
  a report read, and the two surfaces answer from one source rather than two.
- **2026-10-04** — Reports waived targets, for `CON-012` and `SW-044` (`STR-023`).
  `skipped` keeps meaning gate exclusion and nothing else, which is the decision that shapes the rest:
  a waiver is not an exclusion, and overloading the one element JUnit has for "not run" would make the
  two indistinguishable in the format most likely to be read by a machine that knows nothing about
  Snow-White.
  So a criterion with waived targets takes the shape this spec already uses for a verdict the format
  lacks — a passing `testcase` whose `system-out` names each waived target with its reason and expiry
  — plus `judgedTargetCount`, `waivedTargetCount` and `waivedTargets` properties for a reader that
  parses rather than reads, the first two being the pair `SW-044` requires every surface to state,
  and a waived total on the `testsuites` root beside the threshold.
  The bar and the mapping are unchanged; what the export gains is the ability to distinguish a pass
  that rests on waivers from one that rests on coverage, which `CON-012` requires of every published
  shape and this document could not previously express.
