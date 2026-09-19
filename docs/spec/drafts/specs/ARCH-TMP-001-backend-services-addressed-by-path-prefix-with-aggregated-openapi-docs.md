# Backend services are addressed by a fixed REST path prefix, with their OpenAPI docs re-exposed under one aggregated path per service

<!-- markdownlint-disable MD036 -->

**Title**
Backend services are addressed by a fixed REST path prefix, with their OpenAPI docs re-exposed
under one aggregated path per service

**Lens**: ARCH

**Status**: active

**Description**
`api-gateway` routes by a static, configuration-driven table rather than any form of service
discovery: `RoutingConfig` declares one `RouteLocator` bean per backend
(`api-index-api`, `quality-gate-api`, `report-coordinator-api`), each matching a fixed REST path
prefix (for example `/api/rest/v1/criteria/**` and `/api/rest/v1/quality-gates/**` for
`quality-gate-api`) and forwarding to that backend's configured URL.
No two backends share a path prefix, and no path is routed to more than one backend.

Each backend's own OpenAPI document is re-exposed under a **second**, per-backend route —
`/v3/api-docs/<service>` — with the path rewritten to that backend's own root `/v3/api-docs` before
the request is forwarded.
This is deliberately a second route, not a side effect of the REST-prefix route: a backend's docs
endpoint does not itself live under that backend's REST prefix, so the gateway aggregates all three
under one external path family (`/v3/api-docs/*`) a single Swagger UI can enumerate, rather than
each backend answering its docs request at an address only that backend recognizes.

Separately, when a distinct management port is configured (`management.server.port` differs from
`server.port`), a further route forwards `/management/info` to that port — the only actuator path
the gateway proxies to a different port than the rest of the traffic; see
[CON-TMP-001](CON-TMP-001-actuator-endpoints-deny-by-default.md) for which actuator paths are
reachable at all.

**Rationale**
A reverse proxy in front of a small, fixed set of internally-owned backends does not need dynamic
service discovery — the backend set changes at deploy time, not at request time, so a static route
table declared alongside the deployment configuration is simpler to reason about and to test than a
discovery mechanism this system has no other use for.
Aggregating the OpenAPI docs under one path family lets the frontend (and any external tooling)
address "the docs for backend X" without needing to know each backend's own internal docs path or
run a separate discovery step per backend; the rewrite keeps that external contract stable even if
a backend's own docs path convention changes.
Proxying only `/management/info` to a distinct management port — rather than the whole
`/management/**` tree — keeps the gateway's own actuator surface
([CON-TMP-001](CON-TMP-001-actuator-endpoints-deny-by-default.md)) as the single source of truth
for what the gateway itself reports as up, while still surfacing the one piece of information
(active profiles) worth exposing to a caller.

**Verification Description**
`RoutingConfigUnitTest` and `RoutingConfigIT` assert each backend's REST-prefix route resolves to
the configured backend URL and that the docs route rewrites the path correctly.
`ApiIndexApiAppTest`, `QualityGateApiAppTest`, and `ReportCoordinationServiceAppTest` exercise the
routes end to end against the real backends.
`ManagementEndpointsAppTest` asserts `/management/info` is served from the configured management
port and returns the active profiles.

## Relations

**Related**

- [CON-TMP-001](CON-TMP-001-actuator-endpoints-deny-by-default.md) — the actuator surface this
  routing decision keeps narrow
- [SYS-012](SYS-012-result-consumption.md) — the visual-consumption capability this routing serves
  as the UI's ingress
