---
title: 'Release 2.4.0: ask a running Snow-White over MCP'
excerpt: >
  Snow-White publishes a Model Context Protocol server with four read-only
  tools, so an agent can ask a running installation what a report already
  says instead of recalculating to find out. It is off by default, and it
  carries no authentication of its own.
tags:
  - release
  - agentic
  - mcp
  - claude-code
---

Before this release, an agent that wanted to know anything about coverage had one way to find out:
run `snow-white calculate` again and parse what came back.
That is the right tool when the calculation still has to happen, and the wrong one for "which APIs
are even indexed?" or "what does this criterion actually judge?" — questions a report already
answers.

2.4.0 adds a second surface for those: an MCP server, `mcp-api`, reachable at `/mcp` through
`api-gateway`.

## Four tools, all read-only

None of them can start a calculation or change a gate.

- **`list_apis`** — which specifications Snow-White has indexed, optionally narrowed to one service
  or API name.
  The place to start, because it says what can be asked about.
- **`get_coverage`** — the most recent report covering a service or API, with each interface's
  per-criterion coverage and the judged and waived target counts.
  It does not say whether the gate passed; the report's own status does, and it distinguishes a
  calculation still running from absent coverage.
- **`list_findings`** — one report in full, keyed by the calculation UUID `get_coverage` returns,
  with every finding's JSON Pointer into the specification and, on a covered one, the traces that
  evidence it.
  An uncovered finding carries no evidence by design, and a calculation still running answers with
  no findings at all rather than an error — so here too, read the status before concluding a report
  found nothing.
- **`explain_criterion`** — what one criterion named by a report judges.

The figures these return are the same published values the JSON REST API, the JUnit export and
`--agentic` carry for that report, so mixing surfaces across one investigation is safe.

The [APM skill]({% post_url 2026-09-12-agentic-mode-and-apm-skill-package %}) now knows about them
and when to prefer them: the CLI when the calculation still has to run, these tools when a report
exists and the question is "what else?".

## It is off by default

`snowWhite.mcpApi.enabled` is `false`.
An MCP server only earns its JVM where an agentic client actually reaches it, so an installation
deploys one where an operator asked for one:

```yaml
snowWhite:
  mcpApi:
    enabled: true
```

Leaving it off also leaves `/mcp` unrouted.
The gateway's route exists only when `snow.white.api.gateway.mcp-api-url` is set — the chart emits
it as `SNOW_WHITE_API_GATEWAY_MCP-API-URL`, and only when the server is enabled — so an
installation without the server answers `/mcp` as a path it does not serve, rather than routing to
a backend that is not there.

For local work there is also a `stdio` profile, which runs the same tools over standard streams for
a process an agent launches itself instead of a hosted deployment.

## What an operator has to know

**`/mcp` is not authenticated.**
`mcp-api` brings no security configuration of its own, and the gateway's chain ends in
`anyExchange().permitAll()` — so `/mcp` is exactly as exposed as `/api/**` already is.
Everything it serves is read-only, and all of it is readable through the REST API today, so this
opens no new data.
It does mean that if your `api-gateway` is reachable from somewhere you would not want a stranger
querying, enabling this adds a surface to whatever already fronts it.
Closing that properly is its own piece of work, not a footnote to this one.

The module is a `jlink` JVM image rather than a native one, and it proxies read-only calls to three
sibling APIs — no state, no database, nothing to size a pool for.

## Also in 2.4.0

Two changes that have nothing to do with MCP ride along in the tag:
`report-coordinator-api` now rejects a calculation naming an API no coverage calculator serves,
instead of accepting it and producing nothing, and a new `spring-kafka-autoconfiguration` toolkit
module carries the OpenAPI correlation convention over to AsyncAPI.
It annotates the consumer span of a record a `@KafkaListener` receives, rather than the record
itself — nothing it does writes to your topics, and it covers the receiving side only.
For the rest — the specs, the chart, the Sonar and image plumbing — the list is the
[compare view](https://github.com/bbortt/snow-white/compare/v2.3.0...v2.4.0).
