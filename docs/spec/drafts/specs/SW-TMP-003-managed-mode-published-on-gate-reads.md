# Every gate read publishes who owns the gates, as a response header

<!-- markdownlint-disable MD036 -->

**Title**
Every gate read publishes who owns the gates, as a response header

**Lens**: SW

**Status**: planned

**Description**
`GET /api/rest/v1/quality-gates` and `GET /api/rest/v1/quality-gates/{name}` carry the instance's
gate-ownership mode on every response, as a response header beside the list's existing
`X-Total-Count`.
A client therefore knows whether a write would be refused before it offers the control that issues
one.

The mode is published on the single-gate read as well as the list, because an edit view is
reachable by deep link without the list ever being fetched.

It is not published as a field on `QualityGateConfig`.
That schema describes one gate; an instance-level fact placed on it would be indistinguishable from
the per-gate ownership flag [ARCH-TMP-001](ARCH-TMP-001-gitops-mode-is-instance-configuration.md)
excludes, and would have to be repeated identically on every element of a page.
Nor is a new info or configuration endpoint introduced for it.

**Rationale**
A UI that renders create, edit and delete controls which then fail server-side is worse than one
that renders them disabled with a reason: the operator learns the rule by breaking it, and learns
nothing about where the gates actually come from.
Avoiding that needs the mode to be discoverable by the frontend, which means the server has to
publish it somewhere the frontend is already guaranteed to look.

A header on the reads is that place.
`quality-gate-config.reducer` already reads `x-total-count` off the list response, so header access
on this exact call is established rather than new, and the reads are precisely the calls a client
must make before it can render a gate or a control on one.
A client that does not care ignores a header it does not read, so nothing existing breaks.

The alternatives are worse for concrete reasons rather than on taste.
Actuator's `info` is deny-by-default at the gateway
([CON-006](CON-006-actuator-endpoints-deny-by-default.md)) and is served on each service's own
management port, which the SPA cannot reach.
A new business endpoint adds a surface, a schema and a route to answer one boolean that the call the
client is already making can carry.

**Verification Description**
A test asserts both reads carry the header, with the value matching the configured mode, on an
instance in each mode.
A webapp test asserts the gate create, edit and delete controls render disabled with a reason when
the header reports file ownership, and enabled when it does not, and that no write request is issued
in the disabled state.

## Relations

**Realizes**

- [CON-TMP-001](CON-TMP-001-managed-instance-has-exactly-one-gate-writer.md) — makes the single
  writer visible to a client rather than only enforced against it

**Related**

- [ARCH-TMP-001](ARCH-TMP-001-gitops-mode-is-instance-configuration.md) — the instance-level fact
  this read publishes, and the per-gate field it must not become
- [SW-TMP-002](SW-TMP-002-managed-instance-rejects-crud-writes-with-conflict.md) — the refusal this
  lets a client avoid issuing
- [SW-009](SW-009-quality-gate-crud-contract.md) — the read contract this extends without changing
  a status code or a body
- [CON-006](CON-006-actuator-endpoints-deny-by-default.md) — why the management surface is not an
  option for this
- [ARCH-018](ARCH-018-management-surface-without-a-business-one.md) — the same boundary, stated from
  the service side
