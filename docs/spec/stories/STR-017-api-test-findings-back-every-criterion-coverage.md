# Every criterion's coverage is backed by per-target findings carrying the traces that satisfied them

<!-- markdownlint-disable MD036 -->

**Title**
Every criterion's coverage is backed by per-target findings carrying the traces that satisfied
them

**Status**: planned

**Business Value**
A quality gate that reports `RESPONSE_CODE_COVERAGE: 0.67` tells a developer that something is
missing but not what, and a gate that reports `1.00` gives them no way to show anyone _why_ it
passed.
Snow-White's whole premise (`STK-001`) is correlating a specification with the telemetry that
exercised it; today that correlation is computed and then thrown away, and what survives into the
report is a rounded ratio plus a prose sentence
(`"The following error codes in paths are uncovered: ..."`).
Nothing downstream can query it, link to a trace, or tell a covered target from an inapplicable
one.

Making the per-target verdict a first-class, queryable record turns the report from an assertion
into evidence: a developer can see which spec location failed, and prove which test satisfied the
ones that passed.
It is also the missing primitive for criterion-level waivers (#2009), whose unit of exemption is
the individual target — a concept that has no representation in the system today.

**Problem / Context**
Every `OpenApiCoverageCriteria` calculator already computes the per-target verdict this story
wants to expose, and then discards it.

`AbstractOpenApiCoverageCalculator.CoverageCalculationResult` is the choke point:

```java
public record CoverageCalculationResult(
  BigDecimal coverage,
  @Nullable String additionalInformation
) {}
```

`ResponseCodeCoverageCalculator.calculateCoverage` builds a `Set<ResponseCode>` per operation,
decides `isCovered(...)` for each one, and reduces the outcome to two `AtomicInteger` counters
plus a `HashSet<String>` of `"path [code]"` strings that only exists to be joined into
`additionalInformation`.
The uncovered targets survive as prose; the covered ones do not survive at all.
The same shape repeats across the calculators under
`io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator` —
`ContentTypeCoverageCalculator`, `ParameterCoverageCalculator`,
`RequiredErrorFieldsCoverageCalculator` and the rest each collapse their targets into counters and
a message.

The evidence problem is sharper than the target problem, and it is the part of this story that is
**not** a persistence change.
`OpenTelemetryData` carries `spanId` and `traceId`, but no calculator ever reads them on a
matching path:

```java
Set<String> observedErrorCodes = extractObservedErrorCodes(
  getTelemetryForTemplate(pathToTelemetryMap, path)   // List<OpenTelemetryData> in…
);                                                     // …Set<String> out
if (isCovered(responseCode, responseCodes, observedErrorCodes)) { … }
```

A `grep` for `traceId` and `spanId` across the calculator package returns no hit on any matching
path — the sole `spanId` reference is a debug log in
`ResponseCodeCoverageCalculator.extractStatusCodeFromAttributes`.
Matching happens against a flattened `Set<String>` of observed _values_, with the span that
produced each value already gone.
Acceptance criterion 3 of the issue — _"a covered finding lists the traces that satisfied it"_ —
therefore cannot be met by adding a column; it requires every criterion's matching logic to retain
which telemetry satisfied which target instead of collapsing to a boolean.
That is the bulk of the work, not the entity.

Three existing decisions complicate the match-to-evidence step and must be honoured rather than
worked around:

- `SW-002` makes a documented `default` response a wildcard: it is covered by any observed status
  code that no _other_ documented entry of the same operation matches.
  Its evidence is therefore a set of spans defined by exclusion, not a single matching span.
- `SW-007` makes `default` the error fallback case for required-error-field checks, with the same
  consequence.
- `SW-003` inverts the direction of three of the fourteen criteria.
  `NO_UNDOCUMENTED_RESPONSE_CODES`, `NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES` and
  `NO_UNDOCUMENTED_ERROR_RESPONSE_CODES` ask "was every observed code documented", so their target
  is an observed status code that the spec may not contain at all.
  Both halves of the naive model break there: the target has no node to point at, and the evidence
  attaches to the _uncovered_ finding rather than the covered one.
  The drafts resolve both rather than carving these three out.

Downstream, `OpenApiTestResult` (the Kafka payload in `internal/commons`) and `ApiTestResult`
(the persisted entity in `report-coordinator-api`) both carry `coverage` plus an
`additionalInformation` `@Lob`.
Whatever findings are produced must cross both boundaries, and `SW-020` already requires a
redelivered criterion result to _replace_ rather than accumulate — a guarantee that now has to
extend to a child collection.

**Solution Approach**
Invert the calculator contract: a calculator's primary output becomes the list of targets it
judged, each with its verdict and its evidence, and the coverage ratio is derived from that list
rather than computed beside it (`ARCH-010`).
Matching predicates that today answer `boolean` answer with the satisfying `OpenTelemetryData`
instead, so evidence is captured by the match that proves it rather than reconstructed by a second
pass (`ARCH-011`).

A finding names its target by an RFC 6901 pointer into the indexed OpenAPI document
(`SW-029`), carries the denormalized `httpPath` / `httpMethod` / `responseCode` /
`parameterName` / `contentType` discriminators the issue specifies, and holds its evidence: one
entry per distinct `(traceId, testCaseName)` pair among the spans that satisfied it, where
`testCaseName` is the test identity the span carried and is null until `SW-032`'s convention is in
use (`ARCH-011`).
The pair, rather than a bare trace id, is what makes issue AC 3's "test name, else truncated trace
id" reachable without reopening the published report contract later.
A target the criterion structurally does not judge — a `2xx` response under
`ERROR_RESPONSE_CODE_COVERAGE`, for instance — is recorded `NOT_APPLICABLE` rather than omitted
(`SW-030`), so the finding list is the complete enumeration of the criterion's target space.

Findings travel on `OpenApiTestResult` across Kafka and persist as a child collection of
`ApiTestResult`; `coverage` stays on both as a denormalized cache so the report list view keeps
reading one column (`ARCH-012`), guarded by the invariant that the cached ratio always equals
what the findings imply (`CON-009`).
The report API serves a criterion result's findings inline with the report (`SW-031`).

The 14 criteria are migrated calculator by calculator behind the new contract, with `STR-007`'s
existing correctness tests as the regression net: each calculator's coverage ratio must be
unchanged for identical input telemetry.

**Acceptance Criteria**

- For every one of the 14 `OpenApiCoverageCriteria`, a calculation over a given OpenAPI spec and
  telemetry set produces exactly one finding per target in that criterion's target space, and the
  coverage ratio derived from those findings equals the ratio that criterion produced before this
  change for the same input.
- For each of the eleven forward criteria, a `COVERED` finding carries at least one evidence entry,
  every trace id it carries belongs to a span the criterion's own matching rule accepted as
  satisfying that target — not merely a span observed on the same operation — and an `UNCOVERED`
  finding carries none.
- For each of the three inverted criteria (`SW-003`), an `UNCOVERED` finding carries exactly the
  traces of the spans that exhibited the undocumented status code.
- A `NOT_APPLICABLE` finding carries no evidence, under any criterion.
- An evidence entry is a `(traceId, testCaseName)` pair end to end — in the calculator's output, on
  the Kafka event, in the evidence table and in the `v1-report-api.yml` response — with
  `testCaseName` null, and serialised as `null` rather than omitted, where the evidencing span
  carried no test identity.
  Over telemetry carrying no test identity at all, a finding's entry count equals its distinct
  trace count.
- A `default` response entry covered under `SW-002`'s wildcard rule carries, as its evidence,
  exactly the spans whose observed status code matched no other documented entry of the same
  operation.
- Every finding's `specPointer` resolves, as an RFC 6901 pointer, against the indexed OpenAPI
  document the calculation ran on — including on the inverted criteria, where it addresses the
  operation's `responses` map rather than an entry the document does not contain.
- The persisted `ApiTestResult.coverage` equals the ratio derived from that result's stored
  findings, for every result in a completed report.
- A redelivered calculation result for a criterion replaces that criterion's findings wholesale;
  no finding from the superseded delivery remains (`SW-020` extended to the child collection).
- Fetching a report through `report-coordinator-api` returns each criterion result's findings
  without a second request.

**Out of scope**

- **The drilldown UI** — the magnifying glass, the expandable coverage bar, trace links via a
  configured link template, and the collapsed-by-default treatment of not-applicable findings.
  That is phase 2 of #1642 and ships against the API this story delivers.
- **Criterion-level waivers** (#2009).
  This story deliberately ships the three-value status `COVERED | UNCOVERED | NOT_APPLICABLE`
  without a `WAIVED` member; the waiver work adds the fourth value and the matching rules on top,
  once the target it exempts exists.
- **Producing a test identity on the span, and rendering one.**
  Issue AC 3 prefers a test name where the span carried a test identifier and falls back to a
  truncated trace id.
  The half of that this story owns is the _shape_: a finding's evidence is a list of
  `{ traceId, testCaseName? }` entries rather than a `Set<String>` of trace ids, so the slot exists
  in the persisted model and in `v1-report-api.yml` from the first release that has findings at all
  (`SW-029`, `ARCH-012`, `SW-031`).
  What stays out of scope is the producing end — the attribute convention a test harness writes and
  how it reaches the server span, specified separately (`ARCH-013`, `SW-032`) — and the rendering
  end: truncation, grouping by trace, and the trace link template, which belong to phase 2.
  `#2011` consumes that convention from a whitebox test harness; it does not own it.
- **The criteria-per-endpoint transpose and any Swagger-UI-style rendering**, explicitly excluded
  by the issue.
- **Replacing `additionalInformation`.**
  It keeps rendering as free text alongside the findings (issue AC 6).
  Whether a per-target message should migrate onto the finding, and the HTML-formatting idea from
  the 2026-09-07 comment, are follow-ups this story neither forecloses nor performs.
- **Normalising a templated spec path against a concrete observed path** beyond what
  `CalculatorUtils.getTelemetryForTemplate` already does.
  The finding's `httpPath` records the templated path as the spec states it; the observed path
  lives on the evidencing span.

## Relations

**Realizes**

- [ARCH-010](../specs/ARCH-010-coverage-derived-from-findings.md) — findings become the
  calculator's primary output and the ratio is derived from them
- [ARCH-011](../specs/ARCH-011-evidence-captured-at-the-match.md) — how a satisfying span
  reaches the finding it evidences
- [ARCH-012](../specs/ARCH-012-findings-on-the-event-coverage-as-cache.md) — how findings
  cross the Kafka and persistence boundaries
- [SW-029](../specs/SW-029-finding-identified-by-spec-pointer.md) — what names a finding's
  target
- [ARCH-013](../specs/ARCH-013-test-identity-travels-as-baggage.md) — how a test identity reaches
  the span whose match this story evidences
- [SW-032](../specs/SW-032-test-identity-on-the-span.md) — the attribute an evidence entry's
  `testCaseName` is read from
- [SW-031](../specs/SW-031-findings-served-with-the-report.md) — how a consumer reads
  findings
- [SW-030](../specs/SW-030-unjudged-target-is-not-applicable.md) — why the target space is
  enumerated in full
- [CON-009](../specs/CON-009-coverage-agrees-with-findings.md) — the cached ratio never
  disagrees with the findings

**Related**

- [STK-001](../specs/STK-001-correlate-specs-with-telemetry.md) — the stakeholder need this story
  finally makes inspectable rather than only computable
- [STR-007](STR-007-coverage-criteria-calculator-correctness.md) — the calculator-correctness
  story whose tests are this story's regression net across all 14 criteria
- [CON-004](../specs/CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the ratio
  rules the derivation must keep producing unchanged, including the `required == 0` and
  near-complete-clamping conventions
- [SW-002](../specs/SW-002-response-code-coverage-treats-default-as-wildcard.md) — the wildcard
  rule that makes a `default` target's evidence a set defined by exclusion
- [SW-007](../specs/SW-007-default-response-key-is-the-error-fallback-case.md) — the same wildcard
  shape for required-error-field checks
- [SW-003](../specs/SW-003-undocumented-response-code-detection.md) — the three inverted criteria
  whose target is an observed code, flipping both the pointer's depth and the polarity of evidence
- [SW-020](../specs/SW-020-redelivered-criterion-result-replaces-existing-one.md) — the
  replace-not-accumulate guarantee that now has to cover a child collection; amended for it on
  2026-09-22
- [ARCH-006](../specs/ARCH-006-report-status-persisted-as-stable-code.md) — the stable-numeric-code
  precedent a finding's status follows; amended for it on 2026-09-22
- [SW-016](../specs/SW-016-api-test-verdict-is-gate-scoped.md) — the gate-scoped verdict that
  continues to read the cached ratio, not the findings
- [SW-017](../specs/SW-017-junit-export-mirrors-the-gate-verdict.md) — the JUnit export that
  `#2009` will later teach to emit `<skipped>` per waived finding
- [SYS-012](../specs/SYS-012-result-consumption.md) — the result-consumption capability this story
  widens
