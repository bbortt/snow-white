# Correct, precisely-defined per-criterion coverage calculation

<!-- markdownlint-disable MD036 -->

**Title**
Correct, precisely-defined per-criterion coverage calculation

**Status**: done

**Business Value**
Snow-White's coverage numbers are only useful if they are trustworthy.
A criterion that silently caps below 100% regardless of how well an API is tested, or one that
reports a parameter as tested when it never actually appeared in a request, produces a wrong
verdict that a quality gate then enforces — the opposite of what the tool exists to do.
This story fixes the two confirmed cases of that and pins down, for every criterion, exactly what
"covered" and "required" mean, so future changes have a spec to work from instead of only the
implementation.

**Problem / Context**
A source-level audit of `openapi-coverage-stream`'s `service/calculator` package (13
`OpenApiCoverageCriteria` calculators) found two calculators whose counting logic does not match
their own stated intent:

- `RESPONSE_CODE_COVERAGE` and `ERROR_RESPONSE_CODE_COVERAGE` count a documented `default` response
  toward the required total, but match it against observed telemetry using the literal string
  `default` — which real telemetry (always a numeric HTTP status) can never produce.
  Any operation documenting a `default` response therefore cannot reach 100% coverage no matter how
  thoroughly it is tested.
  The calculator's own diagnostic message claims default codes are "ignored for the calculation,"
  which is not true of the arithmetic.
  Two sibling calculators (`NO_UNDOCUMENTED_RESPONSE_CODES`, `REQUIRED_ERROR_FIELDS_COVERAGE`)
  already implement `default` correctly as a wildcard catch-all — the pattern exists in the
  codebase, it is just not applied consistently.
- `PARAMETER_COVERAGE` (and its `REQUIRED_`/`OPTIONAL_` subsets) detect a query parameter's presence
  by substring search (`queryString.contains(paramName + "=")`, `.contains(paramName + "&")`,
  `.endsWith(paramName)`) instead of parsing the query string into parameter tokens.
  A parameter named `id` is falsely reported covered by a query string containing `validId=5`.

Beyond these two, the remaining 11 calculators were read in full and found to correctly implement
their stated criterion — but no spec exists yet describing what each one computes, so there is
nothing to check a future change against beyond the implementation itself.

**Solution Approach**
Document, per criterion family, the exact covered/required definition each calculator must
implement — including the two corrected behaviors above — as software specs, plus one constraint
spec for the shared bounded-ratio invariant every calculator relies on
(`MathUtils.calculatePercentage`).
Then bring the two incorrect calculators in line with their spec.
No other calculator's behavior changes; this story documents their existing, already-correct
logic rather than changing it.

**Acceptance Criteria**

- `RESPONSE_CODE_COVERAGE` and `ERROR_RESPONSE_CODE_COVERAGE` treat a documented `default` response
  as satisfied by any observed status code in the corresponding range, the same way
  `NO_UNDOCUMENTED_RESPONSE_CODES` and `REQUIRED_ERROR_FIELDS_COVERAGE` already do — an operation
  whose every real response is exercised reaches 100% even when it documents a `default` response.
- `PARAMETER_COVERAGE` (and its required/optional subsets) detects a query parameter's presence by
  parsed parameter name, not substring containment — a query string containing `validId=5` does not
  count as covering a parameter named `id`.
- Every one of the 13 `OpenApiCoverageCriteria` has a software spec stating its covered/required
  definition precisely enough that a test can check a calculator against it independently of
  reading the implementation.
- The shared coverage-ratio formula's bounded-result invariant (`0 ≤ coverage ≤ 1`, vacuous-truth
  and zero-covered edge cases) is recorded as a constraint spec.

**Out of scope**

- The lower-severity findings from the same audit (a stale javadoc cross-reference, two calculators
  duplicating shared status-code classification logic inline instead of reusing it, the scale-2
  `HALF_UP` rounding letting a near-complete ratio display as `1.00`) — these do not change a
  reported coverage number and are tracked as straight cleanup, not a spec-worthy behavior change.
- Whether `default` should be classified as an error code project-wide
  (`HttpStatusCodeUtils.isErrorHttpStatusCode`) — addressed separately by
  [SW-007](../specs/SW-007-default-response-key-is-the-error-fallback-case.md), which
  documents the existing, unchanged classification rather than revisiting it here.

## Relations

**Realizes**

- [CON-004](../specs/CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the
  shared formula invariant
- [SW-001](../specs/SW-001-structural-call-coverage.md) — path, method, and operation-success
  coverage
- [SW-002](../specs/SW-002-response-code-coverage-treats-default-as-wildcard.md) — response-code
  coverage, corrected
- [SW-003](../specs/SW-003-undocumented-response-code-detection.md) — the inverse,
  already-correct direction
- [SW-004](../specs/SW-004-parameter-coverage-matches-by-token-not-substring.md) — parameter
  coverage, corrected
- [SW-005](../specs/SW-005-content-type-coverage.md) — request body content-type coverage
- [SW-006](../specs/SW-006-required-error-fields-coverage.md) — required error-field
  coverage
- [SW-007](../specs/SW-007-default-response-key-is-the-error-fallback-case.md) — the
  existing `default`-as-error classification, stated explicitly

**Related**

- [SYS-006](../specs/SYS-006-criteria-based-evaluation.md) — the system-level evaluation these
  specs detail
- [CON-001](../specs/CON-001-deterministic-analysis-results.md) — determinism these calculators
  must also honor
- [STR-003](STR-003-criteria-based-analysis.md) — the story that introduced
  criteria-based analysis
- [STR-008](STR-008-openapi-coverage-stream-backend-and-messaging-architecture.md) — the
  sibling story documenting this same service's backend and messaging architecture
