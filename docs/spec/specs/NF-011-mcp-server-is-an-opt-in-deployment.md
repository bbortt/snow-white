# The MCP server is an opt-in deployment, and its gateway route exists only where it is configured

<!-- markdownlint-disable MD036 -->

**Title**
The MCP server is an opt-in deployment, and its gateway route exists only where it is configured

**Lens**: NF

**Status**: active

**Description**
The chart deploys `mcp-api` only when an operator asks for it.
`snowWhite.mcpApi.enabled` is off by default, and with it off the chart renders no Deployment, no
Service and no PodDisruptionBudget for that module: a default installation serves Snow-White's
programmatic and visual consumption paths and no MCP surface at all.
With it on, the chart renders all three and hands the `api-gateway` that Service's in-cluster
address, exactly as it does for the three REST backends.

The gateway treats that address as optional.
`snow-white.api-gateway.mcp-api-url` is the one backend URL whose absence is not a startup failure:
unset, the gateway starts, declares no `/mcp` route, and answers `/mcp` and `/mcp/**` with `404`.
The other three backend URLs stay required, because an installation missing one of those cannot
compute or serve a result at all.
`/mcp` is held out of the SPA fallback whether or not it is routed
([SW-024](SW-024-spa-fallback-rewrites-unmapped-routes-to-index-html.md)), so an unrouted `/mcp` is
answered as absent rather than served the SPA shell.

The setting governs the deployed HTTP transport only.
A stdio process a developer's own agent client launches is not deployed by the chart and reads none
of its values ([ARCH-022](ARCH-022-one-module-two-transports.md)), so it is out of this setting's
reach — as is the tool set, which is the same read-only four in either transport
([CON-014](CON-014-mcp-tool-set-is-read-only.md)).

**Rationale**
MCP is a consumption path for one audience: a client that speaks the protocol.
An operator who has no such client would otherwise run a second JVM, a Service and a
PodDisruptionBudget, and reserve the memory they ask for, to serve a surface nothing in that
installation calls.
That is a different kind of component from the rest of the chart: the gateway, the index, the gate
and the coordinator are how Snow-White computes and serves a result at all, while this module only
re-reads what they have already published
([ARCH-021](ARCH-021-mcp-surface-is-a-facade.md)).
A façade over existing reads is precisely the component whose absence costs an installation nothing
beyond the surface itself.

Off by default rather than on, because the server has never shipped: nothing deployed anywhere has
it, so making it opt-in now has no migration to pay for.
The same decision one release later would have to choose between leaving the default on forever and
taking a working endpoint away from somebody's installation during an upgrade.

The gateway drops the route rather than keeping it and letting it fail, because `503` is a verdict
an operator has to be able to read.
A deployed MCP server that is unhealthy, or whose Service has no ready endpoints, answers `503`; a
route left pointing at a server this installation never deployed would answer the same `503`, and
collapse the one distinction the operator would act on differently — restart something, or enable
something.
`404` says this installation has no MCP surface.

Whether the route exists is read from the URL rather than from a second gateway setting.
That address is already the fact the route is built from, so an `mcp-api.enabled` sitting beside an
`mcp-api-url` would be two sources for one truth, free to disagree, and the disagreement would
surface as either a route to nowhere or a deployed server nothing reaches.

**Verification Description**
A chart test asserts a default render contains no `mcp-api` Deployment, Service or
PodDisruptionBudget, and that enabling the module renders all three.
A chart test asserts the `api-gateway` container's environment carries no MCP URL entry in a default
render, and carries the `mcp-api` Service's in-cluster address when the module is enabled — the
assertion that catches the two halves drifting apart, a gateway told about a server that was never
deployed or a server deployed that the gateway was never told about.
A gateway test asserts the application context starts with `mcp-api-url` absent and the other three
backend URLs present, and that `/mcp` is answered as unrouted in that configuration rather than
forwarded or rewritten to the SPA shell.
A gateway test asserts the route is declared and forwards unrewritten when the URL is set.
The validator's unit test asserts each of the other three backend URLs still fails startup when it
is absent, so making one of the four optional did not quietly make them all optional.

## Relations

**Related**

- [SYS-015](SYS-015-agent-queryable-consumption-path.md) — the consumption path this setting makes
  optional to deploy
- [ARCH-021](ARCH-021-mcp-surface-is-a-facade.md) — the façade decision that makes this module's
  absence cost nothing beyond the surface
- [ARCH-022](ARCH-022-one-module-two-transports.md) — the two transports this governs the deployed
  one of, and not the stdio one
- [ARCH-008](ARCH-008-backend-services-addressed-by-path-prefix-with-aggregated-openapi-docs.md) —
  the path-prefix routing this makes conditional for one backend
- [SW-024](SW-024-spa-fallback-rewrites-unmapped-routes-to-index-html.md) — the fallback exclusion
  that makes an unrouted `/mcp` a `404` rather than the SPA shell
- [NF-010](NF-010-reference-resolution-is-off-by-default.md) — the sibling decision to ship an
  operator setting off by default
- [CON-014](CON-014-mcp-tool-set-is-read-only.md) — the tool set this setting does not vary
