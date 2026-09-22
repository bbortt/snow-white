# A target's satisfying spans are captured by the match that proves it, never reconstructed afterwards

<!-- markdownlint-disable MD036 -->

**Title**
A target's satisfying spans are captured by the match that proves it, never reconstructed
afterwards

**Lens**: ARCH

**Status**: planned

**Description**
Every calculator's matching step answers with the telemetry that satisfied the target, not with a
boolean.
`ResponseCodeCoverageCalculator.isCovered(ResponseCode, Set<ResponseCode>, Set<String>)` and its
equivalents in the sibling calculators change shape: they take the operation's
`List<OpenTelemetryData>` and return the subset of it that satisfied the target under that
criterion's own rule.
A finding's trace ids are the distinct `traceId` values of the spans that criterion's rule
attributed to that target.

For the eleven forward criteria — "was every documented thing observed" — the attributed spans are
the ones that satisfied the target, so a `COVERED` finding is exactly one whose subset is non-empty
and an `UNCOVERED` finding carries no trace ids.

For the three inverted criteria (`SW-003`) — `NO_UNDOCUMENTED_RESPONSE_CODES` and its positive and
error variants, which ask "was every observed thing documented" — the observed span _is_ the
target, so it is attributed either way and the polarity of evidence flips: an `UNCOVERED` finding
for an undocumented `418` carries the traces that exhibited it.
That is the evidence a developer most needs on those criteria, and a rule that stripped trace ids
from every `UNCOVERED` finding would delete it.

The flattening helpers that stand between telemetry and the match are what make this impossible
today, and they go: `extractObservedErrorCodes` returning `Set<String>`,
and the `HashSet<String>` reductions of the same shape in `ContentTypeCoverageCalculator`,
`ParameterCoverageCalculator`, `RequiredErrorFieldsCoverageCalculator` and the rest.
Where an observed value is still the thing being compared, it is carried together with the span it
came from rather than instead of it.

Evidence must be the spans that satisfied _that target_ under _that criterion's_ rule — never
every span observed on the operation.
Two rules make this a real distinction rather than a pedantic one:

- Under `SW-002`, a documented `default` response is satisfied by the observed status codes that
  matched no _other_ documented entry of the same operation; its evidence is the set so defined,
  which is a strict subset of the operation's spans whenever any sibling entry also matched.
- Under `SW-007`, the same exclusion shape applies to the error-fallback case for required error
  fields.

No calculator may attribute a span to a target by proximity — same path, same operation, same
trace — where the criterion's own matching rule did not accept it.

**Rationale**
The per-target verdict already exists inside each calculator; the span that produced it does not
survive the step before.
Once telemetry is reduced to a `Set<String>` of observed values, the link from a value back to its
span is gone, and no amount of downstream modelling recovers it: two spans can carry the same
status code, the same content type, the same parameter token, and only one of them may be the one
a given target's rule accepted.
Capturing at the match is therefore not an optimisation over reconstructing later — reconstruction
is not correct, only plausible.

A post-hoc re-scan was the considered alternative: let the calculators keep their flattened
matching, then, for each `COVERED` finding, walk the operation's telemetry again and attach every
span that looks like it could have satisfied the target.
It is a much smaller diff and it is wrong in precisely the case the feature exists to serve.
Under `SW-002` the re-scan cannot reproduce the exclusion rule without re-implementing the match it
was trying to avoid, and everywhere else it over-attributes: a developer opening the drilldown to
prove which test covered a target would be shown spans that did not.
Evidence that is sometimes wrong is worse than no evidence, because it is quoted.

The cost is honest and should be stated: this is the expensive half of the story.
It touches the matching logic of all 14 criteria rather than adding a field to a record, and
`STR-007`'s correctness suites are the reason it is affordable at all — the ratio each calculator
produces is pinned, so a migration that changes a verdict is caught rather than shipped.

**Verification Description**
Per calculator, a unit test builds telemetry in which more than one span is observed on the same
operation and only one satisfies the target under test, then asserts the resulting finding's trace
ids name exactly that span's trace — not the operation's full span set.
A dedicated test for the `SW-002` wildcard asserts that a `default` entry's evidence contains only
the spans whose status code matched no sibling documented entry, and is empty (status `UNCOVERED`)
when every observed code matched a sibling.
For a forward criterion, an `UNCOVERED` finding is asserted to carry no trace ids even where the
operation had telemetry; for each of the three inverted criteria, an `UNCOVERED` finding is
asserted to carry exactly the traces of the spans that exhibited the undocumented code.
A review check confirms no matching path reduces `List<OpenTelemetryData>` to a collection of bare
values before the verdict is taken.

## Relations

**Related**

- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — the contract whose findings
  this decision fills with evidence
- [SW-029](SW-029-finding-identified-by-spec-pointer.md) — what the evidenced target is
  named by
- [SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md) — the wildcard rule whose
  exclusion semantics the evidence subset must reproduce exactly
- [SW-007](SW-007-default-response-key-is-the-error-fallback-case.md) — the same exclusion shape
  for required error fields
- [SW-003](SW-003-undocumented-response-code-detection.md) — the three inverted criteria on which
  the polarity of evidence flips
- [SW-021](SW-021-required-attribute-key-set-derivation.md) — the narrowed attribute set the
  evidencing spans arrive with; `traceId` and `spanId` are record identity, not narrowed
  attributes, so this decision adds no key to that set
- [STR-007](../stories/STR-007-coverage-criteria-calculator-correctness.md) — the correctness
  suites that make a 14-calculator migration safe
