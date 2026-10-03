# The report publishes the bar it was scored against, so its consumers stop inventing one

<!-- markdownlint-disable MD036 -->

**Title**
The report publishes the bar it was scored against, so its consumers stop inventing one

**Status**: planned

**Business Value**
`snow-white calculate --agentic` exists to tell a coding agent what to fix.
Today it tells it that everything is broken: `qualityGateFailures` lists every criterion the gate
included, whether it scored 100% or 0%, and `summary.qualityGateFailureCount` counts them all.
An agent acting on that output works on criteria that already pass, and a pipeline reading the
count sees failures on a gate that passed.

The cause is not a slip in one filter — the information needed to compute a per-criterion verdict
is absent from every report read.
The number each criterion is measured against is pinned on the report
(`SW-016`), used by the JUnit export, and published nowhere.
So each consumer invents a substitute: the CLI treats inclusion as failure, and the webapp fetches
the **live** gate configuration — the number `SW-016` deliberately rejects.

**Problem / Context**
Premises checked on 2026-10-02, in this session, not assumed:

- `AgenticQualityGateResponseTransformer.transformInterface`
  (`toolkit/cli/src/action/calculate/agent-quality-gate-response-transformer.ts:53`) builds
  `qualityGateFailures` as `(api.testResults ?? []).filter(testResult =>
testResult.isIncludedInQualityGate)` and never looks at `coverage`.
  `transform` sums those arrays into `summary.qualityGateFailureCount` (`:69`).
- The unit test asserts the defect as the contract, by name: "should only include test results
  marked as included in the quality gate as failures"
  (`agent-quality-gate-response-transformer.spec.ts:97`), over a fixture whose every result carries
  `coverage: 1` (`:27`).
- `pages/_pages/cli.md:208` documents the opposite: `qualityGateFailures` "lists only the criteria
  that are part of the quality gate and did not pass".
- `.apm/skills/snow-white/SKILL.md:55` — the APM package consumers install, of which
  `.claude/skills/snow-white/SKILL.md` is the generated copy — states the equivalence the output is
  meant to hold: `qualityGateFailures` is the `--agentic` counterpart of the JUnit `<failure>`
  elements.
- A criterion's verdict is `coverage >= minCoveragePercentage / 100`, applied identically in the
  two places that own it: `ApiTestResultLinker:129-160` (which also reuses the same number as the
  bar on the _share_ of criteria clearing it) and `JUnitReporter:283`.
- The threshold is pinned on the report, not read live: `QualityGateReport.minCoveragePercentage`
  is `@Column(nullable = false, updatable = false)` with a domain default of `100`
  (`QualityGateReport.java:59-64`), `NOT NULL` since
  `V2026_09_18__quality_gate_report_min_coverage_percentage.sql:12` and `CHECK (… BETWEEN 80 AND
100)` since `V2026_09_25__quality_gate_report_min_coverage_percentage_bounds.sql:11`.
  Every persisted report therefore has a value, in `[80, 100]`.
- No report read publishes it.
  `QualityGateReport.yml` and `QualityGateReportWithFindings.yml` carry `calculationId`,
  `qualityGateConfigName`, `status`, `calculationRequest`, `interfaces` and `initiatedAt`, and
  nothing else; `ApiTestResult.yml` carries `coverage`, `additionalInformation` and
  `isIncludedInQualityGate`, with no verdict.
  The JUnit export does publish it — as a `<property>` on the suite (`Properties.java:17`,
  `JUnitReporter:67-96`) — which is why that artifact can say "failure" and the JSON cannot.
- The webapp obtains it from the live configuration, inside the same thunk that reads the report:
  `getEntity` awaits `qualityGateApi.getQualityGateByName(...)` (`quality-gate.reducer.ts:92-96`)
  and the detail view draws the threshold marker from it (`quality-gate-detail.tsx:135` →
  `coverage-progress-bar.tsx:34-40`).
  A gate edited after the calculation marks the wrong bar on a historical report; a gate since
  deleted or renamed rejects the thunk, so the report detail fails to load at all even though the
  report is intact.
- The CLI's exit code does not depend on the failure set: `11` comes from `report.status` alone
  (`poll-calculation-result.ts:71-73`, `calculate-quality-gates.ts:88`).
  So the defect misreports, and never mis-exits — which is why it survived to this point.

**Solution Approach**
Publish the pinned number on the report reads, then let each consumer derive the verdict the one
way `SW-016` defines it.

`minCoveragePercentage` joins the shared report component — `QualityGateReport.yml` and its
findings-carrying fork — so the list read, the single-report read and both `202` polls all carry it.
It is declared `required`, because the column is `NOT NULL` with a check constraint and every
persisted report has a value; a consumer never has to default it, and so never has to guess one.
MapStruct maps it by name from the entity, so no mapping is hand-written for it.

The CLI then filters `qualityGateFailures` to the included criteria whose `coverage` is below that
bar, which is exactly the JUnit `<failure>` set the skill package already promises.
Its mis-asserting unit test is rewritten around the corrected rule rather than deleted, so the
inclusion half of the filter stays covered.

The webapp's threshold marker moves to the report's pinned value.
The live-configuration read stays for the gate's name, description and criteria, which are not
pinned and are presented as the gate's current definition — but the bar a historical report is drawn
against comes from the report.

A per-criterion verdict field — a `passed` boolean on `ApiTestResult.yml` — is deliberately **not**
what is published.
The comparison is one `>=` that `SW-016` already pins; a boolean would hide the margin a reader
needs ("0.9 against a bar of 0.8"), would duplicate the rule into a second place that can drift from
the first, and would not serve the webapp's marker, which needs the number itself.
The number is also the more additive change: it is the same value the JUnit export already publishes
to the same consumers.

**Acceptance Criteria**

- Every report read carries the report's pinned `minCoveragePercentage`: the paginated list read,
  the single-report read, and the `202` of both the calculation trigger and the JUnit poll.
- The value served is the one pinned on the report at trigger time, and is unaffected by a later
  edit to the gate it was taken from.
- `--agentic`'s `qualityGateFailures` contains exactly the included criteria whose coverage is below
  that bar, and `summary.qualityGateFailureCount` is their total across all interfaces.
- A criterion that clears the bar without reaching full coverage is not reported as a failure — the
  same verdict the JUnit export gives it, where it passes and explains the gap in `system-out`.
- A criterion the gate excluded is never reported as a failure, whatever its coverage.
- For one report, the `--agentic` failure set and the `<failure>` elements of the same report's
  JUnit export name the same criteria.
- A report whose every included criterion clears the bar emits `qualityGateFailures: []` and
  `qualityGateFailureCount: 0`.
- `testResults` still lists every evaluated criterion, included or not, unchanged.
- The webapp's coverage-bar threshold marker is drawn from the report's pinned value, and a report
  whose gate has since been deleted still loads its detail view.
- `pages/_pages/cli.md` and `.apm/skills/snow-white/SKILL.md` describe the failure set as the
  criteria below the report's pinned bar, and the generated `.claude/skills/snow-white/SKILL.md`
  copy matches.

**Out of scope**

- A per-criterion verdict field in the API.
  Excluded with its reason in the Solution Approach, and
  reversible: adding one later is additive.
- Changing the verdict rule itself, at either altitude.
  `SW-016` and `SW-017` are unchanged — this
  story makes the existing rule computable by a consumer, and nothing about it is restated in a new
  place.
- `AGENTIC_SCHEMA_VERSION`, which stays `1`.
  No field of the agentic document is added, removed or
  retyped; the values in `qualityGateFailures` become correct.
  A consumer that branches on the
  schema version is not affected by a correction it could not previously work around.
- The CLI's exit codes.
  They derive from the report status, not from the failure set, and the
  mapping is unchanged.
- The webapp's live read of the gate's name, description and criteria set.
  Those are not pinned on
  the report, and showing the gate's current definition beside a historical report is the existing,
  deliberate behaviour.
- Bumping `info.version` in `v1-report-api.yml` and `v1-quality-gate-api.yml`.
  Both have stood at
  `1.0.0` across every additive change to date; treating this one differently would be an
  inconsistency, and the repo-wide versioning decision is tracked separately.
- Re-deriving the API-test or report status client-side.
  Both are already published, and `SW-015`
  owns how they aggregate.

## Relations

**Realizes**

- [SW-039](../specs/SW-039-report-publishes-its-pinned-threshold.md) — the pinned
  threshold on the report reads

**Related**

- [SW-016](../specs/SW-016-api-test-verdict-is-gate-scoped.md) — the verdict rule this makes
  computable, and the pinning it relies on
- [SW-017](../specs/SW-017-junit-export-mirrors-the-gate-verdict.md) — the export whose
  `<failure>` set the agentic output is meant to match
- [SYS-009](../specs/SYS-009-machine-consumable-gate-result.md) — the machine-consumable result
  this corrects
- [SYS-012](../specs/SYS-012-result-consumption.md) — the result-consumption capability the new
  property serves
- [SW-014](../specs/SW-014-in-progress-report-answers-accepted.md) — the `202` poll shapes that
  gain the property
- [SW-031](../specs/SW-031-findings-served-with-the-report.md) — the forked report component the
  property is added to alongside the shared one
- [CON-004](../specs/CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the bounded
  ratio the comparison is made against
- [CON-010](../specs/CON-010-rest-responses-never-carry-null.md) — why a `required` property is
  safe here: the value is never null, so it is never omitted
