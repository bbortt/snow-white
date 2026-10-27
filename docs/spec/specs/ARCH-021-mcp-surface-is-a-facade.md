# The MCP surface is a façade over the existing REST reads, derives nothing, and does not port the CLI's agentic transformer

<!-- markdownlint-disable MD036 -->

**Title**
The MCP surface is a façade over the existing REST reads, derives nothing, and does not port the
CLI's agentic transformer

**Lens**: ARCH

**Status**: active

**Description**
Every MCP tool is implemented as a call through a generated Java REST client — the same
`openapi-generator-maven-plugin` setup (`java` generator, `restclient` library) `api-sync-job`,
`otel-event-filter-stream`, `openapi-coverage-stream`, and `report-coordinator-api` already use —
against an existing read endpoint on `api-index-api`, `quality-gate-api`, or
`report-coordinator-api`.
No tool handler computes, compares, aggregates, or otherwise re-derives a
value those services do not already serve; a handler's own logic is limited to parameter
passthrough and reshaping the already-served value into the MCP response schema.

Two things that look like local logic are permitted, because neither produces a value the service
did not serve.
Selecting one element by its identifier out of a collection an endpoint serves whole is passthrough:
`explain_criterion` names one criterion out of `listOpenApiCoverageCriteria`'s catalog, and returns
that element verbatim rather than computing anything about it.
Narrowing a collection through query parameters the endpoint already accepts is likewise
passthrough: `get_coverage` reaches a service's and API's most recent report through
`listQualityGateReports`' own `serviceName`, `apiName`, `sort` and `size` parameters, so the
ordering and the narrowing both happen in the service, not the handler.
That filter matches at report level, so the handler still has to name one `TestedApi` out of the
report's `interfaces` — which is the same permitted selection, by the service, API and version the
caller supplied rather than by a rule of the handler's own.

Reshaping may drop nothing a consumer needs to reach the same conclusion as the other surfaces.
`CON-012` requires every published shape to carry the judged and waived target counts, so a
criterion emptied by waivers cannot read as a coverage pass; an MCP response that omitted them
would make this surface the one place that distinction disappears.
The same holds for whether a criterion is included in the gate at all.
A criterion below its threshold but excluded from the gate is not a failure, and the other surfaces
say so — the JUnit export skips it and `--agentic` leaves it out of the failure set it derives.
An MCP response carrying its coverage without that flag would let a client call it a failure, which
is the fourth-surface disagreement this spec exists to prevent.

`toolkit/cli`'s `agent-quality-gate-response-transformer.ts` is not ported to the new module, in
Java or otherwise.
That transformer does not merely reshape the report read: it derives the per-criterion gate verdict
the report does not publish, comparing each included criterion's coverage against the pinned
threshold, and then counts the failures it derived.
The report publishes a criterion's coverage and the threshold it was scored against (`SW-039`); the
per-criterion verdict is the comparison of the two, and only the API-level status (`TestedApi`) is
served.
`get_coverage` therefore serves what the report publishes — each criterion's coverage, whether it
counts toward the gate, the pinned threshold, and the judged and waived counts — and does not derive
the verdict.
A client that holds both numbers and can ask a follow-up question does not need the comparison made
for it; a document that a caller reads once and cannot interrogate does, which is why the CLI makes
it and this surface does not.
A finding already carries its JSON Pointer identity server-side (`SW-029`), so enumerating findings
is REST retrieval rather than derivation.

**Rationale**
`SW-044`, `SW-039`, `CON-012`, and `SW-016` establish that the JSON read, the JUnit export, and
`--agentic` already agree on the same numbers for one report — three surfaces reading one
underlying computation.
An MCP tool that calls through to the same REST reads is a fourth surface
that inherits that agreement for free, because it answers from the same storage the other three
already agree on.
An MCP tool that recomputed or re-derived any part of that answer would become
a fourth surface able to disagree with the other three, and would need its own agreement test to
rule that out — the same test burden `SW-044`'s "three surfaces of one report cannot disagree"
verification already carries for the existing set.
Deriving nothing is therefore not a style preference; it is what keeps a
new consumption path from reopening a correctness question the other three already closed.

The CLI transformer is the sharpest case of this, not an exception to it.
Because it derives the per-criterion verdict rather than reshaping a published one, porting it would
put a second implementation of a fail-closed threshold comparison into the system — and that
comparison is delicate in a documented way: it asks whether a criterion _clears_ the bar and negates
that, rather than asking whether it is below the bar, precisely so that an absent threshold fails
closed instead of reading as a pass.
A second implementation is a second chance to get that backwards, in a language whose own
`NaN` behavior differs, for a verdict that decides whether a build fails.
Leaving the derivation where it is keeps one implementation of it, and leaves this surface serving
the two numbers it compares.

**Verification Description**
An ArchUnit rule, alongside `internal/archunit-rules`' existing per-module layering rules, asserts
the MCP tool-handler package reaches no Snow-White package other than the generated REST clients
and the MCP response types — so a handler cannot reach a service, a repository, or a calculator to
derive a value itself.
The rule is stated over Snow-White's own packages because a handler necessarily depends on the MCP
server framework that dispatches to it; it is the reach into this system's internals that the rule
forbids.
That rule bounds the handler's dependencies, not its statements, and the agreement test below does
not close the gap either: structural equality shows a value matches, not how it was produced, so a
handler that derived the right answer would pass both.
What the two together do enforce is the consequence this spec exists for — a handler cannot reach
the inputs needed to derive a value the services do not serve, and any value it does return has to
match the surface it came from.
A handler that recomputed a published value from published inputs and got it right is left to review
rather than to a test, because a test that could see it would have to assert over statements.
A test asserts a value returned by an MCP tool — a coverage figure, a finding, a criterion
description, an indexed API — is structurally equal to the same value read through the
corresponding REST endpoint for the same report, API, or criterion.
A test asserts the MCP module has no dependency on `toolkit/cli`, and that no MCP response carries a
per-criterion pass or fail field — the shape a port of the transformer's verdict derivation would
have to produce.

## Relations

**Related**

- [SYS-015](SYS-015-agent-queryable-consumption-path.md) — the consumption path this
  façade decision serves
- [SW-044](SW-044-report-publishes-judged-and-waived-target-counts.md) — the
  three-surfaces-agree precedent this surface inherits as a fourth surface
- [SW-039](SW-039-report-publishes-its-pinned-threshold.md) — the pinned-threshold agreement
  this façade reads rather than recomputes
- [CON-012](CON-012-criterion-emptied-by-waivers-is-never-a-coverage-pass.md) — the
  waiver-derived-pass distinction this façade must read already-published, not recompute
- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — the gate-scoped verdict rule this façade
  does not reapply; the report publishes the API-level status and the inputs, not the
  per-criterion verdict
- [SW-029](SW-029-finding-identified-by-spec-pointer.md) — the spec-pointer identity that makes
  finding enumeration pure REST retrieval rather than derivation
- [ARCH-022](ARCH-022-one-module-two-transports.md) — the module this façade is
  implemented inside
