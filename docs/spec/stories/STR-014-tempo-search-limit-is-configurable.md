# Tempo's per-query search bounds are operator-configurable properties, not a fixed constant and a hidden backend default

<!-- markdownlint-disable MD036 -->

**Title**
Tempo's per-query search bounds are operator-configurable properties, not a fixed constant and a
hidden backend default

**Status**: done

**Business Value**
Operators run `openapi-coverage-stream` against APIs of very different traffic volumes and against
Tempo deployments of very different capacity.
A single hard-coded ceiling on how many matched
traces one calculation's search can return cannot be right for all of them: too low silently caps
coverage visibility for a high-traffic API, too high risks the same kind of oversized single
request this project is otherwise working to keep bounded (`NF-006`).
Today that ceiling cannot be
changed without a code change and a rebuild.

The second bound is worse than inflexible — it is invisible.
A Tempo search returns at most three
spans per span-set unless the caller says otherwise, and this service has never said otherwise, so
any trace matching more than three spans has been contributing only three of them to coverage.
No
error is raised and nothing in the response marks the response as truncated, which makes it exactly
the kind of under-reporting a coverage tool must not do: the busier the API, the more it
under-reports, and the result still looks like a clean answer.

**Problem / Context**
`TempoQueryClient` fixes `SEARCH_LIMIT = 1_000` as a `private static final int`, applied as the
`limit` query parameter on every `search()` call, with no configuration surface. `NF-006` already
anchors this constant as the thing that currently satisfies its bounded-fetch requirement for the
Tempo backend — this story does not change that it is a hard bound (the query stays bounded), only
that its value becomes operator-set rather than compiled in.

No `spss` parameter is sent at all, so Tempo applies its own documented default of `3` spans per
span-set. `TempoTelemetryServiceImpl.resolveMatchedSpans` compounds this by reading the deprecated
singular `spanSet` field rather than the `spanSets` array that replaced it, and it collects only
the span ids found there before fetching each trace in full — so the by-ID fetch this service
performs today, despite returning a complete trace, still only ever yields the spans that survived
the `spss` cap.
The truncation therefore already affects production, and is not introduced by
`STR-013`'s removal of that fetch; what `STR-013` changes is that the search response becomes the
only source of spans, which makes fixing this a prerequisite of that story rather than an
improvement alongside it.

**Solution Approach**
Move both bounds into `TempoProperties`, alongside the other `tempo.*` connection settings already
read via `@ConfigurationProperties`:

- `tempo.search-limit`, defaulting to `1000` — the value `SEARCH_LIMIT` fixes today, so existing
  deployments see no behavior change unless they opt in.
- `tempo.spans-per-trace-limit`, defaulting to `100`, sent as `spss` on every search.
  This one does
  change existing behavior, deliberately: `100` is Tempo's own server-side
  `max_spans_per_span_set` default, making it the largest value a stock Tempo accepts, and a
  request above the server's configured maximum is rejected rather than clamped.

`TempoQueryClient.search` reads both from `TempoProperties` instead of the constant and the absent
parameter. `resolveMatchedSpans` reads `spanSets`, falling back to `spanSet` only when `spanSets`
is absent or empty, so the two fields — which Tempo populates with the same spans for backward
compatibility — cannot double-count.

**Acceptance Criteria**

- Setting `tempo.search-limit` changes the `limit` query parameter value on the Tempo `search()`
  request; the default (no property set) is `1000`, unchanged from today's constant.
- Every `search()` request carries an explicit `spss` parameter; the default (no property set) is
  `100`, and setting `tempo.spans-per-trace-limit` changes it.
- A trace whose span-set carries more than three matched spans contributes all of them to the
  returned telemetry data, where today it contributes three.
- A search response populating both `spanSets` and the deprecated `spanSet` with the same spans
  yields each span exactly once.
- A non-positive `tempo.search-limit`, or a negative `tempo.spans-per-trace-limit`, is rejected at
  context startup, not at query time, consistent with how this service already fails fast on other
  backend misconfiguration (`ARCH-001`).
  Zero is valid for `tempo.spans-per-trace-limit` alone,
  where Tempo reads it as "no per-span-set limit" — available only to operators who have also
  removed the server-side ceiling.

**Out of scope**

- An equivalent configurable limit for the InfluxDB backend — InfluxDB currently applies no
  result-count bound at all (a pre-existing gap `NF-006` already tracks as follow-up, not
  introduced or closed by this story).
- Any change to _which attributes_ a matched span returns once fetched — that is `STR-013`'s
  concern, independent of how many matches a query is allowed to return.
- Detecting or reporting that a response _was_ truncated by either bound.
  Tempo's search response
  carries no marker distinguishing "this trace had exactly `spss` matching spans" from "this trace
  had more and was cut off", so a truthful truncation warning is not available from the response
  alone; raising the bound above what any realistic trace reaches is the mitigation this story
  offers.

## Relations

**Realizes**

- [NF-007](../specs/NF-007-tempo-search-limit-is-operator-configurable.md) — the per-query
  trace-count bound's configurability
- [NF-008](../specs/NF-008-tempo-search-returns-every-matched-span-per-trace.md) — the
  per-trace matched-span bound's configurability, and sending it explicitly rather than
  inheriting Tempo's default of three

**Related**

- [NF-006](../specs/NF-006-bounded-telemetry-fetch-footprint.md) — the bounded-fetch constraint
  both values satisfy; this story changes who sets them, not that they remain bounds
- [STR-013](STR-013-telemetry-fetch-requests-only-required-attribute-keys.md) — the sibling
  telemetry-fetch story narrowing fetch _width_; this story concerns fetch _count_, on both the
  trace and per-trace-span axes. `SW-022` removes the per-trace follow-up fetch, which makes this
  story's `spss` fix a prerequisite rather than an independent improvement
- [SW-022](../specs/SW-022-tempo-search-returns-only-required-keys.md) — the search-only Tempo
  implementation that depends on this story's bounds being set explicitly
