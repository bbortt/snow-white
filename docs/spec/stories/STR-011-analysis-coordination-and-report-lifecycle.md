# Documented analysis coordination and report lifecycle for `report-coordinator-api`

<!-- markdownlint-disable MD036 -->

**Title**
Documented analysis coordination and report lifecycle for `report-coordinator-api`

**Status**: done

**Business Value**
`SYS-010`, `SYS-011`, and `SYS-009` name analysis triggering, bounded-time resolution to a
terminal state, and a machine-consumable gate result as system capabilities — but nothing in the
spec corpus pins down the component that actually owns all three.
`report-coordinator-api` is that component: it is the entry point a CI pipeline calls, the owner
of the report a caller polls, and the producer of the JUnit XML a build server consumes.
Its most consequential behaviours are exactly the ones a caller cannot see from the OpenAPI
contract: that a trigger with one unindexed API creates no report at all, that a report still
running answers `202` rather than `200`, that a report can pass its quality gate while the JUnit
export marks the same criteria as failures, and that a stale report is timed out rather than
deleted.
Pinning these down gives the CLI, the `api-gateway` UI, and future changes something to check
against instead of only the running code.

**Problem / Context**
`microservices/report-coordinator-api` coordinates a quality-gate analysis end to end: it
validates the requested APIs against `api-index-api`, resolves the gate against
`quality-gate-api`, persists a report, fans calculation requests out over Kafka, folds the
asynchronous coverage responses back in, derives a verdict, and serves the result as JSON or
JUnit XML.
A source read of the module (`QualityGateResource`, `ReportResource`, `HousekeepingResource`,
`ReportService`, `ApiIndexService`, `QualityGateService`, `ApiTestResultLinker`,
`QualityGateStatusCalculator`, `QualityGateCalculationRequestDispatcher`, `OpenApiResultListener`,
`QualityGateReportHousekeeper`, `JUnitReporter`, and the `ReportStatus` model, against the
existing unit, integration, and `ReportCoordinatorApiAppTest` black-box suites) found no code
anchored to any spec in the corpus — this module's behaviour is currently undocumented outside
the code.

Several behaviours stood out as consequential enough to need a spec of their own:

- A trigger is **all-or-nothing**: `ApiIndexService` validates every requested API in parallel and
  `QualityGateResource` aggregates _all_ failures into one `400` body, so a request naming four
  APIs of which one is unindexed creates no report and dispatches nothing.
- `GET` on a report that is still `IN_PROGRESS` answers `202` **with the partial report body**,
  not `200` and not an error — the polling contract a CI client depends on.
- The report verdict and the JUnit export disagree **by design**: `ApiTestResultLinker` passes an
  API test when the share of gate-included criteria meeting the gate threshold itself meets that
  threshold, while `JUnitReporter` marks any criterion below _full_ coverage as a `failure`.
- `QualityGateReportHousekeeper` **times out** stale reports (it updates their status to
  `TIMED_OUT`); despite the job's `Deleting …` debug log, nothing is removed.
- `OpenApiResultListener` rethrows to let Kafka retry, but on the **last** delivery attempt it
  first folds the failure into the report as `FINISHED_EXCEPTIONALLY`, so a permanently failing
  response resolves the report instead of leaving it to the housekeeping timeout.

**Solution Approach**
Add seven software specs for the module's observable behaviour — the trigger contract, the
report-retrieval polling contract, report-status aggregation, the API-test verdict rule, the
JUnit export mapping, the stale-report timeout, and the last-delivery-attempt absorption — and
three architecture specs for the structural decisions behind them: the per-API-test Kafka fan-out
keyed by calculation id, dispatch deferred until after the report transaction commits, and report
status persisted as a stable numeric code with a tolerant decode.
No code changes — this documents the existing, already-tested behaviour and anchors it.

**Acceptance Criteria**

- A spec states the calculation-trigger contract: `202` with a `Location` header pointing at the
  **public** `api-gateway` URL, `400` carrying every API-validation failure joined into one
  message, `404` for an unknown quality gate — and that a request with any invalid API creates no
  report and dispatches no calculation request.
- A spec states that retrieving a report answers `404` when unknown, `202` with the partial report
  body while it is still in progress, and `200` once it has reached a terminal status.
- A spec states the report-status aggregation rule: no API tests means `NOT_STARTED`, otherwise
  the first match of `IN_PROGRESS`, `FAILED`, `FINISHED_EXCEPTIONALLY`, else `PASSED`; and that a
  report already in `FAILED`, `FINISHED_EXCEPTIONALLY`, or `TIMED_OUT` is never recomputed.
- A spec states that a criterion outside the gate is persisted but flagged not-included, that only
  included criteria decide the API test's verdict, that the gate's `minCoveragePercentage` acts
  both as the per-criterion bar and as the bar on the share of criteria clearing it, and that an
  API test with no included criteria passes.
- A spec states the JUnit XML mapping: report to `testsuites`, API test to `testsuite`, criterion
  result to `testcase`; excluded criteria emitted as `skipped` rather than omitted; any included
  criterion below full coverage emitted as a `failure` — deliberately stricter than the gate
  verdict — with suites and cases ordered by name.
- A spec states that a report left in `NOT_STARTED` or `IN_PROGRESS` past the configured cutoff is
  transitioned to `TIMED_OUT` and retained, not deleted, and that the cutoff is evaluated against
  an injected `Clock`.
- A spec states that a coverage response whose processing fails is rethrown for Kafka redelivery,
  and that on the final delivery attempt the failure is recorded on the report as
  `FINISHED_EXCEPTIONALLY` before the rethrow; and that a response for an API the report does not
  contain is logged and dropped rather than retried.
- A spec states that a calculation request is dispatched as one Kafka message **per API test**,
  all keyed by the calculation id, with the OpenTelemetry trace context injected into the record
  headers.
- A spec states that calculation requests are dispatched only after the report's transaction
  commits, and explains why.
- A spec states that report status is persisted as a stable numeric code rather than an enum name
  or ordinal, and that an unrecognised code decodes to `NOT_STARTED`.

**Out of scope**

- Coverage _calculation_ itself — that is `openapi-coverage-stream`'s concern (`STR-007`,
  `STR-008`); this module only requests it and folds the results back in.
- Quality-gate _configuration_ — owned by `quality-gate-api` (`STR-009`); this module only
  resolves a gate by name and reads its criteria and threshold.
- Anchoring the system-level specs (`SYS-009`, `SYS-010`, `SYS-011`) this module realizes.
  Those remain unanchored here, consistent with the `quality-gate-api` and
  `openapi-coverage-stream` retraces, which likewise related to their `SYS` parents without
  claiming them; closing the `SYS`/`STK`/`NF` coverage gaps is its own story.
- The generic `{code, message}` error envelope `ApiExceptionHandler` normalises Spring MVC's own
  exceptions into — a convention shared by every microservice, explicitly left cross-cutting by
  `STR-009` and still unclaimed.

## Relations

**Realizes**

- [SW-013](../specs/SW-013-calculation-trigger-is-all-or-nothing.md) — the trigger
  contract and its all-or-nothing validation
- [SW-014](../specs/SW-014-in-progress-report-answers-accepted.md) — the polling
  contract
- [SW-015](../specs/SW-015-report-status-aggregates-with-sticky-terminal.md) — report
  status aggregation and terminal stickiness
- [SW-016](../specs/SW-016-api-test-verdict-is-gate-scoped.md) — the API-test verdict
  rule
- [SW-017](../specs/SW-017-junit-export-mirrors-the-gate-verdict.md) — the JUnit
  XML mapping
- [SW-018](../specs/SW-018-stale-reports-time-out-not-deleted.md) — the stale-report
  timeout
- [SW-019](../specs/SW-019-final-delivery-attempt-absorbs-failure.md) — last-attempt
  absorption of a failing coverage response
- [ARCH-004](../specs/ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — the
  Kafka fan-out shape
- [ARCH-005](../specs/ARCH-005-dispatch-after-transaction-commit.md) — dispatch
  deferred past commit
- [ARCH-006](../specs/ARCH-006-report-status-persisted-as-stable-code.md) — the
  persisted status encoding

**Related**

- [SYS-010](../specs/SYS-010-analysis-triggering.md) — the triggering capability this module's
  REST surface implements
- [SYS-011](../specs/SYS-011-bounded-time-resolution.md) — the bounded-time guarantee the
  housekeeping timeout upholds
- [SYS-009](../specs/SYS-009-machine-consumable-gate-result.md) — the machine-consumable result
  the JUnit export delivers
- [SYS-012](../specs/SYS-012-result-consumption.md) — the consumption surface the report
  endpoints serve
- [SYS-008](../specs/SYS-008-quality-gate-definitions.md) — the gate definitions this module
  resolves but does not own
- [CON-002](../specs/CON-002-tolerate-dependency-outages.md) — the outage tolerance this
  module's retrying clients and Kafka redelivery contribute to
- [NF-005](../specs/NF-005-clear-failure-feedback.md) — the failure feedback the aggregated
  `400` body provides
- [STR-009](STR-009-quality-gate-configuration-and-criteria-management-api.md) — the sibling
  retrace covering the gate configuration this module consumes
- [STR-006](STR-006-result-consumption.md) — the story covering result consumption, which these
  endpoints serve
