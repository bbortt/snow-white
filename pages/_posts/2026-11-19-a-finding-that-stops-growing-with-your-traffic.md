---
title: 'Release 2.6.1: a finding that stops growing with your traffic'
date: 2026-11-19 14:00:00 +0100
excerpt: >
  A coverage response used to grow with how much telemetry the window held
  rather than with the size of the document under test — far enough past
  Kafka's 1 MiB default that the stream client shut down and every restart
  failed identically. Evidence is now bounded at 32 entries per finding, and
  the bound is chosen so a capped list still says which tests reached the
  target.
tags:
  - release
  - operators
  - coverage
  - opentelemetry
---

Findings ride the result they explain, so a coverage response grew with how much telemetry the
calculation window held — not with the size of the document under test.
In one real calculation, 2579 telemetry data points became a 1.09 MB event.

Past Kafka's 1 MiB default this fails in a particularly unhelpful way.
The producer refuses its own record before the broker ever sees it, the stream client shuts down,
and the input offset is never committed — so the restart re-reads the same request and fails exactly
the same way.

## Headroom first, then the actual fix

Two changes, and only the second one is the fix.

The producer and the response topic are now sized by one constant on the event both sides speak,
`OpenApiCoverageResponseEvent.MAX_SERIALIZED_BYTES`, at 10 MiB.
Both services that might create that topic declare the limit, because either could be the one to
reach an empty cluster first, and `spring.kafka.admin.modify-topic-configs` applies it to a cluster
that already holds the topic.
Without that last part, declaring it changes nothing for an existing deployment and the rejection
merely moves from the producer to the broker.

Then the bound that makes the headroom unnecessary: at most **32 evidence entries per finding**,
matching the ceiling `ConformanceVerdict` already puts on violation pointers.
It is applied in one place — `CalculatorUtils.toEvidence`, the helper every production evidence path
across all nine calculators funnels through.
A cap in any single calculator would have left the other eight unbounded, and a test at that
chokepoint means a calculator added later cannot reintroduce the shape without failing it.

## It is not a blind truncation

This was the design decision worth making.

The attributed spans arrive grouped by test, so taking the first 32 outright would keep 32 traces of
the earliest test case and silently lose every later one.
That answers "was this target reached" while discarding "by which tests" — which is the half the
`testCaseName` dimension exists for, and the half someone reviewing a waived finding actually needs.

So the bound admits the first entry of each distinct test identity before any second entry of one
already represented, then backfills in arrival order.
A heavily exercised target keeps the breadth of which tests reached it rather than the depth of one.

The bound is deliberately not in the event's canonical constructor, either: an event serialized
before this shipped has to deserialize whole, because a reader silently dropping what a producer
sent would make the record it stored disagree with the record it received.

## What operators have to do

**Almost certainly nothing, and you do not need to clear any topic.**

Nothing oversized ever reached a topic — the producer refused its own record client-side, before the
broker.
What was stuck is an uncommitted offset on the request topic, and the record sitting there is an
ordinary, readable calculation request that merely produced an output the producer would not send.
On restart with 2.6.1 it is re-read, produces a far smaller response, commits, and the stream moves
on.

Two things worth checking anyway:

- **The response topic's `max.message.bytes`.** Applying it to an existing topic needs
  `ALTER_CONFIGS` on that topic; where that is denied, `KafkaAdmin` logs the failure and starts
  anyway, so the topic silently stays at 1 MiB and the rejection just moves to the broker with an
  error that does not say why.
  It is also only declared when `snowWhite.kafka.initTopics` is `true`,
  which is the chart default.
  With the evidence bound in place a response should fit under the old
  default anyway — that is the point of the bound — so this is belt and braces rather than a
  prerequisite.
- **Retention.** If the stuck request is older than the request topic's retention it is already
  gone, so there is nothing to reprocess and that one calculation is simply lost rather than healed.
  Re-trigger it.

## Also in 2.6.1

The API contract now describes the evidence array correctly.
It had promised no ceiling, and it claimed evidence is empty for anything but a covered finding —
which was never true: **a waived finding keeps its evidence**, deliberately, because that is how a
reviewer sees a waiver is no longer needed.

The ceiling is stated in prose rather than as a schema `maxItems`.
The bound is applied where the event is built, so a report calculated before 2.6.1 can still serve
more than 32, and a schema-validating consumer would then reject a perfectly legitimate read.

## What this leaves

`MAX_SERIALIZED_BYTES` stays at 10 MiB, and it is now headroom rather than a load-bearing limit.
What remains unbounded is the _number_ of findings, which tracks the document under test —
predictable and reviewable in a way someone else's traffic volume was not.

The full list is the
[compare view](https://github.com/bbortt/snow-white/compare/v2.6.0...v2.6.1), and the release before
this one is
[2.6.0]({% post_url 2026-11-19-quality-gates-as-code %}).
