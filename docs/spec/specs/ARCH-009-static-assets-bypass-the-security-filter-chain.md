# Static frontend assets bypass the reactive security filter chain entirely, rather than being permitted within it

<!-- markdownlint-disable MD036 -->

**Title**
Static frontend assets bypass the reactive security filter chain entirely, rather than being
permitted within it

**Lens**: ARCH

**Status**: active

**Description**
`SecurityConfig`'s `securityMatcher` is a `NegatedServerWebExchangeMatcher` over
`/app/**`, `/i18n/**`, and `/content/**` — meaning the entire `SecurityWebFilterChain` this class
configures **does not run at all** for those three path families.
This is a different decision than authorizing those paths inside the chain: a request under
`/content/**` receives no CORS handling, no CSRF handling, no `SpaWebFilter` rewrite, and none of
the response headers `SecurityConfig` otherwise adds (CSP, `X-Frame-Options`, referrer-policy,
permissions-policy).
Every other path — including the SPA's own client-side routes, which fall through to `anyExchange`
— is authorized inside the chain and does receive that processing.

**Rationale**
`/app/**`, `/i18n/**`, and `/content/**` serve immutable, pre-built static files (compiled
JS/CSS bundles, translation JSON, images) that carry no session state and originate the requests
they are fetched by, so none of CORS, CSRF, or a CSP/frame/referrer header apply to them the way
they do to the HTML document or an API response.
Excluding them from the matcher, rather than `permitAll`-ing them inside it, means Spring Security
never allocates a security context, evaluates the authorization rule set, or runs `SpaWebFilter`'s
path-rewrite logic for the highest-volume request class the gateway serves — a real cost difference
at the scale a browser's own asset-loading burst produces, not just a modelling nicety.
The trade-off is that a change to CORS, CSRF, or header policy must be deliberately verified as
`securityMatcher`-exempt or not for each new static path family, since the exemption is boolean and
total rather than a per-header opt-out.

**Verification Description**
`StaticResourcesAppTest` requests `/content/images/logo.png` against a real, `prod`-profile
gateway and asserts `200` with `Content-Encoding: gzip` and the correct `image/png` media type —
confirming the request reaches the static-resource handler rather than being rejected or rewritten
by the security chain or `SpaWebFilter`.

## Relations

**Related**

- [SW-024](SW-024-spa-fallback-rewrites-unmapped-routes-to-index-html.md) — the
  `SpaWebFilter` rewrite this exemption keeps static assets out of
