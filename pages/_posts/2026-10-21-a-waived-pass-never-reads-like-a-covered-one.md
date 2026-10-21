---
title: 'Release 2.2.0: a waived pass never reads like a covered one'
excerpt: >
  2.0.0 accepted waivers and deliberately changed nothing — it said so in
  writing. 2.1.0 made them resolve against the findings a calculation emitted,
  and 2.2.0 makes a waived pass impossible to mistake for a covered one. The
  increment behind #2009 is closed.
tags:
  - release
  - quality-gate
  - waivers
  - cli
---

The 2.0.0 post had a section called "Waivers: the intake half", and it was blunt about what the
release did not do.
A waiver was accepted, validated, persisted and echoed back; nothing resolved it against findings,
no finding became `WAIVED`, and no verdict changed.
The reason given was that a report which passes with waivers must never read like one that passed
on coverage, and that this was worth getting right more than it was worth getting early.

Two releases later it is right.
2.1.0 made waivers resolve; 2.2.0 makes the result legible and documents the whole feature.
This post covers both, because 2.1.0 went out without an announcement of its own and the engine is
not much use without the half that lets you read it
([#2009](https://github.com/bbortt/snow-white/issues/2009)).

## What a waiver is

A waiver declares that one criterion does not apply to one target of one API, with a reason, until
a given date.
Everything else stays enforced, including other targets of the same criterion.

It is an input to a single calculation, not configuration.
It travels in the calculation request, it is echoed back on the report, and Snow-White stores no
waivers of its own
([`SYS-014`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SYS-014-exemption-is-an-input-never-stored-state.md),
[`SW-040`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-040-waiver-enters-only-on-the-calculation-request.md)).
That is what makes the motivating case workable: a documented `500` reachable only from a component
test with the downstream mocked no longer forces you to choose between dropping
`ERROR_RESPONSE_CODE_COVERAGE` for every endpoint and living with a permanently red build.

## Resolution runs against findings, not paths

1.12.0 put findings underneath every coverage ratio — the exact spec locations checked, and whether
each was covered.
Waivers resolve against exactly those findings, rather than matching paths against the
specification a second time
([`SW-041`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-041-waiver-resolves-against-the-emitted-findings.md)).

There is one matching step in the system, so a waiver cannot agree with the specification and
disagree with the thing that was actually judged.
A waived finding keeps its place in the report with the status `WAIVED`, and carries
`waiverReason` and `waiverExpiresOn` — the justification travels with the evidence, not in a
changelog somebody has to go and find.

## A waived target leaves the fraction

This is the part worth reading twice if anything you own parses a report.

A waived target counts on neither side of the coverage ratio.
A criterion with ten targets, two of them waived, reports its coverage over the remaining eight.
Every criterion result publishes both numbers on every read
([`SW-044`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-044-report-publishes-judged-and-waived-target-counts.md)):

```json
{
  "id": "ERROR_RESPONSE_CODE_COVERAGE",
  "coverage": 1.0,
  "judgedTargetCount": 0,
  "waivedTargetCount": 3
}
```

That is not a pass on coverage, and it is deliberately not reported as one
([`CON-012`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/CON-012-criterion-emptied-by-waivers-is-never-a-coverage-pass.md)).
A criterion whose every target was waived still reports `1.00`, because the ratio over an empty set
has no other honest answer — so the counts beside it are how you tell the two apart.
`judgedTargetCount: 0` with a non-zero `waivedTargetCount` means waivers emptied the criterion.

A fully waived criterion neither raises nor lowers its API's verdict, which means an API whose
included criteria are all fully waived is `PASSED`.
**A `coverage` of `1.00` and a `PASSED` status are therefore never proof on their own that anything
was exercised.**
If you gate on these reads, check `judgedTargetCount` before you believe them.

Every report shape carries the distinction, not just the JSON:

- **JSON and `--agentic` output**: the counts above, on every criterion result.
- **JUnit export**: a criterion with waived targets is a passing `testcase` with
  `judgedTargetCount`, `waivedTargetCount` and `waivedTargets` properties, and its `system-out`
  lists each waived target with its expiry and reason.
  The `testsuites` root carries the document total.
  `skipped` still means only that the gate excluded the criterion.
- **Web UI**: waived findings appear in the drilldown with their reason and expiry, and a criterion
  emptied by waivers is marked as fully waived rather than drawn as a clean pass.

## Which criteria a waiver reaches

One target is usually judged by several criteria.
A documented `500` is judged by `RESPONSE_CODE_COVERAGE`, `ERROR_RESPONSE_CODE_COVERAGE` and
`REQUIRED_ERROR_FIELDS_COVERAGE` — three different claims about the same response.
So "waive this response" is ambiguous, and the optional `criterion` field is how you say which
claim you are giving up
([`SW-042`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-042-criterion-narrowing-reaches-the-containing-criteria.md)).

- **Without `criterion`**, the target is waived for every criterion that judged it.
  This is the strong claim: nothing can exercise this target.
- **With `criterion`**, the target is waived for that criterion _and for every criterion containing
  it_.
  `criterion: ERROR_RESPONSE_CODE_COVERAGE` also waives the target for `RESPONSE_CODE_COVERAGE`, so
  a waived child never leaves its parent failing on the same target.
  `REQUIRED_ERROR_FIELDS_COVERAGE` still judges that response.

Naming a `criterion` the chosen gate does not include is accepted rather than rejected, so one
waiver list in a repository survives a change of gate.

## Expiry is judged when the calculation was triggered

`expires` is compared with the report's `initiatedAt`, not with the time the report is read
([`SW-043`](https://github.com/bbortt/snow-white/blob/main/docs/spec/specs/SW-043-expiry-judged-against-the-triggering-instant.md)).
Recalculating an old report therefore gives the same waiver verdicts it gave the first time, which
is the only version of this that makes a stored report mean one thing.

An expired waiver is not silently dropped.
Its target is judged normally — covered or uncovered on the evidence — and the criterion's result
names the waiver and the date it expired.
A build that goes red because an exemption ran out says so, instead of looking like a sudden
coverage regression.

## A waiver that matches nothing fails the calculation

Every waiver has to match at least one target that a criterion actually judged.
Otherwise that API's calculation fails and the error names the waiver.

The alternative was a waiver that quietly stops doing anything, and there are only two ways to get
one: it was written wrong — `/orders/42` where the specification says `/orders/{orderId}` — or the
target no longer exists.
Both are things the author needs to be told.
Note that a waiver whose `apiVersion` is `'*'` has to match in every version it reaches, and it is
the only kind whose target can disappear without anyone editing it.

## From the CLI

Waivers come from the `waivers` key of the config file, and from nowhere else.
No command-line flag sets them, so adding an exemption is always a change someone can review in a
diff:

```json
{
  "url": "http://<snow-white-host>",
  "qualityGate": "full-feature",
  "apiInformation": [
    {
      "serviceName": "order-service",
      "apiName": "orders-api",
      "apiVersion": "1.0.0"
    }
  ],
  "waivers": [
    {
      "serviceName": "order-service",
      "apiName": "orders-api",
      "apiVersion": "*",
      "path": "/orders/{orderId}",
      "method": "GET",
      "responseCode": "500",
      "criterion": "ERROR_RESPONSE_CODE_COVERAGE",
      "reason": "Only reachable by failing the database mid-request",
      "expires": "2027-04-30"
    }
  ]
}
```

`serviceName`, `apiName`, `apiVersion`, `path`, `method`, `reason` and `expires` are required; at
most one of `responseCode`, `parameterName` and `contentType` may be set.

The CLI checks what it can before sending anything and exits `3` on a missing field, an impossible
date such as `2026-02-30`, or an unknown key.
That last one matters more than it looks: a misspelt `responseCod` would otherwise be dropped, and
the waiver would silently apply to the whole operation instead of one response code.
Checks that need the calculation itself stay on the server, which answers `400` — the CLI exits
`10` — for an API that is not part of the calculation, a `'*'` outside `apiVersion`, or a criterion
that does not exist.
A waiver matching no target fails the gate with exit code `11`.

Unless `--agentic` is set, the CLI prints how many waivers it sent.
The full field reference is on the
[Quality Gate Criteria]({{ '/quality-gate-criteria/' | relative_url }}) page, and the config file on
the [CLI Reference]({{ '/cli/#calculate' | relative_url }}).

## Also in these two releases

- **A compatibility matrix** on the [Deployment]({{ '/deployment/' | relative_url }}) page, naming
  the PostgreSQL, Kafka and `cp-kafka` image versions Snow-White is verified against.
  Pull requests verify the latest of each; the full matrix runs weekly.
- **The raw request view shows the whole calculation request**, so the waivers a report was
  calculated with are visible beside the lookback window and attribute filters it was given.
- The report-coordinator **stops documenting a `400` that housekeeping cannot return**.
- The api-gateway dev server **survives BrowserSync's port scan** instead of exiting during it.
- The devcontainer **keeps its `node_modules` off the host**.
- Waivers are documented on the Quality Gate Criteria and CLI pages, and in the Claude Code skill.

## Upgrading

1. **Nothing to do if you do not use waivers.** There is no stored waiver state, no migration, and
   no change to any gate's composition or threshold.
2. **If anything you own parses a report, check `judgedTargetCount`** before treating `coverage`
   `1.00` or a `PASSED` status as evidence of coverage.
   This is the one way 2.1.0 can change what a consumer should conclude from a report it already
   knows how to read.
3. **Regenerate your API clients if you deserialize reports.** The waiver request object is
   unchanged since 2.0.0, but the response schemas are not: findings gained the `WAIVED` status with
   `waiverReason` and `waiverExpiresOn`, and criterion results gained the two counts.
   Unrelated to waivers, `POST /api/v1/housekeeping` stopped documenting a `400` it could never
   return, which renames the generated error type for that operation.
4. **Expect reports calculated before waivers existed to lack the counts.** They are absent, not
   zero — deliberately, because a defaulted `judgedTargetCount: 0` would assert that the criterion
   judged nothing.

The full lists are in the changelog, for
[2.1.0](https://github.com/bbortt/snow-white/blob/main/CHANGELOG.md#210-2026-10-18) and
[2.2.0](https://github.com/bbortt/snow-white/blob/main/CHANGELOG.md#220-2026-10-21).
