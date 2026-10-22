# Only the gate's own criteria decide an API test, and every one of them must clear the bar

<!-- markdownlint-disable MD036 -->

**Title**
Only the gate's own criteria decide an API test, and every one of them must clear the bar

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
  A result passes when its coverage reaches the gate's `minCoveragePercentage`, compared as a
  ratio and compared **exactly** — nothing is rounded on either side, so a result short of the
  bar by any margin misses it.
  The API test passes when **every** included result passed, and fails as soon as one did not,
  however many others cleared the bar.
  The threshold is a floor under each included criterion, not a quota that a majority of them can
  fill on behalf of the rest.
- An API test whose included set is **empty** passes.
- A response carrying no results at all leaves the API test untouched — neither its results nor
  its status change.

The `minCoveragePercentage` applied is the one **pinned onto the report** when the calculation was
triggered, not the one the gate carries at the moment a response arrives.
The gate's criteria set is still read live, because it is not pinned.

**Rationale**
Keeping excluded results rather than dropping them is what lets a report explain itself: a reader
can see that a criterion was measured and deliberately not counted, which is also what
[SW-017](SW-017-junit-export-mirrors-the-gate-verdict.md) renders as a skipped test
case rather than a missing one.
Making the threshold a floor under every included criterion is the decision worth pinning, and it
is a reversal: this rule used to apply the same number twice, once per criterion and once to the
share of criteria clearing it.
That second application read as a designed allowance and was not one.
A bar is bounded to 80–100 and a gate can select at most the fourteen criteria the coverage enum
declares, and within those bounds the share tolerated at most two failing criteria, usually zero,
and at bars of 95 or above never a single one — so what looked like a second altitude was in
practice a flat "one or two criteria may be ignored", available only to gates that happened to
select enough of them.
Worse, a tolerated failure carried no floor of its own: a criterion at 0% coverage, which is a
check that found nothing it was looking for, could be waved through by its neighbours.
Nobody configures a gate meaning that, and `80%` does not say it; a reader told a gate is set to
80% hears the per-criterion sentence, which is the one now in force.
Each criterion standing alone is also what makes a verdict explainable: the gate names the
criteria it cares about, and every name it lists is a condition rather than a vote.
That is what lets [SW-017](SW-017-junit-export-mirrors-the-gate-verdict.md) keep its promise that
the exported document never contradicts the verdict it accompanies — under the share rule a
`PASSED` suite could contain an explicit `failure` test case, which is a document no build server
would honour.
The empty-set pass is vacuous truth made explicit: a gate that selected no criterion this API
produced results for has nothing to object to, so the API test must not fail on absence of
evidence.
Reading the pinned threshold rather than the live one keeps a single calculation coherent: responses
for the same report arrive over minutes, and a gate edited in between would otherwise score the
earlier API tests under one bar and the later ones under another, leaving a report no reader could
explain.

**Verification Description**
A test attaches a mixed result set to an API test under a gate selecting a subset of the criteria
and asserts: excluded results are persisted with the not-included flag, included results alone
drive the outcome, a set where every included result reaches the threshold yields `PASSED` while
one where a single included result misses it yields `FAILED` however many others passed, a result
sitting exactly on the bar passes and one just below it fails, an API test with no included
results yields `PASSED`, and an empty result set leaves the API test unchanged.
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
- [SW-017](SW-017-junit-export-mirrors-the-gate-verdict.md) — the export that
  renders the same results under this same threshold
- [SW-039](SW-039-report-publishes-its-pinned-threshold.md) — publishes the pinned threshold this
  rule applies, so a consumer can reach the same verdict
- [CON-012](CON-012-criterion-emptied-by-waivers-is-never-a-coverage-pass.md) — the vacuous `1.00`
  a waiver-emptied criterion publishes, which this rule no longer has to hold out of a population
- [CON-011](CON-011-waiver-matching-no-finding-fails-the-calculation.md) — the gate scoping that lets
  a waiver name a criterion this calculation excluded
- [SW-044](SW-044-report-publishes-judged-and-waived-target-counts.md) — the counts that identify a
  verdict this rule reached over criteria whose targets were waived

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
- **2026-09-18** — The threshold is now read from the report, which pins it at trigger time,
  rather than from the live gate on every arriving response.
  Unchanged for a gate nobody edits mid-calculation, which is every gate in practice.
- **2026-10-03** — Records `SW-039` (`STR-022`) as the read that publishes the pinned threshold.
  The rule here is unchanged and is deliberately not restated there: both consumers that had to
  reach this verdict — the CLI's agentic summary and the webapp's coverage bar — were inventing a
  substitute because the number was pinned on the report and served nowhere, so making it
  computable was a publishing gap rather than a defect in this comparison.
- **2026-10-04** — A result whose every target was waived leaves the share of included results that
  passed — numerator and denominator alike — rather than counting as one that passed, for `CON-012`
  (`STR-023`).
  Neither comparison changes, and the bar does not move.
  What changes is the population the second comparison is taken over, and the reason is that this rule
  applies the threshold twice.
  Waiving targets inside a criterion is meant to help that criterion, and it does: the waived targets
  leave its fraction.
  But a criterion emptied by waivers arrives at the second comparison as a `1.00`, and counting that
  as a passing included result would add a yes vote about targets nothing judged — so it could carry a
  _different_ criterion's genuine failure over the bar.
  Five included criteria at an 80% bar, three genuinely passing and two genuinely failing: waiving one
  of the failures away would score 4/5 and pass the API test with a real failure still in it, where
  dropping it scores 3/4 and still fails.
  Waiving therefore stops a criterion dragging the share down without ever voting in favour of one,
  which is the difference between an exemption and an assertion of coverage.
  Where the removal empties the share entirely, the existing empty-share rule stands and the API test
  is `PASSED`; no seventh `ReportStatus` member is introduced for it, and the published counts are
  what identify such a pass as waiver-derived.
- **2026-10-08** — The share is now compared exactly, where it was previously rounded to whole
  percents before meeting the bar.
  The bar does not move and the population does not change; what changes is that a share can no
  longer round its way over a bar it misses.
  The rounding was half-up on the ratio at two decimal places, so it could only ever lift a share
  _up_ across the bar, never drop a passing one below it — every correction is therefore a verdict
  that flips `PASSED` to `FAILED`, and none goes the other way.
  A gate's bar is bounded to 80–100 and it can select at most the fourteen criteria the coverage
  enum declares, which together make the set of affected verdicts finite and small: bars of 82, 85,
  86, 88, 89, 91, 92 and 93, each at one or two particular included-result counts.
  The smallest is six of seven included results at a bar of 86 — a true share of 85.71% that scored
  `0.86` and passed — and the lowest affected bar is 82, at nine of eleven.
  Neither predefined gate was affected: `basic-coverage` sits at 80 and the default at 100, and
  within fourteen criteria neither bar can be rounded over (80 would first need forty-four included
  results and 100 two hundred), so only a custom gate at one of those eight bars was reading a
  verdict it had not earned.
- **2026-10-08** — The threshold is no longer applied twice.
  Where the API test passed when the _share_ of included criteria clearing the bar cleared it too,
  it now passes only when **every** included criterion clears the bar, and the title and rationale
  above are rewritten accordingly.
  This supersedes the exact-share comparison recorded immediately above: there is no share left to
  compare, and the per-criterion comparison was always exact, so no rounding remains anywhere in
  this rule.
  The second application looked like a designed allowance and was not one.
  Bounded to 80–100 over at most fourteen criteria, it tolerated two failing criteria at the very
  widest, zero for most gate shapes, and none at all at bars of 95 or above — and what it did
  tolerate had no floor under it, so a criterion at 0% coverage could be carried by its neighbours.
  The deciding argument is `SW-017`.
  The export emits a `failure` of type `AssertionError` for any included criterion below the bar
  while claiming the document never contradicts the verdict it accompanies; under the share rule a
  `PASSED` suite could carry explicit failures, which is a document no build server honours, and
  that claim was simply false.
  It is now true.
  `CON-012` gets simpler rather than rewritten: it removed a waiver-emptied criterion's vacuous
  `1.00` from the share so it could not carry a different criterion's genuine failure over the bar,
  and with each criterion standing alone no criterion can carry another either way, so the removal
  is no longer load-bearing for the verdict.
  Of the four predefined gates only `basic-coverage` changes: six criteria at 80% tolerated exactly
  one failure and now tolerates none.
  `full-feature` at 100% and `minimal` at one criterion already demanded every included criterion,
  and `dry-run` selects none, so the empty-set rule still passes it.

  BREAKING: an API test that passed with one included criterion below the bar now fails, and the
  reports that roll up from it (`SW-015`) fail with it.
  No gate configuration changes and no stored report is rewritten; the new rule applies to API
  tests scored from here on.
