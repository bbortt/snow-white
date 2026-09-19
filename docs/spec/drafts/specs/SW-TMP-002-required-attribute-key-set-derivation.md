# A calculation's required telemetry attribute-key set is the fixed calculator keys plus the target spec's own header-parameter names

<!-- markdownlint-disable MD036 -->

**Title**
A calculation's required telemetry attribute-key set is the fixed calculator keys plus the target
spec's own header-parameter names

**Lens**: SW

**Status**: planned

**Description**
Given the `OpenAPI` spec for a calculation, the required-key computation returns exactly:

- the fixed keys read by one or more of the 14 `OpenApiCoverageCriteria` calculators or by
  `OpenApiCoverageService`'s operation-grouping step: `http.request.method`, `url.path`,
  `http.response.status_code`, `url.query`, `http.request.header.content-type`, and the
  operator-configured `OpenApiCoverageStreamProperties.operationIdAttribute`;
- one `http.request.header.<paramName>` (lower-cased, matching `ParameterCoverageCalculator`'s own
  lookup) for every parameter with `in: header` declared on any operation in the given spec.

Path and query parameters contribute no key beyond the fixed `url.path` and `url.query` already
included — `ParameterCoverageCalculator` reads a path parameter's coverage from the already-matched
operation key, and a query parameter's presence from tokenizing the single `url.query` value, never
from a per-parameter attribute key.
A spec with no header parameters at all still yields the fixed
key set, never an empty one.

**Rationale**
This is the enumeration `ARCH-TMP-001` pins as the single, explicit source of the required-key set,
computed once and handed to whichever backend is active.
Keeping it as one small, readable method
— rather than spread across each calculator — makes the complete key inventory reviewable in one
place, which matters because nothing enforces it staying in sync with the calculators automatically
(`ARCH-TMP-001`'s accepted trade-off).

**Verification Description**
A unit test builds an `OpenAPI` spec with a mix of operations — one with no parameters, one with
only path/query parameters, one with header parameters (some sharing a name across operations,
some unique) — and asserts the computed key set equals the fixed keys plus exactly the distinct,
lower-cased `http.request.header.<paramName>` keys for the declared header parameters, with no
duplicates and no keys for path or query parameters.

## Relations

**Related**

- [ARCH-TMP-001](ARCH-TMP-001-required-attribute-keys-computed-once-by-caller.md) — where this
  computation is invoked and how its result is passed on
- [SW-TMP-003](SW-TMP-003-tempo-search-returns-only-required-keys.md) — the Tempo backend's use of
  this key set
- [SW-TMP-004](SW-TMP-004-influxdb-query-narrows-attributes-to-required-keys.md) — the InfluxDB
  backend's use of this key set
- [SW-004](SW-004-parameter-coverage-matches-by-token-not-substring.md) — the query-parameter
  matching behavior this spec's rationale for excluding per-query-parameter keys depends on
