# The gateway's error response distinguishes an unreachable backend from a timeout from any other failure

<!-- markdownlint-disable MD036 -->

**Title**
The gateway's error response distinguishes an unreachable backend from a timeout from any other
failure

**Lens**: SW

**Status**: active

**Description**
`GatewayErrorHandler` maps three outcomes: a downstream connectivity failure
(`ConnectException`, `NoRouteToHostException`, or `UnknownHostException` — a routed backend that
cannot be reached at all) answers `503` **and** carries a `Gateway-Error: DOWNSTREAM_UNAVAILABLE`
response header; a `TimeoutException` (a backend reached but not answering in time) answers `504`
with no such header; any other exception answers `500`, also with no such header.
The header is the only signal that distinguishes the connectivity case from the generic-failure
case — both can otherwise present as a bare non-2xx status with no body.

**Rationale**
"The backend is unreachable" and "something else went wrong in the gateway" call for different
responses from an operator or an automated caller: the first is transient infrastructure state
worth retrying or alerting on with a specific runbook, the second may be a defect that retrying
will not fix.
A status code alone conflates them — `503` is a generic "try later" signal in HTTP — so the header
carries the more specific classification a status code cannot.
Timeouts are mapped to the standard gateway-timeout status (`504`) rather than folded into the
downstream-unavailable case, because a slow-but-reachable backend is a different failure mode than
an absent one and already has its own standard HTTP semantics to use.

**Verification Description**
`GatewayErrorHandlerUnitTest` asserts each of the three connectivity exceptions maps to `503` with
the header set, `TimeoutException` maps to `504` with the header absent, and an arbitrary
exception maps to `500` with the header absent.
`GatewayErrorHandlerIT` exercises the downstream-unavailable path end to end against a real,
unreachable backend URL and asserts `503` with the header present.

## Relations

**Related**

- [CON-002](CON-002-tolerate-dependency-outages.md) — the outage-tolerance property this mapping
  makes diagnosable rather than silent
- [NF-005](NF-005-clear-failure-feedback.md) — the clear-feedback attribute the header provides
  over a bare status code
