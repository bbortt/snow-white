# A coverage ratio is always well-defined and bounded between 0 and 1

<!-- markdownlint-disable MD036 -->

**Title**
A coverage ratio is always well-defined and bounded between 0 and 1

**Lens**: CON

**Status**: active

**Description**
Every `OpenApiCoverageCriteria` calculator reports its result as a `covered` count over a
`required` count, reduced to a single ratio.
That ratio must always satisfy `0 ≤ ratio ≤ 1`, and must always be defined, even when `required`
is `0`.
A calculator counts an item as `covered` only when that same item was already counted toward
`required` — `covered` is a subset of `required` by construction — so the general case
(`required > 0`) is never negative and never exceeds `1`.
Two edge cases outside the general division are fixed by convention rather than left undefined:
`required == 0` reports `1` (vacuous truth — nothing was required, so nothing is missing), and
`required > 0` with `covered == 0` reports `0`.
The ratio is rounded to two decimal places, rounding half up.

**Rationale**
A coverage number feeds a quality gate's pass/fail decision.
An undefined ratio (division by zero) or a ratio outside `[0, 1]` would be meaningless to a gate
threshold, and a `covered` count that is not provably a subset of `required` would let a bug
inflate a reported percentage without any structural check catching it.
Fixing the two edge cases by convention, rather than leaving them to whatever a division happens
to produce, keeps every calculator's output comparable without a caller needing to special-case an
empty criterion.

**Verification Description**
A property-based test generates arbitrary non-negative `covered`/`required` pairs with
`covered ≤ required`, computes the ratio, and asserts the result is always in `[0, 1]`; a
`required == 0` case asserts the result is exactly `1`, and a `covered == 0, required > 0` case
asserts the result is exactly `0`.

## Relations

**Related**

- [SW-001](SW-001-structural-call-coverage.md) — a calculator this invariant governs
- [SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md) — a calculator this
  invariant governs
- [SW-003](SW-003-undocumented-response-code-detection.md) — a calculator this invariant
  governs
- [SW-004](SW-004-parameter-coverage-matches-by-token-not-substring.md) — a calculator this
  invariant governs
- [SW-005](SW-005-content-type-coverage.md) — a calculator this invariant governs
- [SW-006](SW-006-required-error-fields-coverage.md) — a calculator this invariant governs
- [CON-001](CON-001-deterministic-analysis-results.md) — determinism the same
  calculators must also honor
