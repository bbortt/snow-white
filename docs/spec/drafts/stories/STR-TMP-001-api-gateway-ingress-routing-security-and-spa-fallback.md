# Documented ingress routing, security perimeter, and SPA fallback for `api-gateway`

<!-- markdownlint-disable MD036 -->

**Title**
Documented ingress routing, security perimeter, and SPA fallback for `api-gateway`

**Status**: planned

**Business Value**
`SYS-012` names visual consumption of analysis and quality-gate results through the web UI as a
system capability, and `pages/_pages/architecture.md` states that every UI call travels
synchronously through `api-gateway` to its owning backend — but nothing in the spec corpus pins
down how the gateway actually behaves as that single ingress.
`api-gateway` is the last microservice still missing a retrace: `api-index-api`,
`quality-gate-api`, `report-coordinator-api`, and `openapi-coverage-stream` were already retraced
(`STR-008`, `STR-009`, `STR-011`).
Its most consequential behaviours are exactly the ones a caller or an operator cannot see from
reading the route table alone: that static frontend assets skip the security filter chain
entirely, that every actuator endpoint except health and info is denied even though the gateway
exposes a management port, that an unmapped client-side path is rewritten server-side rather than
404ing, and that a caller can tell "the backend is down" apart from any other gateway failure by a
response header.
Pinning these down gives the CLI, other consumers of the gateway's HTTP surface, and future
changes something to check against instead of only the running code.

**Problem / Context**
`microservices/api-gateway` is a Spring Cloud Gateway reverse proxy: it routes REST calls and each
backend's OpenAPI document to the owning backend service (`api-index-api`, `quality-gate-api`,
`report-coordinator-api`), serves the React SPA and its static assets, and enforces the security
perimeter (CORS, security headers, actuator exposure) in front of all of it.
A source read of the module's Java backend (`RoutingConfig`, `SecurityConfig`, `SpaWebFilter`,
`GatewayErrorHandler`, `ApiGatewayProperties`/`ApiGatewayPropertiesValidator`,
`InfoEndpointConfig`, against the existing unit and `SpaWebFilterIT`/`GatewayErrorHandlerIT`-style
black-box suites) found no code anchored to any spec in the corpus — this module's behaviour is
currently undocumented outside the code.
Scope is the Java backend only: clew's only configured generator targets Java, so it cannot anchor
the React/TypeScript webapp under `src/main/webapp`.

Several behaviours stood out as consequential enough to need a spec of their own:

- Each backend service owns a fixed REST path prefix and its own OpenAPI document, re-exposed by
  the gateway under one path per backend (`/v3/api-docs/<service>`) so a single aggregated
  Swagger UI can address all three independently-versioned backends.
- `SecurityConfig`'s `securityMatcher` excludes `/app/**`, `/i18n/**`, and `/content/**` from the
  reactive security filter chain **entirely** — not `permitAll`'d inside it — so static frontend
  assets receive no CORS, CSRF, security-header, or `SpaWebFilter` processing at all.
- Every actuator endpoint is denied except `/management/health(/**)` and `/management/info`, even
  though the gateway can expose a separate management port for the latter.
- `SpaWebFilter` rewrites any request that isn't under `/api/`, `/management`, `/swagger-ui`,
  `/v3/api-docs`, and contains no `.`, to `/index.html` server-side, so a deep link or a hard
  refresh into a client-side SPA route resolves instead of 404ing.
- `GatewayErrorHandler` maps a downstream connectivity failure (`ConnectException`,
  `NoRouteToHostException`, `UnknownHostException`) to `503` with a `Gateway-Error:
DOWNSTREAM_UNAVAILABLE` response header, a `TimeoutException` to `504`, and everything else to
  `500` — so a caller can tell an unreachable backend apart from any other gateway failure.

**Solution Approach**
Add two architecture specs for the structural decisions — the path-prefix routing and OpenAPI-docs
aggregation scheme, and static assets bypassing the security filter chain entirely — one
constraint spec for the actuator deny-by-default invariant, and two software specs for the
gateway's own observable behaviour — the SPA fallback rewrite and the downstream-failure error
mapping.
No code changes — this documents the existing, already-tested behaviour and anchors it.

**Acceptance Criteria**

- A spec states that each backend service owns a fixed REST path prefix the gateway routes to by
  configured URL, and that each backend's OpenAPI document is re-exposed under
  `/v3/api-docs/<service>` with the path rewritten to that backend's own root docs path before
  forwarding.
- A spec states that `/app/**`, `/i18n/**`, and `/content/**` are excluded from the reactive
  security filter chain by `securityMatcher`, not merely `permitAll`'d within it, and what that
  means in practice (no CORS, CSRF, security-header, or SPA-fallback processing for those paths).
- A spec states the actuator deny-by-default invariant: only `/management/health(/**)` and
  `/management/info` are reachable through the gateway; every other `/management/**` path is
  denied regardless of whether a separate management port is configured.
- A spec states the SPA fallback rule: a request rewritten to `/index.html` when its path is not
  under `/api/`, `/management`, `/swagger-ui`, or `/v3/api-docs`, and contains no `.`; and that a
  path failing that rule (an API call, a dotted static-asset request) is passed through unchanged.
- A spec states the gateway's error-mapping contract: downstream connectivity failures answer
  `503` with a `Gateway-Error: DOWNSTREAM_UNAVAILABLE` header, timeouts answer `504`, and any other
  failure answers `500` with no such header.

**Out of scope**

- The React/TypeScript webapp under `src/main/webapp` — clew's only configured generator targets
  Java and cannot anchor it.
- Anchoring the system-level capability (`SYS-012`) this module partially realizes as the UI's
  synchronous ingress.
  That remains unanchored here, consistent with the `quality-gate-api` and
  `report-coordinator-api` retraces, which likewise related to their `SYS` parents without
  claiming them; closing that `SYS`/`STK` coverage gap is its own story.
- The specific security-header **values** applied (CSP directives, permissions-policy list,
  frame/referrer policy) — the spec pins that the gateway is the sole layer applying them, not
  each value's rationale, which is a browser-hardening decision better revisited as its own NF
  spec if it needs to change.
- `ApiGatewayPropertiesValidator`'s required-configuration fail-fast check.
  The same
  per-module `*PropertiesValidator` pattern exists across every microservice and none of the prior
  retraces speced it either; documented once, generically, if it is ever pulled out of this
  per-module repetition.
- The generic `{code, message}` error envelope shared cross-cutting convention, already explicitly
  left unclaimed by `STR-009`.

## Relations

**Realizes**

- [ARCH-TMP-001](../specs/ARCH-TMP-001-backend-services-addressed-by-path-prefix-with-aggregated-openapi-docs.md)
  — the routing and docs-aggregation scheme
- [ARCH-TMP-002](../specs/ARCH-TMP-002-static-assets-bypass-the-security-filter-chain.md) — static
  assets excluded from the filter chain entirely
- [CON-TMP-001](../specs/CON-TMP-001-actuator-endpoints-deny-by-default.md) — the actuator
  deny-by-default invariant
- [SW-TMP-001](../specs/SW-TMP-001-spa-fallback-rewrites-unmapped-routes-to-index-html.md) — the
  SPA fallback rewrite
- [SW-TMP-002](../specs/SW-TMP-002-gateway-error-mapping-distinguishes-downstream-unavailable.md)
  — the downstream-failure error mapping

**Related**

- [SYS-012](../specs/SYS-012-result-consumption.md) — the visual-consumption capability this
  module's routing and SPA serving implement
- [CON-002](../specs/CON-002-tolerate-dependency-outages.md) — the outage tolerance the
  downstream-unavailable error mapping contributes to
- [NF-005](../specs/NF-005-clear-failure-feedback.md) — the clear-feedback attribute the
  `Gateway-Error` header serves
- [STR-009](STR-009-quality-gate-configuration-and-criteria-management-api.md) — a sibling retrace
  that likewise left its `*PropertiesValidator` and `SYS` anchoring out of scope
- [STR-011](STR-011-analysis-coordination-and-report-lifecycle.md) — a sibling retrace with the
  same shape (undocumented existing behaviour, no code changes)
