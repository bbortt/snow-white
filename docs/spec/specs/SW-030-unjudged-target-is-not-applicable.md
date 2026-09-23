# A target the criterion does not judge is recorded as NOT_APPLICABLE, never omitted from the findings

<!-- markdownlint-disable MD036 -->

**Title**
A target the criterion does not judge is recorded as NOT_APPLICABLE, never omitted from the
findings

**Lens**: SW

**Status**: active

**Description**
A criterion's finding list enumerates its whole target space, not only the part it passes judgement
on.
Where a criterion structurally excludes a target — the target exists in the spec, but this
criterion's rule has nothing to say about it — the finding is present with status
`NOT_APPLICABLE`.
It is never silently dropped.

The excluding rules are the criteria's own, already in the code:

- `ERROR_RESPONSE_CODE_COVERAGE` judges only `4xx`/`5xx`/`default` entries
  (`ErrorResponseCodeCoverageCalculator.judgesResponseCode`).
  A documented `200` is a `NOT_APPLICABLE` finding under that criterion, and a judged finding under
  `RESPONSE_CODE_COVERAGE`.
- The positive-response counterpart excludes the error entries symmetrically.
- The required- and optional-parameter criteria each exclude the parameters of the other's
  `required` flag.

A `NOT_APPLICABLE` finding carries no evidence, and enters neither side of the coverage fraction
(`ARCH-010`) — a criterion whose every target is inapplicable derives `required == 0` and
reports `1` under `CON-004`'s vacuous-truth convention, which is what it reports today.

**Rationale**
Omitting a target and marking it inapplicable produce the same ratio, so the difference is entirely
about what a reader can conclude from the list — and the two conclusions are not equivalent.
Against an omitted target, "this criterion says nothing about `200`" is indistinguishable from
"this criterion never saw a `200`", which is indistinguishable from a bug in target extraction.
Recording it makes the criterion's own scope visible in its output, which is the cheapest place to
catch a scope bug: a `200` that goes missing from `ERROR_RESPONSE_CODE_COVERAGE`'s list rather than
appearing as inapplicable is now a failing assertion, not an invisible absence.

The issue's own acceptance criteria assume this shape — not-applicable findings are called out as
collapsed by default in the drilldown, which is only possible if they are there to collapse.

It is also what makes the finding list an enumeration of the spec's targets rather than a list of
what happened to be interesting, and that distinction is what #2009's stale-target detection needs:
a waiver written against a target that no longer exists in the spec can only be detected as stale
by comparing against a complete target set.
An incomplete list would make a removed target and an unjudged one look alike, and the waiver work
would have to re-derive the target space it is checking against — the exact duplication this story
exists to remove.

The alternative reading — that an inapplicable target belongs to the criterion that _does_ judge it
and nowhere else — was rejected for the same reason: it is true of the verdict and false of the
scope, and a per-criterion drilldown is a view of one criterion's scope.

**Verification Description**
A unit test on `ErrorResponseCodeCoverageCalculator` over an operation documenting `200`, `404` and
`500` asserts three findings: `200` as `NOT_APPLICABLE`, and `404`/`500` judged.
The same fixture under `ResponseCodeCoverageCalculator` is asserted to judge all three, with none
inapplicable.
The required/optional parameter pair is asserted symmetrically over an operation declaring one of
each.
A test asserts that a `NOT_APPLICABLE` finding carries no evidence and that the derived ratio is
identical with and without inapplicable findings present in the list.

## Relations

**Related**

- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — the derivation that excludes
  inapplicable findings from both sides of the fraction
- [SW-029](SW-029-finding-identified-by-spec-pointer.md) — inapplicable targets are
  pointed at like any other
- [SW-031](SW-031-findings-served-with-the-report.md) — the consumer that receives them and
  will collapse them
- [SW-003](SW-003-undocumented-response-code-detection.md) — the inverse case: an observed code the
  spec does not document, which is a different concern from a documented code this criterion does
  not judge
- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the vacuous-truth
  convention an all-inapplicable criterion lands on
- [ARCH-002](ARCH-002-criteria-metadata-owned-by-enum.md) — where a criterion's own scope is
  declared

## Changes

- **2026-09-23** — Replaced "carries no trace ids" with "carries no evidence", in both the
  description and the verification description.
  The same review that made an evidence entry a `(traceId, testCaseName)` pair rather than a bare
  trace id (`ADR-0002`) left this spec asserting the absence of a shape that no longer exists; the
  claim itself — an inapplicable target is evidenced by nothing — is unchanged.
  Recorded retroactively: the edit shipped in `33cb806d` without an entry.
- **2026-09-23** — Set active: the response-code family is the first criterion group with an
  inapplicable target space, so this is the step that gives the spec something to be Covered by.
  The excluding rule is now named `judgesResponseCode` rather than
  `includeObservedResponseCodeInCalculation`: one predicate decides both which documented entries a
  criterion judges and which observed codes it will match them against, and the old name described
  only the second use.
  The rename does not alter this spec's meaning.
  The parameter criteria's symmetric case, named in the verification description, arrives with the
  step that migrates them.
