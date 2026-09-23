# A calculation's required telemetry attribute-key set is the fixed calculator keys plus the target spec's own header-parameter names

<!-- markdownlint-disable MD036 -->

**Title**
A calculation's required telemetry attribute-key set is the fixed calculator keys plus the target
spec's own header-parameter names

**Lens**: SW

**Status**: active

**Description**
Given the `OpenAPI` spec for a calculation, the required-key computation returns exactly:

- the fixed keys read by one or more of the 14 `OpenApiCoverageCriteria` calculators or by
  `OpenApiCoverageService`'s operation-grouping step: `http.request.method`, `url.path`,
  `http.response.status_code`, `url.query`, `http.request.header.content-type`, and the
  operator-configured `OpenApiCoverageStreamProperties.operationIdAttribute`;
- the operator-configured `OpenApiCoverageStreamProperties.testCaseNameAttribute`
  (default `test.case.name`, `SW-032`) — the one key in this set no criterion judges: it is
  requested so an evidence entry can name the test that satisfied a target (`ARCH-011`), and a
  calculation over telemetry that never carries it is unaffected;
- one `http.request.header.<paramName>` (lower-cased, matching `ParameterCoverageCalculator`'s own
  lookup) for every parameter with `in: header` declared on any operation in the given spec.

Path and query parameters contribute no key beyond the fixed `url.path` and `url.query` already
included — `ParameterCoverageCalculator` reads a path parameter's coverage from the already-matched
operation key, and a query parameter's presence from tokenizing the single `url.query` value, never
from a per-parameter attribute key.
A spec with no header parameters at all still yields the fixed
key set, never an empty one.

The set is therefore "what a calculation reads", not "what a criterion judges" — the test-identity
key is read for evidence rather than for a verdict, and omitting it would make an unjudged attribute
silently unavailable to the only component that could ever attach it to a finding.

**Rationale**
This is the enumeration `ARCH-007` pins as the single, explicit source of the required-key set,
computed once and handed to whichever backend is active.
Keeping it as one small, readable method
— rather than spread across each calculator — makes the complete key inventory reviewable in one
place, which matters because nothing enforces it staying in sync with the calculators automatically
(`ARCH-007`'s accepted trade-off).

**Verification Description**
A unit test builds an `OpenAPI` spec with a mix of operations — one with no parameters, one with
only path/query parameters, one with header parameters (some sharing a name across operations,
some unique) — and asserts the computed key set equals the fixed keys plus exactly the distinct,
lower-cased `http.request.header.<paramName>` keys for the declared header parameters, with no
duplicates and no keys for path or query parameters.
A test asserts the configured test-identity key is in the set for every spec, including one with no
parameters at all, and that overriding the property changes the key requested.

## Relations

**Related**

- [ARCH-007](ARCH-007-required-attribute-keys-computed-once-by-caller.md) — where this
  computation is invoked and how its result is passed on
- [SW-022](SW-022-tempo-search-returns-only-required-keys.md) — the Tempo backend's use of
  this key set
- [SW-023](SW-023-influxdb-query-narrows-attributes-to-required-keys.md) — the InfluxDB
  backend's use of this key set
- [SW-004](SW-004-parameter-coverage-matches-by-token-not-substring.md) — the query-parameter
  matching behavior this spec's rationale for excluding per-query-parameter keys depends on
- [SW-032](SW-032-test-identity-on-the-span.md) — the test-identity attribute added to this set,
  and the only member of it no criterion judges
- [ARCH-013](ARCH-013-test-identity-travels-as-baggage.md) — why that attribute has to be
  requested here rather than recovered from the trace later

## Changes

- **2026-09-19** — Set active: implementation of STR-013 began.
- **2026-09-23** — Added the operator-configured test-identity key
  (`testCaseNameAttribute`, default `test.case.name`) to the required set, for `STR-017`'s evidence
  entries.
  This is the first key in the enumeration that exists for evidence rather than for a verdict, so
  the description now states the set as "what a calculation reads" rather than "what a criterion
  judges"; `ARCH-007`'s staleness trade-off is unchanged, and this is its first real exercise.
