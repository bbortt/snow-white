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
The ratio is rounded to two decimal places, rounding half up — except that rounding must never
produce `1.00` unless `covered == required` exactly: a `covered < required` ratio that would
otherwise round up to `1.00` (for example `999/1000`) is instead reported as `0.99`, the largest
representable value below `1.00` at this scale.
A reported ratio of exactly `1` is therefore always literal completeness, never an artifact of
rounding.

**Rationale**
A coverage number feeds a quality gate's pass/fail decision, and at least one consumer
(`JUnitReporter.buildForApiTestResult`) treats a coverage value equal to `1` as passing and
anything less as a failure — it compares the persisted, already-rounded ratio, not the original
`covered`/`required` counts, which are not retained downstream.
An undefined ratio (division by zero) or a ratio outside `[0, 1]` would be meaningless to a gate
threshold, and a `covered` count that is not provably a subset of `required` would let a bug
inflate a reported percentage without any structural check catching it.
Fixing the two edge cases by convention, rather than leaving them to whatever a division happens
to produce, keeps every calculator's output comparable without a caller needing to special-case an
empty criterion.
Clamping the near-complete case below `1.00` preserves that same guarantee for a consumer that can
only see the rounded ratio: standard rounding alone would let `999/1000` (99.9% covered) display
and compare identically to `1000/1000` (100% covered), silently passing a gate that should fail.

**Verification Description**
A property-based test generates arbitrary non-negative `covered`/`required` pairs with
`covered ≤ required`, computes the ratio, and asserts the result is always in `[0, 1]`; a
`required == 0` case asserts the result is exactly `1`, and a `covered == 0, required > 0` case
asserts the result is exactly `0`.
A dedicated case with `covered < required` chosen so the unclamped division would round to `1.00`
(for example `999/1000`) asserts the result is `0.99`, not `1.00`.

## Changes

- **2026-09-14** — Added the near-complete rounding clamp: a `covered < required` ratio that would
  round to `1.00` under plain half-up rounding is now reported as `0.99` instead.
  Found that
  `JUnitReporter.buildForApiTestResult` treats a coverage of exactly `1` as passing by comparing
  the persisted, already-rounded value, so the original half-up-only rule let 99.9%-covered
  criteria (e.g. `999/1000`) silently pass a quality gate as if fully covered.

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
