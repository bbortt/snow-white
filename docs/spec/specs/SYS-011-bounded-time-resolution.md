# Bounded-time resolution of a triggered analysis to a terminal state

<!-- markdownlint-disable MD036 -->

**Title**
Bounded-time resolution of a triggered analysis to a terminal state

**Lens**: SYS

**Status**: active

**Description**
A triggered analysis always resolves to a terminal state — a result or a defined error — within a
bounded time, even if the telemetry backend or the asynchronous coverage computation never
responds.
A caller is never left waiting indefinitely.

**Rationale**
Coverage calculation is deliberately asynchronous (Kafka-mediated, per `docs/spec/architecture.md`)
to keep long-running work off the synchronous request path; without a bound, that same decoupling
would let a caller hang forever if the downstream response never arrives.

**Verification Description**
An integration test triggers an analysis while the telemetry backend or the response topic is
unavailable, and asserts the caller receives a terminal (error) state within the configured bound
rather than hanging.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — guarantees the correlation always
  concludes

**Related**

- [SYS-010](SYS-010-analysis-triggering.md) — what starts the bounded wait
