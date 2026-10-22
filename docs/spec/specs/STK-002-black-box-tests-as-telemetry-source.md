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
- [ARCH-020](ARCH-020-test-identity-published-not-propagated.md) — how a black-box suite names the
  test behind each request it makes, which only matters because the runner and the span are in
  different processes

## Changes

- **2026-10-07** — Related to `ARCH-020`, which realizes this need's harder half.
  Treating a test run as a first-class telemetry source is what puts the test's identity in one
  process and the span in another, so a finding could say a target was exercised without being able
  to say by what.
  `ARCH-020` closes that for a JUnit-driven suite.
  Nothing about this need changed; it gained the spec that makes its evidence nameable.
