# Evaluation of correlated API behavior against a fixed set of specification-derived criteria, per API version

<!-- markdownlint-disable MD036 -->

**Title**
Evaluation of correlated API behavior against a fixed set of specification-derived criteria, per
API version

**Lens**: SYS

**Status**: active

**Description**
Snow-White evaluates the correlated specification and telemetry against a fixed, predefined set of
criteria (for example path coverage, response-code coverage, parameter coverage — see
`pages/_pages/quality-gate-criteria.md` for the full list), and expresses each criterion's outcome
as fulfilled or unfulfilled.
Results are associated with the specific API version analyzed, not the API in general.

**Rationale**
A fixed, named criteria set makes results comparable across runs and across services, and lets a
quality gate reference criteria by name rather than inventing a check per analysis.
Versioning results keeps a change to the API's contract from silently blending with a prior
version's history.

**Verification Description**
A test correlates a known specification and telemetry set with a deliberately incomplete criterion
(e.g. an untested required parameter) and asserts that criterion is reported unfulfilled while
others are reported fulfilled.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — turns the correlation into a
  verdict

**Related**

- [SYS-003](SYS-003-openapi-as-indexed-format.md) — the format-agnostic criteria model
- [SYS-008](SYS-008-quality-gate-definitions.md) — how criteria are grouped into a gate
