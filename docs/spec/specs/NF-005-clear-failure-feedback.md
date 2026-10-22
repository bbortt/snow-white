# Clear feedback when a requirement for successful analysis is not met

<!-- markdownlint-disable MD036 -->

**Title**
Clear feedback when a requirement for successful analysis is not met

**Lens**: NF

**Status**: active

**Description**
When an analysis cannot proceed or complete meaningfully — a missing `@SnowWhiteInformation`
annotation, an unindexed API, an unparsable specification — Snow-White reports a specific,
actionable reason rather than a generic failure or a silently empty result.

**Rationale**
A user diagnosing "why is my coverage zero" without a specific reason has to guess between many
possible causes; naming the actual missing precondition turns a support question into a
self-service fix.

**Verification Description**
A test triggers an analysis against a service missing required instrumentation and asserts the
returned error names that specific missing precondition, not a generic failure message.

## Relations

**Related**

- [SYS-012](SYS-012-result-consumption.md) — the channel this feedback is delivered through
