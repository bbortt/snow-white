---
title: 'Release 2.6.0: quality gates as code'
date: 2026-11-19 09:00:00 +0100
excerpt: >
  Four releases went out without a word here, so this one catches up. The
  headline: an instance deployed with a gate file holds exactly the gates that
  file names, and every write through the API answers 409. Also a fifteenth
  criterion that judges what an API answered rather than whether it was
  reached, and a Flux bug that was killing whole calculations for anyone who
  followed OpenTelemetry's own instructions.
tags:
  - release
  - quality-gate
  - operators
  - coverage
  - kubernetes
---

[2.4.0]({% post_url 2026-10-30-ask-a-running-snow-white-over-mcp %}) was the last release announced
here.
2.4.1, 2.5.0, 2.5.1 and 2.6.0 have shipped since, and this is the catch-up — heaviest first.

## A quality gate can finally live in your repository

A quality gate decides whether a build passes, which makes it part of the build contract.
It was the only part of that contract that did not live in a repository.
A gate change had no diff, no author, no PR and no history beyond whatever the database retained;
it could not be promoted from staging to production except by re-entering it, and it could not be
tried out without mutating state every pipeline shared.

2.6.0 lets the file own it.
Set `snowWhite.qualityGateApi.qualityGates` in the chart, each entry the same payload
`POST /api/rest/v1/quality-gates` already takes.
The chart renders those into a `ConfigMap`, mounts it, and the service reads it at startup.

After that startup the instance holds **exactly** those gates:

- a gate not named in the list is deleted
- none of the four predefined gates is seeded
- every create, update and delete on a gate path answers `409`
- the web UI's gate controls render disabled, with the reason, rather than quietly vanishing

The file's presence is the whole of the switch.
There is no second mode flag to disagree with it and no gate record carries an ownership marker, so
"what owns my gates, and what are they?" is answered from the deployment rather than by querying a
database.
Both gate reads publish the answer as an `X-Quality-Gate-Ownership` header — `database`, `file`, or
`file-empty` for a file-owned instance whose file defines nothing.

### Why startup, and not an apply endpoint

Seeding was already a writer, and the one that is easy to miss: `initPredefinedQualityGates` upserts
the four predefined gates on _every_ boot.
Any scheme that applied a file and then cleared the seeded gates would be undone by the next
restart.
So the file replaces that step's hardcoded definitions as its input instead, which makes startup the
single writer rather than a third one to work around.

That also deleted a lot of design surface that would otherwise need deciding: no apply endpoint, no
CLI command, no upsert-versus-prune choice, and no new authentication question.

### What it costs, stated up front

- **A gate change needs a restart.** The chart checksums the rendered `ConfigMap` into the pod
  template, so `helm upgrade` rolls the pod for you.
- **The gate file lives in the deployment repository**, not in the repository the gate is about.
- **`qualityGates: []` is not a way back.** It is a file-owned instance that defines no gates — a
  placeholder to get a deployment up.
  Omitting the key entirely is what leaves gates managed
  through the API and the web UI.

Switching an existing instance over does not refuse to come up.
Predefined gates already in the database become ordinary gates that the file replaces or removes.

## A criterion that judges what the API answered

`RESPONSE_SCHEMA_CONFORMANCE` landed in 2.5.0 as the fifteenth criterion, and the first that judges
what an API answered rather than whether it was reached.

Its target is one documented response entry of one documented operation carrying a conformance
verdict.
Covered means every verdict for that pair conformed — one violation anywhere leaves the pair
uncovered, because a schema the API breaks sometimes is a schema the API does not keep.

Two limits are deliberate:

- **A verdict for a status code no declared entry matches is not a target of this criterion.** An
  undeclared response has no documented schema to be judged against, and that traffic is
  `UNDOCUMENTED_RESPONSE_CODE`'s to report — a second criterion reporting it would count one gap
  twice.
  What this criterion owes such a verdict is that it says it read them, naming each in
  `additionalInformation`, so an operator who published one and got a ratio taken over something
  else can tell the difference from the report.
- **A pair with no verdict is not applicable, never uncovered.** A partial rollout of a
  verdict-producing harness is therefore measured over what it actually vouched for.

It needs your collector to let the verdict spans through.
2.5.0's filter change drops a span only when it carries neither `http.request.method` nor
`openapi.response.schema.valid`; the first condition alone had been dropping every verdict span.
Applied to both the dev collector config and the chart.

## Three fixes worth naming

**A captured request header could kill every calculation for a service** (2.5.1, InfluxDB only).
The Flux query wrapped each required attribute in `string(v:)`, which cannot operate on a composite
value.
`http.request.header.<key>` is an array in the OpenTelemetry semantic conventions — so a service
following OpenTelemetry's own documented
`OTEL_INSTRUMENTATION_HTTP_SERVER_CAPTURE_REQUEST_HEADERS=content-type` instruction made the whole
query fail with `cannot convert [string] to string`, a runtime error inside `map`.
Every calculation for that service died, `basic-coverage` included, not only `CONTENT_TYPE_COVERAGE`.
Each value is now JSON-encoded into its column and read back as a tree, which hands the calculators
the type the span actually carried.
`display(v:)` would also have accepted a composite — and yielded the Flux literal
`["application/json"]`, turning a failed query into a silently wrong coverage score.

**A malformed attribute could silence the strictest criterion** (2.5.1).
An `openapi.response.schema.violations` value that is not a JSON array of strings states no
violation list, but it used to be read as an empty one — so `valid=true` sitting next to "not JSON at
all" read as a conforming response.
The two readings are now distinct, and a list that cannot be read is refused as a verdict whatever
`valid` says.
It is reported as a verdict that could not be read rather than as dropped pointers, because the
operator's action is to fix the producer.

**OTLP export was broken by a transitive pin** (2.5.0).
`kafka-streams-protobuf-serde` declares okio-jvm 3.4.0 directly, which won nearest-first over the
3.18.1 that okhttp 5.5.1 requires.
3.4.0 lacks `Okio.socket(Socket)`, so every OTLP export failed with a `NoSuchMethodError`.

And in 2.4.1, the coverage stream's api-index base URL no longer falls back to
`snowWhite.kafka.brokers` — configuring external Kafka brokers used to point the stream's api-index
client at the broker list.

## The full lists

- [v2.4.0...v2.4.1](https://github.com/bbortt/snow-white/compare/v2.4.0...v2.4.1)
- [v2.4.1...v2.5.0](https://github.com/bbortt/snow-white/compare/v2.4.1...v2.5.0)
- [v2.5.0...v2.5.1](https://github.com/bbortt/snow-white/compare/v2.5.0...v2.5.1)
- [v2.5.1...v2.6.0](https://github.com/bbortt/snow-white/compare/v2.5.1...v2.6.0)
