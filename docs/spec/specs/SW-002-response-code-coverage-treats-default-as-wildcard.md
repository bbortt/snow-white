# Response-code coverage treats a documented `default` response as satisfied by any observed status in its implied range

<!-- markdownlint-disable MD036 -->

**Title**
Response-code coverage treats a documented `default` response as satisfied by any observed status
in its implied range

**Lens**: SW

**Status**: active

**Description**
Three criteria measure whether documented response codes were actually observed:

- `RESPONSE_CODE_COVERAGE`: required is every response code entry in an operation's specification
  (exact codes like `404`, wildcard patterns like `4XX`, and `default`); covered is an entry with a
  matching observed status code.
- `POSITIVE_RESPONSE_CODE_COVERAGE`: the same, restricted to entries in the `1xx`–`3xx` range.
  `default` is never in this range and is excluded from both required and covered for this
  criterion.
- `ERROR_RESPONSE_CODE_COVERAGE`: the same, restricted to entries in the `4xx`–`5xx` range,
  including `default`.

For an exact code (`404`), a match requires the identical observed code.
For a wildcard pattern (`4XX`), a match requires an observed code sharing the leading digit.
For a documented `default` entry, a match requires **any** observed status code that is not
covered by another, more specific entry in the same operation — `default` is a catch-all, not a
literal code, and can never appear as an observed value.

**Rationale**
`default` is OpenAPI's designated catch-all for "any status not otherwise listed" — it names a
category, not a value that ever appears on the wire.
Requiring a literal-string match against it makes it unsatisfiable by any real telemetry, so an
operation that documents a `default` response could never reach 100% on `RESPONSE_CODE_COVERAGE`
or `ERROR_RESPONSE_CODE_COVERAGE` regardless of how completely it is tested — undermining the
criterion's purpose.
The wildcard treatment here matches how `NO_UNDOCUMENTED_RESPONSE_CODES` already treats `default`
in the opposite direction (an observed code is "documented" if the spec has a `default`), so the
two directions of the same concept — is this documented code observed, is this observed code
documented — now agree.

**Verification Description**
A test specifies an operation whose only documented responses are `200` and `default`, correlates
it with telemetry containing a `200` and a `404` (a genuine, unlisted-by-code response covered
only by `default`), and asserts `RESPONSE_CODE_COVERAGE` and `ERROR_RESPONSE_CODE_COVERAGE` both
report 100% for that operation — not capped below it by the unmatchable `default` entry.

## Relations

**Realizes**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the evaluation this criterion
  family is part of

**Related**

- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the shared
  formula this criterion family uses
- [SW-003](SW-003-undocumented-response-code-detection.md) — the inverse direction, whose
  existing `default` handling this spec now matches
- [SW-007](SW-007-default-response-key-is-the-error-fallback-case.md) — how `default` is
  classified as error vs. positive elsewhere
