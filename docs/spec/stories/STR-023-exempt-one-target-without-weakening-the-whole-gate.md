# A pipeline can exempt one target without weakening the gate for every other one

<!-- markdownlint-disable MD036 -->

**Title**
A pipeline can exempt one target without weakening the gate for every other one

**Status**: planned

**Business Value**
A team that wants the full criteria set has exactly one lever today when a single check is
impractical: drop that criterion from the gate, for the whole API.
The motivating case from a user is a documented `500` on one operation, reachable only from a
component test with the downstream mocked.
`ERROR_RESPONSE_CODE_COVERAGE` fails on that one response code, so the team either runs a weaker
gate across every endpoint or accepts a permanently red build.
Both answers lose the criterion everywhere it was working.

A waiver buys back the granularity: criterion `C` does not apply to target `T` of this API, with a
reason and an expiry, and stays enforced everywhere else.
The cost of getting it wrong is a gate that passes because somebody waived their way to green, so
the whole increment is shaped around one requirement — a report that passes with waivers must never
read like one that passed on coverage.

**Problem / Context**
Premises checked on 2026-10-03, in this session, not assumed:

- Nothing waiver-shaped exists in code.
  `QualityGateCalculationRequest.yml` carries `includeApis`, `lookbackWindow` and
  `attributeFilters` and nothing else; `QualityGateCalculationRequestEvent.java:29-39` carries
  `apiInformation`, `lookbackWindow` and `attributeFilters`.
  The only mentions of a waiver are two forward-looking javadoc lines
  (`report-coordinator-api/.../domain/model/FindingStatus.java:16-24`, `ARCH-012:70`).
- The addressing problem is already solved.
  `ApiTestFinding` (`internal/commons/.../event/dto/ApiTestFinding.java:13-23`) names its target by
  `specPointer`, built from the specification's own spelling (`SW-029`), and `SW-030` guarantees one
  finding per target the criterion enumerated — including the ones it does not judge.
  A waiver therefore has a natural key and a complete target set to resolve against.
- There is one derivation and one place to subtract from.
  `AbstractOpenApiCoverageCalculator.deriveCoverage` (`:108-127`) is the only coverage derivation;
  `calculate` is `final` (`:46`), so a subclass has no way to produce a ratio of its own
  (`ARCH-010`, `CON-009`).
  `NOT_APPLICABLE` is skipped on both sides (`:115-117`), and
  `MathUtils.calculatePercentage` returns `1.00` for a zero denominator (`CON-004`).
- So the default inherited behaviour is dishonest.
  A criterion whose every target is waived would derive `judged == 0` and publish `1.00` — full
  coverage — which is the outcome D7 exists to prevent.
- `<skipped>` is taken.
  `JUnitReporter`'s `TestCaseFactory.buildForApiTestResult` emits `Skipped` with the message "Test
  case is not included in Quality-Gate '%s'" (`JUnitReporter.java:268-279`), per `SW-017`.
  Reusing it for a waiver would make a gate exclusion and a waiver indistinguishable to the one
  consumer the feature exists to serve.
  The same method already shows what the format does have for a verdict it cannot express: a
  passing `testcase` carrying `system-out` (`:290-293`).
- A waiver is per target, not per criterion.
  A criterion with one waived target among many still has a real verdict on a smaller denominator.
  Only a criterion whose every judged target is waived has no verdict left.
- A finding has nowhere to put a justification.
  `ApiTestFinding` carries a pointer, a status, five discriminators and evidence — no free text and
  no date; every persisted column is `updatable = false` (`ARCH-012`).
  `reason` and `expires` are the one part of the waiver the evidence release did not pre-build.
- The calculation instant is already an explicit input, twice over.
  `TracingProcessor.process` hands the record timestamp to the pipeline
  (`TracingProcessor.java:88-92`), and that value is what bounds the telemetry window
  (`OpenApiCoverageCalculationServiceImpl.enrichWithOpenTelemetryData:66-81`).
  On the other side, `QualityGateReport.createdAt` is `@Column(nullable = false, updatable = false)`
  (`QualityGateReport.java:85-89`), served as the `required` `initiatedAt` on every report read
  (`QualityGateReportMapper.java:52`).
  The instant a calculation was triggered is therefore already pinned, already carried, and already
  published.
- The criteria hierarchy is prose only.
  `OpenApiCoverageCriteria` declares a label and a description (`ARCH-002`), and the containment
  relation lives in two places no code can read: the English "This is a subset of
  `RESPONSE_CODE_COVERAGE`" inside each description string, and the ASCII tree at
  `pages/_pages/quality-gate-criteria.md:84-105`.
  Nothing in code knows that `ERROR_RESPONSE_CODE_COVERAGE` sits under `RESPONSE_CODE_COVERAGE`.
- One pointer is judged by three criteria.
  `ResponseCodeCoverageCalculator:161` and `RequiredErrorFieldsCoverageCalculator:209` both build a
  finding at `toResponseEntryPointer(...)`, and `ErrorResponseCodeCoverageCalculator` extends the
  first.
  "We cannot provoke this `500`" and "the error body need not declare its required fields" are
  different claims about the same pointer.
- The report echoes its own request.
  `QualityGateReport.yml` carries `calculationRequest` as a `required`
  `$ref: './QualityGateCalculationRequest.yml'`, so anything added to the request body is served
  back on every report read without a second mapping.
- The agentic summary filters on inclusion and the bar alone
  (`agent-quality-gate-response-transformer.ts:83-85`), and `AGENTIC_SCHEMA_VERSION` is `'1'`
  (`:18`).
  No integration consumes that payload yet, so adding counts to it breaks no reader.

**Solution Approach**
Ship the inline form of one waiver document: a `waivers` array on the calculation request, resolved
against the findings the calculators emit, applied inside the calculator before the one derivation,
and published everywhere the ratio is published.

**Intake.** `waivers` joins `QualityGateCalculationRequest.yml`, so it arrives in the request body
and nowhere else, and is echoed back on every report read through `calculationRequest`.
Each entry names an API — an exact `serviceName` and `apiName`, and an `apiVersion` that is either
exact or the single wildcard `'*'` — a written `target` (`path`, `method`, and one of `responseCode` /
`parameterName` / `contentType`), an optional `criterion`, and a mandatory `reason` and `expires`.
The document is recorded on `ReportParameter` beside the lookback window and the attribute filters,
which is the entity that already holds what a calculation was told.

**Fan-out.** `ARCH-004` already sends one record per API test.
Each record carries only the waivers whose API block matches its own `ApiInformation`, and the
report's pinned `createdAt` as the calculation instant.
Filtering at dispatch is what keeps `D6` satisfied without a payload cap.

**Resolution.** A waiver matches emitted findings, by their `specPointer` and discriminators — never
by re-matching paths against the document.
There is one path-matching implementation in the system and this does not add a second.
A waiver that matches no finding fails the calculation, which is what `SW-030`'s complete
enumeration makes possible: a removed target and an unjudged one are already distinguishable.

**Narrowing.** `criterion` stays optional.
Omitted, the waiver waives the target for every criterion that judged it — the loud claim, "nothing
can exercise this target".
Named, it waives the target for that criterion and for the criteria that contain it, so a waived
child never leaves its parent failing on the same pointer.
That requires the containment relation to become machine-readable, which it is not today, so the
`OpenApiCoverageCriteria` enum gains a parent reference and `pages/_pages/quality-gate-criteria.md`
becomes a view of it rather than a second source.

**Application.** The waiver pass sits inside `AbstractOpenApiCoverageCalculator.calculate`, between
`calculateFindings` and `deriveCoverage`.
Applying it anywhere later — in `report-coordinator-api`, on the result event — would mean
recomputing the ratio there, which is the second derivation `ARCH-010` and `CON-009` exist to
forbid.

**Expiry.** A waiver's `expires` is compared against the calculation's own instant, the one the
report pins and publishes as `initiatedAt`, not against wall-clock time at some later read.
Expiry then behaves like every other input: re-deriving a historical report answers what it
answered, and `CON-001` gains a clarifying amendment — the calculation instant is an input
alongside the specification and the telemetry, not ambient state.
The alternative, enforcing expiry in the CLI before submission, is rejected: it puts the only
enforcement point outside the system that publishes the verdict, and an expiry the submitter checks
is an expiry the submitter can skip.

**Honesty.** `WAIVED` becomes a finding status — `(short) 3`, additive, already tolerated by both
decoders — and leaves both sides of the fraction, like `NOT_APPLICABLE`.
What does not come along is `CON-004`'s vacuous `1.00`: a criterion whose judged set is empty
_because_ its targets were waived is not the criterion that never had anything to judge, and must
not publish as full coverage.
Each criterion result therefore publishes two numbers the derivation already computes —
`judgedTargetCount` and `waivedTargetCount` — on the shared report component and its
findings-carrying fork, so the list read, the single read and both `202` polls agree on them.
This is the `SW-039` move: publish the counts, let the consumer compare, restate no rule.
The JUnit export keeps `skipped` for exclusion and reports a waiver the way `SW-017` already reports
a verdict the format lacks — a passing `testcase` whose `system-out` names each waived target with
its reason and expiry, plus a `waivedTargets` property on the case and a total on the `testsuites`
root for a reader that parses rather than reads.
`--agentic` gains the same counts and `AGENTIC_SCHEMA_VERSION` stays `'1'`: the keys are additive and
no integration reads that payload yet, so the first step of that version is kept for a change that
actually removes or reshapes something.
`ApiTestResultLinker.deriveApiTestStatus` drops a fully-waived included result from both sides of
`SW-016`'s share, and where that empties the share the existing empty-share rule stands — the API
test is `PASSED`, and the counts are what identify the pass as waiver-derived.
No seventh `ReportStatus` member is introduced for it.

**Acceptance Criteria**

- A calculation request carrying a waiver for one response code on one operation completes, the
  waived target leaves that criterion's fraction, and every other target of the same criterion is
  still judged.
- A waiver without `reason`, or without `expires`, is rejected when the calculation is triggered;
  the request never reaches a calculator.
- A waiver is accepted in the request body only.
  No command-line flag and no server-side store supplies one.
- A waiver naming an API that is not among the report's API tests is rejected at the trigger with a
  `400`, and no report is created.
- Each dispatched record carries only the waivers matching its own API, so a waiver for one API test
  never reaches another's calculation.
- A waiver whose `apiVersion` is `'*'` reaches every version of that API in the report and no other
  API; `'*'` in any other field is rejected at the trigger.
- A waiver matching no emitted finding fails the calculation, naming the waiver that matched
  nothing; it never silently does nothing.
- A waiver omitting `criterion` waives its target for every criterion that judged that target —
  `ERROR_RESPONSE_CODE_COVERAGE`, `RESPONSE_CODE_COVERAGE` and `REQUIRED_ERROR_FIELDS_COVERAGE` for
  a documented `500`.
- A waiver naming `criterion: ERROR_RESPONSE_CODE_COVERAGE` waives its target for that criterion and
  for `RESPONSE_CODE_COVERAGE`, and leaves `REQUIRED_ERROR_FIELDS_COVERAGE` judging the same pointer.
- A waiver whose `expires` is before the calculation's own instant does not suppress; the criterion
  fails normally and the failure names the expiry.
- Two calculations of the same specification, telemetry, waivers and instant produce identical
  results, including identical waiver verdicts.
- A criterion with some waived targets publishes a real ratio over the remaining judged targets, and
  a `waivedTargetCount` greater than zero.
- A criterion whose every judged target is waived is not published as `1.00` passing on coverage: its
  `judgedTargetCount` is `0`, its `waivedTargetCount` is greater than zero, and no published shape
  presents it as covered.
- A fully-waived included result changes the gate's share of passing included results in neither
  direction: an API test with one covered and one fully-waived included result scores as though the
  waived one were not included.
- An API test whose every included result is fully waived is `PASSED` under `SW-016`'s existing
  empty-share rule, and is distinguishable from a coverage pass by its published counts; no new
  report status is introduced.
- The list read, the single-report read and the `202` of both the trigger and the JUnit poll all
  carry `judgedTargetCount` and `waivedTargetCount` for every criterion result that has findings.
- A report persisted before this change serves neither count rather than serving a false `0`, and its
  `coverage` and `findings` are byte-identical to the pre-change response.
- A waived finding is served with its `reason` and its `expires`, and a finding that is not waived
  carries neither key.
- In the JUnit export, a criterion the gate excluded is still `skipped` with the gate-naming message;
  a criterion with waived targets is a passing `testcase` whose `system-out` names each waived target,
  its reason and its expiry, and whose `waivedTargets` property counts them.
- The `testsuites` root publishes the report's total applied waiver count as a property.
- For one report, `--agentic` and the JUnit export agree on which criteria carried waivers and on how
  many targets each waived.
- `pages/_pages/quality-gate-criteria.md` leads with satisfying a criterion by real telemetry
  evidence from the test that exercises it, and presents waivers as the second answer, for behaviour
  nothing can provoke anywhere.
- `pages/_pages/cli.md` documents the `waivers` config key, the mandatory fields, and the agentic
  payload's counts; `claude-skill.md` and `.apm/skills/snow-white/SKILL.md` describe how a consumer
  tells a waived pass from a covered one, with the generated `.claude/` copy in sync.

**Out of scope**

- **Named git-backed waiver profiles.**
  The second phase of the same decision outcome.
  The per-entry shape drafted here is the profile shape, so phase two is a resolution change rather
  than a redesign: a request names a profile instead of carrying the entries.
  The `apiVersion`/`kind`/`metadata` envelope belongs to that phase — inline delivery already knows
  which document it is reading.
- **A gate-level waiver budget.**
  A cap on how many waivers a calculation may apply would land on the gate definition, which means
  `SW-009`'s CRUD contract and `CON-003`'s immutability of the predefined gates.
  Deferred deliberately: the published counts land first, so the question "how many waivers is too
  many" can be answered from data rather than guessed, and a budget is additive once they exist.
- **A wildcard anywhere but `apiVersion`.**
  `'*'` is accepted as an `apiVersion`, where it spares a team re-editing a waiver on every release of
  an API whose gap has not closed; it is rejected on `serviceName`, `apiName`, `path`, `method` and
  every narrow discriminator, which would waive a target space rather than a target.
  The wildcard's own risk — carrying a review made against one release into the next — is bounded by
  the two controls a wildcard waiver still has to clear: a mandatory `expires`, and `CON-011`
  failing the calculation when the waiver matches nothing.
- **Waiving an undocumented-response-code criterion.**
  `SW-003`'s three criteria judge observed codes the document may not contain at all, so their
  pointer addresses a container rather than the target.
  A waiver for them is a different addressing question and is not answered here.
- **Any change to the verdict rule itself.**
  `SW-016`'s comparison and `SW-017`'s mapping keep their bar; what changes is that a fully waived
  criterion is not laundered into the share of criteria that passed.
- **Satisfying the criterion by emitting the missing telemetry** — option 5 of the decision outcome.
  Already possible, needs no code, and is documentation this story only re-orders.
- **A report-level or gate-level waiver verdict flag.**
  The counts are published; summing them is the consumer's job, exactly as applying `SW-016`'s
  comparison is.
  Adding a derived flag later is additive.

## Relations

**Realizes**

- [SYS-014](../specs/SYS-014-exemption-is-an-input-never-stored-state.md) — exemption as an
  input to the calculation rather than server state
- [SW-040](../specs/SW-040-waiver-enters-only-on-the-calculation-request.md) — the intake and
  the two mandatory fields
- [SW-041](../specs/SW-041-waiver-resolves-against-the-emitted-findings.md) — resolution
  through the findings
- [CON-011](../specs/CON-011-waiver-matching-no-finding-fails-the-calculation.md) — no waiver
  silently does nothing
- [SW-042](../specs/SW-042-criterion-narrowing-reaches-the-containing-criteria.md) — what
  `criterion` narrows, and what it still waives
- [ARCH-016](../specs/ARCH-016-criteria-containment-declared-on-the-enum.md) — the
  containment relation the narrowing needs
- [SW-043](../specs/SW-043-expiry-judged-against-the-triggering-instant.md) — expiry against
  the calculation's own instant
- [ARCH-017](../specs/ARCH-017-waivers-applied-before-the-one-derivation.md) — where the
  waiver pass sits
- [CON-012](../specs/CON-012-criterion-emptied-by-waivers-is-never-a-coverage-pass.md) — the
  honesty invariant
- [SW-044](../specs/SW-044-report-publishes-judged-and-waived-target-counts.md) — the counts
  every read publishes

**Related**

- [ARCH-004](../specs/ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — amended: each
  record carries only the waivers matching its own `ApiInformation`, plus the pinned calculation
  instant
- [ARCH-010](../specs/ARCH-010-coverage-derived-from-findings.md) — amended: the shared reduction
  also excludes `WAIVED` and yields the judged and waived counts beside the ratio
- [ARCH-012](../specs/ARCH-012-findings-on-the-event-coverage-as-cache.md) — amended: `WAIVED` is
  `(short) 3`, and a finding gains nullable `waiver_reason` and `waiver_expires_on` columns
- [SW-030](../specs/SW-030-unjudged-target-is-not-applicable.md) — amended: a waived target is not an
  inapplicable one, and the two empty-target-space cases are not the same case
- [SW-031](../specs/SW-031-findings-served-with-the-report.md) — amended: `WAIVED` joins the served
  status enum, and a waived finding carries its reason and expiry
- [SW-016](../specs/SW-016-api-test-verdict-is-gate-scoped.md) — amended: a fully waived included
  result leaves the share of included results that passed rather than counting as one; the
  empty-share rule it already states is unchanged
- [SW-017](../specs/SW-017-junit-export-mirrors-the-gate-verdict.md) — amended: `skipped` stays
  exclusion; waived targets ride `system-out` and a `waivedTargets` property
- [CON-001](../specs/CON-001-deterministic-analysis-results.md) — amended: the calculation instant is
  an input alongside the specification and the telemetry
- [CON-004](../specs/CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — amended: the
  `required == 0` vacuous `1` now has two origins, and the ratio alone never establishes a coverage
  pass
- [CON-009](../specs/CON-009-coverage-agrees-with-findings.md) — amended: the published counts join
  the ratio under the same agreement obligation
- [ARCH-002](../specs/ARCH-002-criteria-metadata-owned-by-enum.md) — amended: the enum also owns the
  containment relation
- [SW-029](../specs/SW-029-finding-identified-by-spec-pointer.md) — the pointer a waiver resolves
  through, unchanged
- [SW-039](../specs/SW-039-report-publishes-its-pinned-threshold.md) — the precedent the published
  counts follow
- [CON-010](../specs/CON-010-rest-responses-never-carry-null.md) — unchanged, and why an unwaived
  finding carries no reason or expiry key
- [SW-013](../specs/SW-013-calculation-trigger-is-all-or-nothing.md) — unchanged: a malformed waiver
  is rejected at the trigger, so no partial calculation is created
- [SW-009](../specs/SW-009-quality-gate-crud-contract.md) — untouched: the waiver budget that would
  reach it is out of scope
- [CON-003](../specs/CON-003-predefined-quality-gates-are-immutable.md) — untouched, for the same
  reason
- [SYS-006](../specs/SYS-006-criteria-based-evaluation.md) — the evaluation a waiver narrows
- [SYS-009](../specs/SYS-009-machine-consumable-gate-result.md) — the machine-consumable result the
  counts complete
- [SYS-012](../specs/SYS-012-result-consumption.md) — the consumption capability this serves
- [SW-032](../specs/SW-032-test-identity-on-the-span.md) — the evidence route that makes option 5 the
  first answer the documentation should give
