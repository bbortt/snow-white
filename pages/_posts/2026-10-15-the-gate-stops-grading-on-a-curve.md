---
title: 'Release 2.0.0: the gate stops grading on a curve'
excerpt: >
  A quality gate used to pass when enough of its criteria passed. Six criteria
  at 80% tolerated one outright failure, and `basic-coverage` had quietly
  stopped measuring whether your tests reached your paths at all. 2.0.0 makes
  the bar a floor under every criterion, puts path coverage back, and publishes
  the threshold each report was actually scored against.
tags:
  - release
  - coverage
  - quality-gate
  - cli
---

The last few releases were about making coverage legible: findings underneath every ratio
(1.12.0), a drilldown and an exportable report (1.13.0).
Making it legible is also how you find out that the number on top of it was wrong.

2.0.0 is a major release because three of those discoveries change verdicts.
A pipeline that was green can go red on this upgrade without anybody touching a gate.
That is the point — the builds it turns red were passing on a technicality — but it is a thing to
know before you upgrade, so it leads.

## The bar is a floor now, not a quota

A gate names a set of criteria and one threshold.
Until this release, that threshold was applied **twice**: once to each criterion, and then again to
the _share_ of criteria that cleared it.
Six criteria at an 80% bar meant five passing criteria scored 83%, which cleared 80%, which passed
the API test — with one criterion sitting at zero.

The threshold is now a floor under every included criterion.
One criterion below it fails the API test, however well the others scored, and the reports that
roll up from that API test
([`SW-015`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-015-report-status-aggregates-with-sticky-terminal.md))
fail with it.
The comparison is exact: no rounding step stands between a criterion's coverage and the bar, so a
custom gate at 85 means 85.

Of the four predefined gates only `basic-coverage` changes behaviour.
`full-feature` at 100% and `minimal` at one criterion already demanded every criterion they select,
and `dry-run` selects none.
This change rewrites no gate configuration of its own and recalculates no stored report — the new
rule applies to API tests scored from here on
([`SW-016`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-016-api-test-verdict-is-gate-scoped.md)).

## `basic-coverage` measures path coverage again

`PATH_COVERAGE` was dropped from `basic-coverage` back in
[`STR-010`](https://github.com/bbortt/snow-white/blob/main/docs/spec/stories/STR-010-drop-redundant-parent-implied-criteria-from-predefined-gates.md)
because `HTTP_METHOD_COVERAGE` was thought to imply it.
That implication only holds when both ratios reach 100%, and this gate's bar is 80%.

Below full coverage the two come apart, because a path counts once in path coverage and once per
operation in method coverage.
One path with twenty called operations, plus five single-operation paths nothing ever called, is
**80% method coverage and 17% path coverage** — and it passed `basic-coverage` with five sixths of
the API unreached.

So the criterion is back, and the gate goes from five criteria to six.
Note what that means together with the section above: under the old quota rule, restoring it would
not have fixed the example, because six criteria at 80% would have absorbed the failure.
It takes both changes.

`initPredefinedQualityGates` upserts predefined gates by name on every startup, so an upgraded
instance gains the criterion without anyone asking for it.
[`CON-003`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/CON-003-predefined-quality-gates-are-immutable.md)
forbids users from mutating a predefined gate precisely so that a pinned name keeps meaning one
thing; changing one across a release owes you this warning in return.

If your pipeline was green on exactly four of five criteria and has paths it never reaches, it now
scores four of six — and fails on the floor regardless.

## Containment says which way the implication runs

The bad `PATH_COVERAGE` edge existed because the criteria containment tree predated any definition
of what containment meant.
It has one now, and the edge did not survive it: a path item and the operations within it are
different targets at different spec pointers.

`getContainedBy()` is joined by `getContainmentForm()`, and a declared container always carries a
form:

- **`SUBSET`** — the same check over a strictly smaller set of targets.
  Full `RESPONSE_CODE_COVERAGE` implies full `ERROR_RESPONSE_CODE_COVERAGE`.
- **`STRENGTH`** — a strictly stronger check over the same set of targets.
  Here the implication runs the _other_ way: full `OPERATION_SUCCESS_COVERAGE` implies full
  `HTTP_METHOD_COVERAGE`, while full method coverage says only that every operation was called.

If you read containment as "is a subset of", read the form before drawing a coverage implication
from an edge
([`ARCH-016`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/ARCH-016-criteria-containment-declared-on-the-enum.md),
[`SW-011`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-011-four-predefined-gates-fixed-composition.md)).
The practical payoff is that no future containment sweep can drop path coverage out of a gate again.

## The report publishes the bar it was scored against

Every report has always been scored against a threshold pinned at trigger time, which a later edit
to the gate does not change.
It just never published it — so every consumer invented a substitute, and both got it wrong.

`snow-white calculate --agentic` exists to tell a coding agent what to fix.
It was listing **every** criterion the gate included as a failure, whether it scored 100% or 0%,
and counting them all in `summary.qualityGateFailureCount`.
An agent reading that output went off to fix criteria that already passed.
Meanwhile the webapp drew its threshold marker from the _live_ gate configuration — the number the
pinned one deliberately rejects — so an edited gate silently redrew old reports.

Report reads now carry `minCoveragePercentage`:

```json
{
  "calculationId": "...",
  "qualityGateConfigName": "basic-coverage",
  "minCoveragePercentage": 80,
  "status": "FAILED"
}
```

The CLI reports only the criteria below that bar, and **fails closed** if a report arrives without
one rather than treating a missing threshold as no bar at all.
The UI marks the coverage bars at the report's own threshold, with no second request for the gate
([`STR-022`](https://github.com/bbortt/snow-white/blob/main/docs/spec/stories/STR-022-report-publishes-the-bar-it-was-scored-against.md)).

## Waivers: the intake half

A team that wants the full criteria set has had exactly one lever when a single check is
impractical — drop that criterion from the gate, for the whole API.
The motivating case was a documented `500` reachable only from a component test with the downstream
mocked: `ERROR_RESPONSE_CODE_COVERAGE` fails on that one response code, so you either weaken the
gate across every endpoint or accept a permanently red build.

`QualityGateCalculationRequest` gains an optional `waivers` array, which is the only way a waiver
reaches a calculation — no query parameter, no header, no gate property, no separate endpoint:

```json
{
  "includeApis": [
    { "serviceName": "petstore", "apiName": "pets", "apiVersion": "1.0.0" }
  ],
  "waivers": [
    {
      "serviceName": "petstore",
      "apiName": "pets",
      "apiVersion": "*",
      "path": "/pets/{petId}",
      "method": "GET",
      "responseCode": "500",
      "reason": "reachable only with the inventory service mocked",
      "expires": "2026-12-31"
    }
  ]
}
```

Entries are validated at the trigger, before any calculation exists: a missing or blank `reason`,
no `expires`, more than one target discriminator, or an API that is not among the report's API tests
is a `400` and **no report is created**.
An `apiVersion` of `*` matches on service and API name alone and reaches every version of that API
in the report.
The accepted document is recorded beside the lookback window and attribute filters that calculation
was told, and served back on every report read through `calculationRequest`
([`SW-040`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-040-waiver-enters-only-on-the-calculation-request.md)).

**Be clear about what this does not do yet.**
In 2.0.0 a waiver is accepted, validated, persisted and echoed back.
Nothing resolves it against findings, no finding becomes `WAIVED`, and no verdict changes.
Resolution
([`SW-041`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-041-waiver-resolves-against-the-emitted-findings.md)),
criterion narrowing
([`SW-042`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-042-criterion-narrowing-reaches-the-containing-criteria.md))
the rule that a criterion emptied by waivers is never a coverage pass
([`CON-012`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/CON-012-criterion-emptied-by-waivers-is-never-a-coverage-pass.md))
and keeping an exemption an input rather than stored state
([`SYS-014`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SYS-014-exemption-is-an-input-never-stored-state.md))
are specified and planned, not shipped.

The whole increment is shaped around one requirement: a report that passes with waivers must never
read like one that passed on coverage.
That is worth getting right more than it is worth getting early
([#2009](https://github.com/bbortt/snow-white/issues/2009)).

## Sorting answers 400 instead of guessing

The report, quality-gate and API-index list reads now answer `400` for a `sort` value they cannot
honour, where they previously answered `500` or ignored it and returned a differently-ordered page
than you asked for.

Two documented names were never the ones the reads accepted:

| Read      | Was               | Use           |
| --------- | ----------------- | ------------- |
| Reports   | `createdAt`       | `initiatedAt` |
| API index | `otelServiceName` | `serviceName` |

Reports can no longer be sorted by `status` at all.
It is published as a name but stored as a stable code whose order those names do not imply, so the
column lost its sort affordance in the UI rather than keeping one that lied
([`STR-021`](https://github.com/bbortt/snow-white/blob/main/docs/spec/stories/STR-021-list-reads-honour-their-documented-sort-contract.md)).

## Also in this release

- **The coverage stream reports its own health**, and Kubernetes restarts one whose Kafka Streams
  topology died instead of leaving a live pod that processes nothing.
  `docker-compose` gained health checks for all microservices.
- **The quality-gate summary counts the traces behind the coverage** — how many distinct traces
  evidenced a finding across the whole calculation, derived from the findings' evidence, so it
  appears on a completed report that carries them rather than reading as zero everywhere else.
- **The api-gateway swallows only a missing gate**, not every fault that looks like one.
- A new **JUnit Jupiter extension** in the toolkit implements the
  [test OTel semantic conventions](https://opentelemetry.io/docs/specs/semconv/registry/attributes/test/)
  and sets the `test.case.name` baggage for you, instead of each test plumbing it by hand as the
  1.12.0 examples do.
  It is built for the internal application and end-to-end tests; use it outside them with caution.
- Performance: batched lazy and eager collection loads, and indexed API-test lookups in the
  report-coordinator.
- Internals: `opentelemetry-spring-boot-starter` replaces `spring-boot-starter-opentelemetry`
  ([#2082](https://github.com/bbortt/snow-white/issues/2082)), and the otel-event-filter-stream
  registers its Kafka Streams native hints.

## Upgrading

1. **Expect `basic-coverage` to gain path coverage on first startup.**
   If your tests do not reach every path, this is the criterion that will say so.
2. **Expect one weak criterion to fail the whole API test.** Find the criteria that were riding on
   the old quota before the upgrade does it for you.
3. **Fix any `sort=createdAt` or `sort=otelServiceName` callers**, and drop `sort=status` on
   reports.
   They now return `400` instead of quietly doing something else.
4. **Regenerate your API clients.** The error DTO on the affected reads was renamed when `400`
   joined the documented responses.
5. If you consume containment programmatically, **read `getContainmentForm()`** before inferring
   anything from an edge.

The full list is in the
[changelog](https://github.com/bbortt/snow-white/blob/main/CHANGELOG.md#200-2026-10-15).
