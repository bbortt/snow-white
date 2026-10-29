# Agent-queryable result consumption

<!-- markdownlint-disable MD036 -->

**Title**
Agent-queryable result consumption

**Status**: planned

**Business Value**
An agentic client acting on Snow-White's output today has to consume a complete document — the
full JSON report or the `--agentic` CLI document — and reason entirely over whatever that document
happened to contain.
A follow-up question costs a new invocation and a fresh parse.
A queryable
interface shortens that loop: the agent asks for exactly the API, criterion, coverage, or finding
it needs, when it needs it, across one session.

**Problem / Context**
`STR-006` framed two audiences for a result: a CI pipeline needing a programmatic,
machine-consumable form, and a developer needing a visual, human-readable form.
Both existing
programmatic paths — the JSON REST reads and the `--agentic` CLI document — are one-shot: a caller
requests, receives a complete document, and reasons over whatever it contains.
An agent that wants
to ask "which operations on `payments-api` are uncovered?", "what does this criterion mean?", or
"show me the evidence for this finding" has no way to do so without shelling out again and
re-parsing from scratch.
Nothing in the repository today implements or depends on MCP (Model
Context Protocol), the mechanism by which an agentic client discovers and calls tools over a
live session.

**Solution Approach**
Expose a read-only subset of the existing REST reads as MCP tools, so an agentic client can query
Snow-White interactively instead of consuming a pre-built document.
The tools surface the same
underlying data the JSON reads and `--agentic` already serve — indexed APIs, criteria, coverage,
findings — computing, comparing, or re-deriving none of it.
v1 ships four tools, in priority order:
`list_apis`, `explain_criterion`, `get_coverage`, `list_findings`.
No tool mutates anything, and the
exposed set is an explicit allowlist a test asserts, so the read-only property holds by
construction rather than by convention.

**Acceptance Criteria**

- An MCP client can list the APIs Snow-White has indexed.
- An MCP client can retrieve a criterion's description without guessing at its meaning from its
  name.
- An MCP client can retrieve an API's coverage by naming its service and API, reaching that API's
  most recent report without already holding a report identifier — so the four tools are callable
  from a cold session, with no prior calculation to quote.
- That answer carries the report's own status and identity, so a most-recent report still running,
  timed out, or finished exceptionally is answered as that rather than as absent coverage: the
  client can ask again, or name an earlier report, instead of reading an unscored result as a
  verdict.
  Which report is the most recent is the service's ordering, and whether it is scored is a
  question about the answer rather than a filter on the request, so neither is the handler's to
  decide.
- That coverage carries each criterion's coverage, whether it counts toward the gate, the threshold
  the report was scored against, and the judged and waived target counts, so a client holds
  everything the verdict is derived from without this surface deriving it, and cannot read an
  excluded criterion as a failure.
- An MCP client can retrieve a report's findings, each with its evidence and spec pointer, keyed by
  the report identity the coverage answer already gave it.
- A report still running is answered as running there too, rather than as a report that has no
  findings: `SW-014` has an unfinished report answer `202` with a shape that carries none, and a
  client told "no findings" cannot tell that from a clean report.
- The server exposes no tool that can alter any state — a gate's configuration, a waiver, or a
  calculation trigger — and this is enforced by a test asserting the exact set of exposed tools,
  not left as an unstated property of what nobody happened to add.
- A value an MCP tool returns agrees with the same value as published by the JSON REST API, the
  JUnit export, and `--agentic`, for the same report — the published figures the four surfaces
  share, not `--agentic`'s derived failure set, which this surface does not produce.
- An installation with no agentic client runs no MCP server: it is deployed where an operator asks
  for it, and a Snow-White without one answers `/mcp` as a surface this installation does not have,
  rather than as a backend that is down.

**Out of scope**

- `trigger_calculation` — deliberately deferred past v1; read-only is the smaller, defensible
  first ship.
- Any tool that mutates a gate, applies or removes a waiver, or otherwise changes a verdict.
- Porting `toolkit/cli`'s `agent-quality-gate-response-transformer.ts` to Java; it derives the
  per-criterion gate verdict the report does not publish, and stays the one implementation of that
  comparison (`ARCH-021`).
- The deployment and packaging work (helm chart, docker-compose entry, `api-gateway` route, JVM
  image, published skill update) — later implementation increments, not a new observable behavior
  these specs need to pin.
  One exception surfaced once that increment existed: whether the server is deployed at all is an
  operator's choice rather than a consequence of installing Snow-White, which is a behavior, and
  `NF-011` pins it.
- Which probes target the deployed HTTP mode's management surface — `ARCH-022` settles the
  posture for both run modes against `ARCH-018` and `NF-001`, and defers the probe wiring to the
  increment that adds the deployment, exactly as `ARCH-018` defers its own.

## Relations

**Realizes**

- [SYS-015](../specs/SYS-015-agent-queryable-consumption-path.md) — the third
  consumption path this story delivers
- [ARCH-021](../specs/ARCH-021-mcp-surface-is-a-facade.md) — the façade decision that
  lets this new path inherit the existing surfaces' agreement for free
- [ARCH-022](../specs/ARCH-022-one-module-two-transports.md) — the module and transport
  shape the tools run on
- [CON-014](../specs/CON-014-mcp-tool-set-is-read-only.md) — the read-only invariant
  this story's tool set must hold
- [NF-011](../specs/NF-011-mcp-server-is-an-opt-in-deployment.md) — the operator's choice
  of whether this path is deployed at all

**Related**

- [STR-006](STR-006-result-consumption.md) — the two-audience framing this story extends with
  a third
- [SYS-012](../specs/SYS-012-result-consumption.md) — the programmatic/visual pair this is a
  third member of
- [SYS-009](../specs/SYS-009-machine-consumable-gate-result.md) — the gate-level
  machine-consumable subset this inherits, read rather than re-derived
