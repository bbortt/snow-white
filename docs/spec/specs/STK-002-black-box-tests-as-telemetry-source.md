# Usable with black-box system or integration tests as the primary telemetry source

<!-- markdownlint-disable MD036 -->

**Title**
Usable with black-box system or integration tests as the primary telemetry source

**Lens**: STK

**Status**: active

**Description**
Teams practicing specification-first or mono-repository API development need coverage feedback
inside their own CI pipeline, before code reaches production — not only from live traffic.
Snow-White meets this by treating OpenTelemetry traces from black-box system or integration test
runs as a first-class telemetry source, equal in standing to production traces.

**Rationale**
Waiting for production traffic to validate API coverage means the feedback loop closes after
release, when a gap is expensive to fix.
A pipeline step that fails the build on insufficient coverage — driven by the same test run that
already exercises the service — closes that loop before merge.

**Verification Description**
A CI pipeline runs its integration test suite with the OpenTelemetry agent attached, then invokes
`snow-white calculate`; the review confirms the calculation used only the telemetry produced by
that test run and that an insufficiently covered API fails the pipeline step.

## Relations

**Related**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — the correlation this telemetry
  source feeds
