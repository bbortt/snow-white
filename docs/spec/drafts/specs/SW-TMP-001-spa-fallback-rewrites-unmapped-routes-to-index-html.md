# An unmapped, extensionless request outside the API/management/docs surface is rewritten server-side to index.html

<!-- markdownlint-disable MD036 -->

**Title**
An unmapped, extensionless request outside the API/management/docs surface is rewritten
server-side to index.html

**Lens**: SW

**Status**: active

**Description**
`SpaWebFilter` rewrites a request's path to `/index.html` when **all** of the following hold: the
path does not start with `/api/`, `/management`, `/swagger-ui`, or `/v3/api-docs`; the path
contains no `.`; and the path matches `/(.*)` (effectively, any absolute path).
Any request failing one of these — an API call, a management/docs/swagger-ui path, or a path
naming a file extension — passes through unchanged.
The rewrite happens **server-side**, before the request reaches the SPA's own client-side router:
the browser's address bar and history keep the originally-requested path, only the served content
changes to the SPA shell.

**Rationale**
A single-page application's client-side router only runs once its JavaScript bundle has loaded and
executed, which happens from `index.html`.
A deep link or a hard refresh into a client-side route (for example `/quality-gate/my-gate`)
otherwise reaches the gateway as a request for a resource literally named `/quality-gate/my-gate`,
which no server-side route serves — a `404` before the client-side router ever gets the chance to
handle it.
Excluding dotted paths keeps a genuinely missing static asset (a mistyped image path) `404`ing
instead of being silently served the SPA shell, which would turn a missing-file bug into a
confusing blank-page-that-looks-like-a-route bug instead.

**Verification Description**
`SpaWebFilterIT` parameterizes several SPA client routes (`/`, `/api-index`,
`/open-api-criterion`, `/quality-gate`, `/quality-gate-config`) and asserts each is served the
`index.html` content, including three, four, and five path-segment-deep unmapped routes; and
separately asserts an `/api/**` request, a `/v3/api-docs` request, and a dotted-file request
(`/file.js`, `/foo.js`, `/foo/bar.js`) are **not** rewritten — the API and docs requests reach
their own handlers, the dotted-file requests answer `404`/`403` rather than the SPA shell.

## Relations

**Related**

- [ARCH-TMP-002](ARCH-TMP-002-static-assets-bypass-the-security-filter-chain.md) — the static-asset
  paths this rewrite never runs against
- [SYS-012](SYS-012-result-consumption.md) — the visual-consumption capability the SPA this
  fallback keeps working for is the UI of
