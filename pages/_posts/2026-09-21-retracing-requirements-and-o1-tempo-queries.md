---
title: 'Release 1.10.2: retracing requirements with clew, and Tempo queries that stop scaling with trace count'
excerpt: >
  We used @ariadne-thread/clew to retrace requirements against the actual
  implementation across our most important microservices, turning up real
  spec drift and a handful of user-facing bugs along the way — and while we
  were in there, Tempo's per-trace fetch became a single request, independent
  of how many traces match.
tags:
  - release
  - spec-driven-development
  - tempo
  - ci
---

The 1.11.0 release is less about one headline feature than about a change in how
Snow-White itself gets built — plus what that process turned up once we pointed it at the
codebase.

## Retracing requirements with clew

Over the past weeks we've been retracing `openapi-coverage-stream`, `report-coordinator-api`,
`quality-gate-api`, and `api-index-api` — the microservices that own coverage calculation and the
quality-gate domain — using [clew](https://www.npmjs.com/package/clew)
(`@ariadne-thread/clew`).
Retracing means reconstructing the spec a piece of running code was
supposed to satisfy from the code itself, then writing that spec down as a story and a set of
verifiable requirements in `docs/spec/`, anchored back to the classes that implement it.

That's groundwork for a direction we're leaning into: specification-driven development, and
further out, something closer to what the [Agentic Delivery Life
Cycle](https://www.adlc.io/#manifesto) describes — specs as the artifact an agent (or a human)
implements against and is checked against, not documentation written after the fact.
We're not
there yet across the whole reactor; this release covers the microservices where it mattered most.

## Bugs the retracing turned up

Reconstructing a spec from running code means reading what the code actually does, closely enough
to write it down precisely — and that closeness surfaced real drift between what Snow-White was
supposed to do and what it did:

- **Redelivered coverage results used to accumulate instead of replace.** If Kafka redelivered a
  calculation response — the exact scenario a slow telemetry fetch used to cause (see below) — the
  redelivered result piled up beside the original instead of replacing it.
  It's an upsert now.
- **A report could pass its quality gate while the JUnit export failed the same criteria.**
  `ApiTestResultLinker` scored against the gate's own coverage threshold, but the JUnit exporter
  failed anything short of full coverage — so a build server reading the XML could see red for a
  criterion the gate itself had already accepted.
  The gate's threshold is authoritative now: a
  criterion that clears it passes, with the remaining gap reported as a comment rather than a
  failure.
- **Reseeding a predefined quality gate left stale criteria attached.** A criterion dropped from a
  predefined gate's definition stayed attached to it in any database that had already reseeded
  once, because the reseed only ever added, never removed.
- **`api-gateway` would start without an API Index API URL configured**, silently, instead of
  failing fast like its other required backend URLs already did.

None of these needed a new spec of their own to fix — each already had (or now has) one from the
retracing itself, which is what let us tell "this behavior is wrong" apart from "this behavior is
undocumented but intentional."

## Small UX improvements

A couple of gaps this surfaced were purely on the reporting side.
When a quality gate timed out,
an API test that hadn't reported a status yet still showed as "not started" — indistinguishable
from a test that genuinely never ran, when what actually happened was the gate itself failed to
complete in time.
It now shows as timed out, matching the gate.
The API result table's coverage
indicators got a pass too, so partial coverage, full coverage, and gate-excluded criteria read
apart from each other more clearly.

## Tempo queries: from O(n+1) to O(1)

The retracing wasn't only about correctness — it's also how we found the confirmed root cause of a
production incident.
`openapi-coverage-stream`'s Tempo backend answered one calculation with a
search request plus one additional HTTP round trip _per matched trace_, to fetch each trace's full
span data.
That fetch runs synchronously inside Kafka Streams' record-processing call, so its
total latency counts against `max.poll.interval.ms` — on a busy API with many matched traces, that
per-trace fan-out was enough to push past the poll interval, fence the consumer out of its
group, and trigger the very redelivery the bug above describes.

The fix: every calculator that reads a telemetry attribute reads one of a small, closed set of
keys (HTTP method, path, status code, content-type header, and the operation's own declared header
parameters — nothing from a response body, nothing open-ended).
That set is now computed once,
up front, and passed to Tempo as a `select()` clause on the search request itself — the search
response alone is sufficient, so the per-trace fetch is gone entirely.
One request per
calculation, independent of how many traces matched, instead of one plus one-per-trace.

That same per-trace fetch turned out to be masking a second bug: Tempo's `spss` (spans-per-trace)
parameter defaults to 3, and nothing in Snow-White was overriding it, so any trace with more than
three matching spans was being silently truncated.
Removing the fetch that hid this required
making the limit explicit — `tempo.spans-per-trace-limit` (default 100) joins the existing
`tempo.search-limit` as an operator-configurable, validated-at-startup setting.

## Configurable pod resources

Every microservice's CPU/memory request and limit was hardcoded in the Helm chart's templates —
the only one an operator could already tune was `api-sync-job`'s CronJob.
They're all set under
`snowWhite.<service>.resources` now, in the same shape as that CronJob already used, so sizing a
deployment for real traffic no longer means forking the chart.

Defaults also moved: `api-index-api` and `quality-gate-api` from 128Mi to 192Mi,
`otel-event-filter-stream` from 128Mi to 160Mi, `report-coordinator-api` from 128Mi to 256Mi,
`api-gateway` from 512Mi to 768Mi, and `openapi-coverage-stream` from 512Mi to 1Gi.
`api-sync-job` is unchanged.

## Behind the scenes: a hardened pipeline

None of this is something you'll see as an operator, but it's worth knowing about: alongside the
retracing, we scoped local mutation testing (`.github/scripts/pitest-changed-classes.sh`) so a
changed-classes-only PIT run takes minutes instead of mutating the whole reactor, added the same
80% mutation-score/test-strength thresholds as a hard CI gate, and added a dev container so a
build inside this repo doesn't have to run unreviewed plugin and lifecycle scripts against a
session's full local access.
All of it is aimed at the same thing the clew retracing is: making
it safe to have an agent, not just a human, working in this codebase.
