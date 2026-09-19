# Tempo's per-query trace-match limit is an operator-configurable property, not a fixed constant

<!-- markdownlint-disable MD036 -->

**Title**
Tempo's per-query trace-match limit is an operator-configurable property, not a fixed constant

**Status**: planned

**Business Value**
Operators run `openapi-coverage-stream` against APIs of very different traffic volumes and against
Tempo deployments of very different capacity.
A single hard-coded ceiling on how many matched
traces one calculation's search can return cannot be right for all of them: too low silently caps
coverage visibility for a high-traffic API, too high risks the same kind of oversized single
request this project is otherwise working to keep bounded (`NF-006`).
Today that ceiling cannot be
changed without a code change and a rebuild.

**Problem / Context**
`TempoQueryClient` fixes `SEARCH_LIMIT = 1_000` as a `private static final int`, applied as the
`limit` query parameter on every `search()` call, with no configuration surface. `NF-006` already
anchors this constant as the thing that currently satisfies its bounded-fetch requirement for the
Tempo backend — this story does not change that it is a hard bound (the query stays bounded), only
that its value becomes operator-set rather than compiled in.

**Solution Approach**
Move the limit into `TempoProperties` (`tempo.search-limit`, alongside the other `tempo.*`
connection settings already read via `@ConfigurationProperties`), with `1_000` as the default so
existing deployments see no behavior change unless they opt in. `TempoQueryClient.search` reads it
from `TempoProperties` instead of the constant.

**Acceptance Criteria**

- Setting `tempo.search-limit` changes the `limit` query parameter value on the Tempo `search()`
  request; the default (no property set) is `1000`, unchanged from today's constant.
- A non-positive configured value is rejected at context startup, not at query time, consistent
  with how this service already fails fast on other backend misconfiguration
  (`ARCH-001`).

**Out of scope**

- An equivalent configurable limit for the InfluxDB backend — InfluxDB currently applies no
  result-count bound at all (a pre-existing gap `NF-006` already tracks as follow-up, not
  introduced or closed by this story).
- Any change to what a matched trace/span returns once fetched — that is `STR-013`'s concern,
  independent of how many matches a query is allowed to return.

## Relations

**Realizes**

- [NF-007](../specs/NF-007-tempo-search-limit-is-operator-configurable.md) — the
  configurability behavior itself

**Related**

- [NF-006](../specs/NF-006-bounded-telemetry-fetch-footprint.md) — the bounded-fetch constraint
  `SEARCH_LIMIT` satisfies; this story changes who sets its value, not that it remains a bound
- [STR-013](STR-013-telemetry-fetch-requests-only-required-attribute-keys.md) — the sibling
  telemetry-fetch story narrowing fetch _width_; this story concerns fetch _count_, unaffected by
  that change
