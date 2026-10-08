# Correlate API specifications with runtime telemetry for actionable insight

<!-- markdownlint-disable MD036 -->

**Title**
Correlate API specifications with runtime telemetry for actionable insight

**Lens**: STK

**Status**: active

**Description**
API teams need to know which parts of their declared API surface are actually exercised in
practice, not just which endpoints exist on paper.
Snow-White meets this by correlating a service's OpenAPI specification against OpenTelemetry
traces produced by real or test traffic, turning that correlation into coverage and quality-gate
results a team can act on.

**Rationale**
An OpenAPI spec alone says nothing about whether its documented behavior is actually tested or
used; runtime telemetry alone has no notion of "the specification." Only the correlation of the
two answers "which parts of your API are actually being tested?" — the question the stakeholder
holds.

**Verification Description**
A black-box or system test drives real requests against an instrumented service, runs a Snow-White
analysis against that service's OpenAPI specification, and confirms the resulting coverage report
reflects the operations actually exercised.

## Relations

**Related**

- [STK-002](STK-002-black-box-tests-as-telemetry-source.md) — a specific consumption mode
  of this correlation
- [STK-003](STK-003-no-app-code-changes-beyond-otel.md) — a constraint on how the
  telemetry side is obtained
