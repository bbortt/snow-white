# Only the health and info actuator endpoints are reachable through the gateway; every other actuator path is denied

<!-- markdownlint-disable MD036 -->

**Title**
Only the health and info actuator endpoints are reachable through the gateway; every other
actuator path is denied

**Lens**: CON

**Status**: active

**Description**
`SecurityConfig`'s authorization rules permit `/management/health`, `/management/health/**`, and
`/management/info`, then `denyAll` the remaining `/management/**` path — evaluated in that order,
so the two permits are carved out of the deny rather than the deny being a catch-all applied only
where nothing else matched.
This holds regardless of whether `/management/info` is served on the gateway's own port or
proxied to a distinct management port
([ARCH-TMP-001](ARCH-TMP-001-backend-services-addressed-by-path-prefix-with-aggregated-openapi-docs.md)):
every other actuator endpoint Spring Boot exposes (env, beans, metrics, loggers, and any other
enabled by `management.endpoints.web.exposure.include`) is unreachable from outside the gateway,
whatever the underlying management server itself would otherwise allow.

**Rationale**
The actuator surface carries operational detail — configuration values, bean graphs, JVM
internals — that is legitimate for an operator with direct access to a service's management port,
but not for anything reaching the service through the public-facing gateway.
Denying by default and carving out only the two paths a client legitimately needs (a liveness
check, the active-profile info this gateway itself contributes) keeps that boundary enforced at
one place, independent of whatever `management.endpoints.web.exposure.include` is set to on any
given deployment.

**Verification Description**
`SpaWebFilterIT` and `ManagementEndpointsAppTest` cover the two permitted paths end to end
(`/management/health` and `/management/info`).
There is currently **no test asserting the deny side** — that a third actuator path (for example
`/management/env` or `/management/beans`) is rejected rather than proxied.
This is a real verification gap this retrace surfaces rather than papers over; see the story's
review notes for how it is resolved before this spec is anchored.

## Relations

**Related**

- [ARCH-TMP-001](ARCH-TMP-001-backend-services-addressed-by-path-prefix-with-aggregated-openapi-docs.md)
  — the routing decision this constrains for `/management/info`
