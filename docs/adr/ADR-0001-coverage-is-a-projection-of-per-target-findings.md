# ADR-0001: Coverage becomes a projection of per-target findings, with evidence captured at the match

- Status: Proposed
- Date: 2026-09-22
- Deciders: @bbortt

## Context

Snow-White's premise (`STK-001`) is correlating an API specification with the telemetry that
exercised it.
The correlation is computed — and then discarded.

`AbstractOpenApiCoverageCalculator.CoverageCalculationResult` carries a `BigDecimal coverage` and a
nullable `additionalInformation` string.
Each of the 14 `OpenApiCoverageCriteria` calculators decides a verdict per target, reduces those
verdicts to two counters, and renders the uncovered ones into prose
(`"The following error codes in paths are uncovered: ..."`).
The covered targets leave no trace at all.

The evidence is lost one step earlier still.
`OpenTelemetryData` carries `spanId` and `traceId`, but every calculator flattens
`List<OpenTelemetryData>` into a `Set<String>` of observed values before matching:

```java
Set<String> observedErrorCodes = extractObservedErrorCodes(
  getTelemetryForTemplate(pathToTelemetryMap, path)
);
if (isCovered(responseCode, responseCodes, observedErrorCodes)) { … }
```

A search for `traceId` or `spanId` across the calculator package finds no hit on any matching path;
the single `spanId` reference is a debug log.
By the time a verdict is taken, the span that justified it is gone.

Two features press on this at once.

**#1642** asks for a drilldown: per-target status, and for a covered target the traces that
satisfied it.
Its acceptance criterion 3 — a covered finding lists its satisfying traces — is not reachable by
adding a column.

**#2009** asks for criterion-level waivers whose unit of exemption is "the evaluation unit, not the
criterion instance", requires failing on waivers that target something the spec no longer contains,
and requires a waived target to be distinguishable from a covered one in the JUnit export.
None of the three has a representation in a `BigDecimal`.

Both want the same missing primitive: the per-target verdict, kept.

## Decision

Coverage stops being a computed value and becomes a projection of a record.

**D1 — A calculator's primary output is its findings.**
`calculateCoverage` returns the ordered list of targets it judged, one finding per target, each
carrying status, identity and evidence.
The coverage ratio is derived from that list once, in `AbstractOpenApiCoverageCalculator`, under
`CON-004`'s existing rules.
No calculator computes, adjusts, or returns a ratio of its own; the per-calculator counters are
removed rather than kept in parallel.

**D2 — Evidence is captured by the match that proves it.**
Matching predicates that answer `boolean` today answer with the attributed `OpenTelemetryData`
instead, and the finding's trace ids are that subset's distinct trace ids.
The flattening helpers between telemetry and the match are removed.
A span is attributed to a target only where the criterion's own rule accepted it — never by
proximity on the same path, operation or trace.
On the eleven forward criteria a covered finding is exactly one whose subset is non-empty and an
uncovered finding carries no traces; on the three inverted criteria of D3b the polarity flips.

**D3 — A finding is identified by an RFC 6901 pointer into the spec**
(`/paths/~1pung~1{message}/get/responses/404`), built from the document's own keys with RFC 6901
escaping, addressing the specification and never rewritten toward an observed concrete path.
The `httpPath` / `httpMethod` / `responseCode` / `parameterName` / `contentType` discriminators are
denormalized projections of it, for query shape.

**D3b — The three inverted criteria are modelled, not carved out.**
`NO_UNDOCUMENTED_RESPONSE_CODES` and its positive and error variants (`SW-003`) ask whether every
_observed_ code was documented, so their target is an observed code the spec may not contain.
Their pointer addresses the nearest container the document does have — the operation's `responses`
map — with the judged code on `responseCode`; a pointer is never built toward a node that does not
exist, since that would read as an assertion that the spec has the entry, the exact opposite of the
finding's meaning.
Consequently identity is the pointer **together with** the discriminators, not the pointer alone.
Their evidence attaches to the _uncovered_ finding: the traces that exhibited the undocumented code
are precisely what a developer needs there.

**D4 — The target space is enumerated in full.**
A target this criterion structurally does not judge — a documented `200` under
`ERROR_RESPONSE_CODE_COVERAGE` — is a `NOT_APPLICABLE` finding, not an omission.
Inapplicable findings sit outside both sides of the coverage fraction.

**D5 — Findings ride the existing result event and persist beside the result.**
`OpenApiTestResult` widens; there is no second topic.
`ApiTestResult.coverage` remains as a denormalized cache for list-shaped reads, and
`CON-009` binds it to always equal what its findings imply.
Finding status persists as a stable numeric code per `ARCH-006`, and is serialised by name on the
read API.

**D6 — Phase 1 ships three statuses, not four.**
`COVERED | UNCOVERED | NOT_APPLICABLE`.
`WAIVED` is #2009's to add, on top of this model.

## Consequences

**What becomes easier.**
The report stops asserting and starts evidencing: a developer sees which spec location failed and
can prove which test satisfied the ones that passed.
`CON-004`'s subtle rules — vacuous truth at `required == 0`, half-up rounding, the clamp that must
never let `999/1000` reach `1.00` — are implemented once instead of fourteen times, and a future
calculator inherits them.
#2009 lands on a model that already has the thing it exempts: `WAIVED` becomes one more status the
shared reduction understands, stale-target detection becomes a set comparison against a complete
enumeration, and `<skipped>` in the JUnit export becomes reachable without nudging a denominator.
Building the waiver schema — which ships in consumers' `snow-white.json`, a public versioned
contract — against the settled model avoids either freezing the wrong shape or breaking users
later.

**What becomes harder.**
D2 is expensive and unglamorous: it changes the matching logic of all 14 criteria rather than
adding a field to a record.
`STR-007`'s correctness suites are what make it affordable — each calculator's ratio is pinned, so
a migration that changes a verdict fails a test instead of shipping.

Three existing decisions resist the per-target shape and must be honoured rather than worked
around.
`SW-002` makes a documented `default` response a wildcard satisfied by observed codes that matched
no other documented entry, and `SW-007` gives required-error-field checks the same shape; in both,
a target's evidence is a set defined by exclusion, so it cannot be derived by looking at that
target alone.
`SW-003` is the sharper one, and the reason D3b exists: it inverts three of the fourteen criteria,
breaking both the pointer (no node to address) and the evidence polarity (traces belong to the
uncovered finding) of the model the other eleven suggest.
It was found by auditing the criteria enum against the draft, not anticipated — the naive model
had already been written down before it surfaced.

Findings are the highest-cardinality rows the system writes — one per target, per criterion, per
API test — with a grandchild table for trace ids.
The storage cost is real and is the reason status is a numeric code rather than a name.

Two amendments fall out: `SW-020`'s replace-not-accumulate guarantee has to extend to a child
collection, and `ARCH-006` has to cover finding status.

**Rejected alternatives.**

_Findings as a parallel return value, keeping each calculator's ratio._
The cheaper diff and the worse decision: ratio and findings become independently derived, a bug in
either is invisible until a user notices the bar and the drilldown disagree, and `CON-009`
becomes an assertion with nothing structural behind it.
It also leaves #2009 no single place to subtract a waived target.

_Post-hoc re-scan: keep flattened matching, then attach plausible spans to each covered finding._
Rejected as incorrect, not merely imprecise.
Under `SW-002` the re-scan would have to re-implement the exclusion rule it was avoiding, and
elsewhere it over-attributes — two spans can carry the same status code where only one satisfied
the target.
Evidence that is sometimes wrong is worse than no evidence, because it gets quoted.

_HTML in `additionalInformation`._
Proposed in the 2026-09-07 issue comment and withdrawn in the 2026-09-07 follow-up: markup in a LOB
lets Snow-White display evidence without ever letting anything query it.

_Carving the three inverted criteria out of findings entirely, leaving them ratio-only._
Rejected: they are the criteria whose failures are least self-explanatory from a ratio — "83% of
observed codes were documented" names none of the offenders — and #2009's waivers would then have
no target to exempt on exactly the criteria most likely to need one.

_A dedicated drilldown endpoint._
Would invent a public URL for a criterion result whose identity is composite and currently
unexposed, to serve a consumer that expands inline beside a bar already on screen.
Additive to introduce later; breaking to remove.

_Backfilling findings for historical reports._
The evidencing telemetry is subject to backend retention, so a backfill would assert per-target
verdicts nobody computed.
Historical results keep their ratio with an empty finding collection, explicitly exempted from
`CON-009`.

**Reversibility.**
Low for D1 and D2 — unwinding them means restoring fourteen reductions and re-flattening fourteen
matching paths.
Moderate for D5: the event and column shapes are additive, and the pre-migration exemption means
old rows stay readable.
High for D6: adding `WAIVED` is what tolerant decoding (`ARCH-006`) was chosen to make cheap.

## Related specs

This decision is carried by [STR-017](../spec/stories/STR-017-api-test-findings-back-every-criterion-coverage.md)
and the specs it realizes:

- `ARCH-010` — D1, the calculator contract: findings are the output, the ratio is derived
- `ARCH-011` — D2, evidence captured at the match
- `ARCH-012` — D5, findings on the event, coverage as a denormalized cache
- `SW-029` — D3 and D3b, the RFC 6901 pointer and the inverted criteria
- `SW-030` — D4, the fully enumerated target space
- `SW-031` — findings served inline with the report
- `CON-009` — the cached ratio never disagrees with the findings
