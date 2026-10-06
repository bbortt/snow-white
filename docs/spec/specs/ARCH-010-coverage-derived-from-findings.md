# A calculator yields per-target findings; its coverage ratio is derived from them, never computed beside them

<!-- markdownlint-disable MD036 -->

**Title**
A calculator yields per-target findings; its coverage ratio is derived from them, never computed
beside them

**Lens**: ARCH

**Status**: active

**Description**
No result record carries a `BigDecimal coverage` that a subclass computed for itself;
`AbstractOpenApiCoverageCalculator.CoverageCalculationResult` is gone rather than emptied.
A calculator's `calculateFindings` returns the ordered list of targets it judged — one finding per
target, each with its status and evidence — and the abstract base derives the ratio from that list
in one place, as `COVERED` over `COVERED + UNCOVERED`, before assembling the `OpenApiTestResult`.

No calculator may compute, adjust, or return a ratio of its own.
The counters this replaces — `ResponseCodeCoverageCalculator`'s `coveredErrorCodes` and
`totalErrorCodes`, and the equivalents in every sibling — are removed rather than kept in parallel:
a second count is a second source of truth, and the one thing this decision exists to prevent is a
ratio that disagrees with the findings behind it (`CON-009`).

`NOT_APPLICABLE` findings are outside both sides of the fraction (`SW-030`), so a criterion
whose every target is inapplicable derives `required == 0` and reports `1` by `CON-004`'s vacuous
-truth convention — the same value it reports today.

**Rationale**
Every calculator already decides the per-target verdict; only the reduction to a scalar is shared
in spirit and duplicated in fact.
Fourteen independent reductions is fourteen chances to diverge from `CON-004`, whose rules are
subtle enough to be worth centralising on their own: the `required == 0` vacuous-truth case, the
two-decimal rounding, and the clamp that must never let `999/1000` round up to `1.00`.
Deriving once means those rules are implemented once and tested once, and a new calculator gets
them for free instead of being expected to remember them.

The alternative — keep each calculator returning a ratio and add findings as a second, parallel
return value — was rejected.
It is the cheaper diff and the worse decision: it makes the ratio and the findings independently
derived, so a bug in either is invisible until a user notices the bar and the drilldown disagree,
and `CON-009` would become an assertion nobody can structurally guarantee.
It also leaves the criterion-waiver work (#2009) with no single place to subtract a waived target
from the denominator; with derivation centralised, `WAIVED` becomes one more status the shared
reduction understands.

Findings are ordered, not a `Set`, because the drilldown renders them and a stable order is
cheaper to guarantee at the source than to reconstruct per consumer; identity for
replace-on-redelivery comes from the spec pointer (`SW-029`), not from list position.

**Verification Description**
A unit test per calculator asserts that `calculateFindings` returns one finding per target in that
criterion's target space and that the derived ratio equals the ratio the same calculator produced
before this change, for identical fixtures — `STR-007`'s existing correctness suites pinned
against the new contract.
A test on `AbstractOpenApiCoverageCalculator` asserts the derivation applies `CON-004`'s rules
(vacuous truth at `required == 0`, half-up rounding to two places, the sub-`1.00` clamp) and that
`NOT_APPLICABLE` findings enter neither numerator nor denominator.
A review check confirms no calculator subclass constructs a `BigDecimal` ratio.

## Relations

**Related**

- [ARCH-011](ARCH-011-evidence-captured-at-the-match.md) — how each returned finding
  obtains the evidence it carries
- [ARCH-012](ARCH-012-findings-on-the-event-coverage-as-cache.md) — where the derived
  ratio goes once the findings leave the calculator
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — why an inapplicable target is in
  the list but outside the fraction
- [CON-009](CON-009-coverage-agrees-with-findings.md) — the invariant single-point
  derivation exists to make structural
- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the ratio rules this
  decision centralises rather than changes
- [ARCH-002](ARCH-002-criteria-metadata-owned-by-enum.md) — the existing
  metadata-in-one-place shape this decision follows for the reduction

## Changes

- **2026-09-23** — Set active: implementation of `STR-017` began, with the shared derivation and
  the first criterion behind it.
  The remaining thirteen calculators are migrated in later steps of
  the same story; until the last one lands, the legacy `CoverageCalculationResult` path still
  exists beside the findings contract, and a calculator that has not migrated still computes its
  own ratio.
  Migrated calculators cannot: the derivation is `final` on the base class they extend,
  which is what makes the transition one-way rather than a convention.
- **2026-09-24** — The last three criteria — parameter, content-type, required-error-fields — moved
  behind the findings contract, so all fourteen now derive their ratio here.
  With no legacy caller left, the transitional base class that held the two paths side by side was
  deleted and its derivation folded into `AbstractOpenApiCoverageCalculator`; `calculate` is `final`
  there and `CoverageCalculationResult` no longer exists, so a subclass has no type through which to
  return a ratio of its own.
  Description and Verification Description are corrected to name what is now there rather than the
  shape the transition passed through.
