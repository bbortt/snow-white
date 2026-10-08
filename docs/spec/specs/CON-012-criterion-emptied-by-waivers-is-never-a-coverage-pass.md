# A waived target leaves the fraction, and a criterion emptied by waivers is never published as a coverage pass

<!-- markdownlint-disable MD036 -->

**Title**
A waived target leaves the fraction, and a criterion emptied by waivers is never published as a
coverage pass

**Lens**: CON

**Status**: planned

**Description**
A `WAIVED` finding enters neither side of the coverage fraction, exactly as `NOT_APPLICABLE` does.
A criterion with ten judged targets, two of them waived, reports coverage over the remaining eight.
Waiving a target the criterion had not covered therefore raises that ratio, which is the point of the
feature, and waiving one it had covered leaves it equal or lower, never higher (`SW-041`) — so the
ratio alone never says which of the two happened.

What a waiver can never do is produce a pass out of nothing.
When every target a criterion would have judged is waived, the denominator is zero and
`CON-004`'s rule returns `1.00`.
That number is published — the arithmetic is unchanged — but it does not stand alone: the same result
publishes `judgedTargetCount: 0` and a `waivedTargetCount` above zero (`SW-044`), so the
vacuous `1.00` is distinguishable from full coverage in every shape that carries it.

The invariant this spec fixes: **a gate that passes only because of waivers must be distinguishable
from one that passes on coverage alone, in every published shape.**
That holds for the JSON reads, which carry the two counts; for the JUnit export, where a criterion
with waived targets is a pass whose `system-out` and `waivedTargets` property name them
(`SW-017`); and for the agentic CLI output, which carries the same counts.
`SW-016` needs no amendment to hold this line.
It scores an API test by requiring every included criterion to clear the bar on its own, so a
waiver-emptied criterion's vacuous `1.00` clears the bar for itself and decides nothing on any other
criterion's behalf.
An API test whose every included result is fully waived is therefore `PASSED`, and what identifies
that pass as waiver-derived is the counts rather than the status: no new report status is introduced,
which is what this invariant requires.

No consumer has to infer any of this from the ratio.
`coverage == 1.00` is never, by itself, evidence that a criterion was satisfied, and neither is a
`PASSED` status.

**Rationale**
`CON-004`'s vacuous `1` was decided for a criterion with nothing to judge — an API declaring no
parameters has full parameter coverage, and the alternative, `0`, would fail a gate for a document
that never had the targets.
Waivers break the premise that made it safe.
Before waivers, an empty target space was a property of the specification, which a reader could see
for themselves; after them, an empty target space can be something a submitter produced, and the two
cases arrive as the identical number.
The honest move is not to change the arithmetic — that would reach every criterion in the system for
the sake of one new case, and `CON-009`'s agreement obligation is written against this formula — but
to make the two origins distinguishable where they are published.

Keeping the waived target out of both sides rather than counting it as uncovered is what the feature
is for: a waiver that left the denominator alone would just move the failure from the criterion to
the threshold, and the team would be back to dropping the criterion entirely.
Counting it as covered is the alternative that must never happen, and it is the one that would have
been easiest to ship: `WAIVED` behaving like `COVERED` would make a waiver an assertion of coverage,
which is the sentence no consumer could then disbelieve.

Publishing counts rather than a verdict flag follows `SW-039`.
A `passedOnCoverageAlone` boolean would restate a rule in a second place that can drift from the
first, while two integers restate nothing and let a consumer ask the question it actually has — how
much of this criterion was waived, and was any of it judged.
The counts are also the only form that stays correct as the rule evolves.

An earlier draft of this spec additionally removed a fully-waived result from `SW-016`'s share of
included criteria that passed, and that removal was load-bearing for as long as `SW-016` applied its
threshold twice.
A vacuous `1.00` counted as a passing criterion would have raised the proportion, making a waiver
_better_ for the gate than imperfect coverage — a scoring gradient that rewards waivers, and one
that could carry a different criterion's genuine failure over the bar.
`SW-016` now requires every included criterion to clear the bar on its own, which dissolves the
concern rather than answering it: no criterion's result is weighed against another's, so a vacuous
`1.00` has nothing to lift and no failure to carry.
The removal is dropped as a requirement because keeping it would be code that cannot change an
outcome, and a rule that cannot change an outcome is a rule nobody can test.
What survives is the publishing obligation, which never depended on the share.

Declining a seventh `ReportStatus` member is the boundary of that correction, and it is a scope
judgement rather than a conviction.
A `PASSED_WITH_WAIVERS` status would read well, and it would reach every consumer of a value
`ARCH-006` persists as a stable code — the webapp, the CLI, the JUnit mapping, the status filter on
the list read — for a distinction the counts already carry.
It is also the harder direction to reverse: a published status member cannot be withdrawn.
The counts ship first, and if they prove insufficient in use, a status or a derived flag is additive
on top of them.

**Verification Description**
A test asserts a criterion with some waived targets reports coverage over the remaining judged
targets: waiving a target it had not covered raises the ratio, and waiving one it had covered leaves
the ratio equal or lower, never higher.
A test asserts a criterion whose every target is waived publishes `judgedTargetCount: 0` and a
non-zero `waivedTargetCount` alongside its ratio, in the list read, the single read and both `202`
shapes.
A test asserts two reports — one at genuine full coverage, one fully waived — are distinguishable in
every published shape: the JSON reads, the JUnit document, and `--agentic`.
A test asserts a fully-waived included result moves no other criterion's verdict in either
direction: an API test with one covered and one fully-waived included result is `PASSED`, and one
with a genuinely failing criterion beside a fully-waived one is `FAILED`, so a waiver neither
rescues another criterion nor condemns one.
A test asserts an API test whose every included result is fully waived is `PASSED`, carries
`judgedTargetCount: 0` and a non-zero `waivedTargetCount` on every result, and is therefore
distinguishable from an API test that passed on coverage.
A test asserts no new `ReportStatus` member is introduced.
A test asserts a criterion with an empty target space in the specification, carrying no waivers,
still publishes the pre-change `1.00` with `judgedTargetCount: 0` and `waivedTargetCount: 0`.
A test asserts no consumer surface derives a satisfied criterion from `coverage == 1.00` alone.

## Relations

**Related**

- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — amended: the vacuous
  `1` now has two origins, and the ratio alone never establishes a coverage pass
- [CON-009](CON-009-coverage-agrees-with-findings.md) — amended: the published counts join the ratio
  under the same agreement obligation
- [SW-044](SW-044-report-publishes-judged-and-waived-target-counts.md) — the counts that
  make the two origins distinguishable
- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — the per-criterion floor that makes a
  waiver-emptied criterion's vacuous `1.00` harmless to every other criterion's verdict
- [SW-017](SW-017-junit-export-mirrors-the-gate-verdict.md) — amended: the export shape that carries
  the same distinction
- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — amended: the reduction that excludes
  `WAIVED` from both sides
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — amended: the inapplicable target this
  treats alike arithmetically and distinguishes in publication
- [SW-039](SW-039-report-publishes-its-pinned-threshold.md) — the publish-the-number-not-the-verdict
  precedent
- [ARCH-006](ARCH-006-report-status-persisted-as-stable-code.md) — the status enum this deliberately
  does not extend
- [SYS-009](SYS-009-machine-consumable-gate-result.md) — the machine-consumable result this
  invariant binds
