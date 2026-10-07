---
title: 'Release 1.12.0: coverage that proves what it measured'
excerpt: >
  Every coverage ratio Snow-White reports is now backed by findings — the exact
  spec locations it checked, whether each was covered, and the traces (and, if
  you want, the tests) that covered them. The UI to browse them is next; the
  data is already there.
tags:
  - release
  - coverage
  - traceability
  - opentelemetry
---

Until now, a Snow-White report told you _that_ 7 of 9 response codes were covered — and asked you
to take its word for it.
1.12.0 changes what sits underneath that number.

## From a ratio to evidence

Every one of the 14 criteria now produces **findings** instead of just a ratio: one per spec
location it evaluated (path and method, plus response code, parameter or content type where it
applies), each marked covered, uncovered or not applicable, and each covered one pointing at the
traces that satisfied it.
The ratio you already know is derived from those findings, not computed on the side — so the
number and the evidence can't drift apart.

In other words: Snow-White no longer claims an endpoint "has been called".
It shows you _which_ call, from _which_ trace — and optionally from _which_ test.

This is the first half of
[#1642 — drill into which tests covered a criterion](https://github.com/bbortt/snow-white/issues/1642).
Findings are computed in `openapi-coverage-stream`, travel with the calculation response,
are persisted by `report-coordinator-api`, and are served with each criterion's result in the
report API.
The one piece missing is the drilldown in the web UI (step S9 in the issue's plan) — so you can't
_see_ findings in Snow-White yet, but every report calculated on 1.12.0 already carries them.

## Naming the test behind a trace

A trace ID proves a call happened; a test name tells you why.
Snow-White now reads a `test.case.name` attribute off server spans and records it on the
evidence.
With the OpenTelemetry Java agent, it's one environment variable on the service under test:

```shell
OTEL_JAVA_EXPERIMENTAL_SPAN_ATTRIBUTES_COPY_FROM_BAGGAGE_INCLUDE=test.case.name
```

and your test runner sends the name as W3C baggage:

```http
baggage: test.case.name=org.example.PetstoreIT.shouldRejectUnknownPet
```

It's entirely optional — without it, findings still point at trace IDs.
The [onboarding guide](https://bbortt.github.io/snow-white/onboarding/#option-a-spring-boot-recommended)
walks through it, and both bundled examples ship with it wired up.

## What's next: waivers

Findings are also the groundwork for
[#2009 — per-endpoint waivers](https://github.com/bbortt/snow-white/issues/2009).
Today, if one check can't realistically be met — say, a `500` that's only provoked in a
component test — your only option is to drop that criterion for the whole API.
Waivers will let you exempt exactly one spec location, with a reason, while the criterion keeps
being enforced everywhere else.
A waiver needs something precise to point at, and that is what a finding is.
Both issues are in progress in parallel.

## Bug fixes

A lot of them — mostly surfaced while rebuilding every calculator on top of findings:

- **Coverage calculation:** literal path segments are now escaped when matching telemetry to
  operations, required-error-field coverage resolves telemetry by path template, and error-code
  checks are consistent across calculators.
  Telemetry is also resolved once per operation instead of once per target.
- **Quality gates:** minimum coverage percentage bounds are enforced consistently in both
  `quality-gate-api` and `report-coordinator-api`.
- **Database schemas:** redundant indexes dropped and column lengths aligned with their actual
  constraints in `api-index-api`, `quality-gate-api` and `report-coordinator-api` — migrations run
  on upgrade, nothing to do on your side.
- **Examples:** the Spring Boot example answers its own endpoints again, and both examples serve
  their API docs under Spring Boot 4.

The full list is in the
[changelog](https://github.com/bbortt/snow-white/blob/main/CHANGELOG.md#1120-2026-09-29).
