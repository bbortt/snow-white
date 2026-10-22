# Drop redundant parent-implied criteria from the predefined quality gates

<!-- markdownlint-disable MD036 -->

**Title**
Drop redundant parent-implied criteria from the predefined quality gates

**Status**: done

**Business Value**
A predefined gate that lists both a criterion and one it already implies (for example
`RESPONSE_CODE_COVERAGE` alongside `POSITIVE_RESPONSE_CODE_COVERAGE`) reports more distinct checks
than it actually performs, which misleads a caller reading the gate's criteria list into thinking
more is being verified than actually is.
Removing the implied entries makes each predefined gate's
criteria list an accurate, minimal statement of what it checks.

**Problem / Context**
`pages/_pages/quality-gate-criteria.md` documents four parent-child criterion pairs — satisfying
the broader (parent) criterion already implies the narrower (child) one was satisfied too:
`RESPONSE_CODE_COVERAGE` over `POSITIVE_RESPONSE_CODE_COVERAGE` and
`ERROR_RESPONSE_CODE_COVERAGE`; `NO_UNDOCUMENTED_RESPONSE_CODES` over
`NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES` and `NO_UNDOCUMENTED_ERROR_RESPONSE_CODES`;
`PARAMETER_COVERAGE` over `REQUIRED_PARAMETER_COVERAGE` and `OPTIONAL_PARAMETER_COVERAGE`; and
`HTTP_METHOD_COVERAGE` over `PATH_COVERAGE`.

`DefaultOpenApiQualityGates` (the seed for the four predefined gates) does not treat this as
redundancy today. `full-feature` lists all 14 `OpenApiCoverageCriteria`, including every parent
alongside every one of its children. `basic-coverage` lists both `HTTP_METHOD_COVERAGE` and its
child `PATH_COVERAGE`.
Neither gate gains anything from including a criterion its own list already
implies — it only inflates the criteria count reported to a caller
([GitHub issue #2010](https://github.com/bbortt/snow-white/issues/2010)).

**Solution Approach**
Change `full-feature` and `basic-coverage` to list only the parent of each parent-child pair they
currently list both sides of, dropping the now-redundant child; a gate that lists a criterion with
no parent present in the same gate, or a child whose parent is absent, is unaffected.
`SW-011` already pins the exact composition of all four predefined gates, so this story revises
that spec's table to the corrected composition rather than adding a new one.

**Acceptance Criteria**

- `full-feature`'s criteria list drops `POSITIVE_RESPONSE_CODE_COVERAGE`,
  `ERROR_RESPONSE_CODE_COVERAGE`, `NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES`,
  `NO_UNDOCUMENTED_ERROR_RESPONSE_CODES`, `REQUIRED_PARAMETER_COVERAGE`,
  `OPTIONAL_PARAMETER_COVERAGE`, and `PATH_COVERAGE`, keeping their seven parents/independent
  criteria: `HTTP_METHOD_COVERAGE`, `OPERATION_SUCCESS_COVERAGE`, `RESPONSE_CODE_COVERAGE`,
  `PARAMETER_COVERAGE`, `CONTENT_TYPE_COVERAGE`, `REQUIRED_ERROR_FIELDS_COVERAGE`, and
  `NO_UNDOCUMENTED_RESPONSE_CODES`.
- `basic-coverage`'s criteria list drops `PATH_COVERAGE`, keeping `HTTP_METHOD_COVERAGE` and its
  other five listed criteria unchanged.
- `minimal` and `dry-run` are unchanged — neither lists a parent alongside its child today.
- `SW-011`'s composition table reflects the corrected criteria lists for `full-feature` and
  `basic-coverage`.
- A test asserts each predefined gate's exact (corrected) criteria set, so a future accidental
  re-addition of a redundant entry is caught.

**Out of scope**

- Changing either gate's `minCoveragePercentage` threshold — only the criteria membership changes.
- Changing `minimal` or `dry-run`, which carry no redundant pair.
- Any change to how a criterion's own coverage percentage is calculated, or to the
  `Criteria Relationships` documentation itself, which already states the hierarchy correctly.

## Relations

**Realizes**

- [SW-011](../specs/SW-011-four-predefined-gates-fixed-composition.md) — revises the
  pinned composition of `full-feature` and `basic-coverage` to drop redundant parent-implied
  criteria

**Related**

- [STR-009](STR-009-quality-gate-configuration-and-criteria-management-api.md) — the story
  that originally pinned the four predefined gates' composition
