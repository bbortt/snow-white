# On-demand recomputation of analysis results when new telemetry or specifications arrive

<!-- markdownlint-disable MD036 -->

**Title**
On-demand recomputation of analysis results when new telemetry or specifications arrive

**Lens**: SYS

**Status**: active

**Description**
An analysis result is not fixed once computed: Snow-White recomputes it when asked again, picking
up any specification or telemetry data that has changed since the prior run, rather than serving a
cached result indefinitely.

**Rationale**
API implementations and their telemetry evolve continuously; a result that could never be
refreshed would drift from reality within days.
Recomputation on request — rather than continuous background recomputation — keeps the cost of
staying current under the caller's control.

**Verification Description**
A test runs an analysis, adds telemetry that changes the outcome, re-triggers the analysis, and
asserts the new result reflects the added telemetry.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — keeps the correlation's verdict
  current

**Related**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — what is recomputed
