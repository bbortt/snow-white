# Every gate read publishes who owns the gates, as a response header

<!-- markdownlint-disable MD036 -->

**Title**
Every gate read publishes who owns the gates, as a response header

**Lens**: SW

**Status**: active

**Description**
`GET /api/rest/v1/quality-gates` and `GET /api/rest/v1/quality-gates/{name}` carry the instance's
gate-ownership mode on every response, as the `X-Quality-Gate-Ownership` header, beside the list's
existing `X-Total-Count`.
A client therefore knows whether a write would be refused before it offers the control that issues
one.

The header takes one of three values: `database` for an instance whose gates are owned through this
API, `file` for one whose gates come from its gate file, and `file-empty` for one whose gate file
defines no gates at all.
Three values of one header rather than two headers or a header plus a boolean: the three are
mutually exclusive states of a single fact about the instance, a client decides what to render from
exactly one lookup, and there is no pair of values that can disagree with each other.
`file-empty` is published rather than left to be inferred from a zero-length page, because an
empty page and a file that defines nothing are not the same thing to a screen that has to say why
it is empty and why its controls are disabled.

The mode is published on the single-gate read as well as the list, because an edit view is
reachable by deep link without the list ever being fetched.

It is not published as a field on `QualityGateConfig`.
That schema describes one gate; an instance-level fact placed on it would be indistinguishable from
the per-gate ownership flag [ARCH-025](ARCH-025-gitops-mode-is-instance-configuration.md)
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
instance in each of the three — including the `400` a malformed `sort` earns, which the resource
never returns itself.
A webapp test asserts the client reads the header off both responses into its gate state, the way it
already reads `x-total-count` off the list.
What the screens then do with it is
[SW-054](SW-054-managed-instance-ui-offers-no-gate-write.md)'s to verify.

## Relations

**Realizes**

- [CON-016](CON-016-managed-instance-has-exactly-one-gate-writer.md) — makes the single
  writer visible to a client rather than only enforced against it

**Related**

- [ARCH-025](ARCH-025-gitops-mode-is-instance-configuration.md) — the instance-level fact
  this read publishes, and the per-gate field it must not become
- [SW-051](SW-051-managed-instance-rejects-crud-writes-with-conflict.md) — the refusal this
  lets a client avoid issuing
- [SW-054](SW-054-managed-instance-ui-offers-no-gate-write.md) — what the web UI does with
  the header, and the screens it has to reach
- [SW-009](SW-009-quality-gate-crud-contract.md) — the read contract this extends without changing
  a status code or a body
- [CON-006](CON-006-actuator-endpoints-deny-by-default.md) — why the management surface is not an
  option for this
- [ARCH-018](ARCH-018-management-surface-without-a-business-one.md) — the same boundary, stated from
  the service side

## Changes

- **2026-11-16** — Set active: implementation of `STR-029` began.
