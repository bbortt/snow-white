# A third consumption path: an agentic client queries a result interactively instead of consuming a document

<!-- markdownlint-disable MD036 -->

**Title**
A third consumption path: an agentic client queries a result interactively instead of consuming
a document

**Lens**: SYS

**Status**: active

**Description**
Beyond the programmatic form a CI pipeline consumes and the visual form a developer consumes
(`SYS-012`), Snow-White makes analysis and quality-gate results queryable by an agentic client
over a live session: the client asks for exactly the indexed API, criterion, coverage, or finding
it needs, when it needs it, rather than receiving one complete document and reasoning only over
whatever that document happened to contain.

**Rationale**
An agent's working loop is interactive: it asks a question, acts on the answer, and asks the next
question informed by it.
A document-shaped interface — the JSON read, the JUnit export,
`--agentic` — forces every follow-up back through a new invocation and a fresh parse, even when
the agent already holds a session open and already knows what it wants to ask next.
Serving the
same data through a queryable interface removes that friction without adding a new capability:
every answer the interface gives is already computed and already served elsewhere; only the shape
of asking for it is new.

**Verification Description**
A test opens a session against the server and, by individual tool call, retrieves the indexed-API
list, a criterion's description, an API's coverage, and a report's findings — asserting each
value equals the same value read through the corresponding existing REST endpoint for the same
input.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — makes the correlation's outcome usable
  by a third audience, alongside the pipeline and the developer `SYS-012` already serves

**Related**

- [SYS-012](SYS-012-result-consumption.md) — the programmatic/visual pair this is a third member
  of
- [SYS-009](SYS-009-machine-consumable-gate-result.md) — the gate-level machine-consumable
  subset this path can also reach
- [ARCH-021](ARCH-021-mcp-surface-is-a-facade.md) — the façade decision that lets this
  path inherit the existing surfaces' agreement for free, deriving nothing of its own
- [ARCH-022](ARCH-022-one-module-two-transports.md) — the module and transport shape
  this path runs on
- [CON-014](CON-014-mcp-tool-set-is-read-only.md) — the read-only boundary this path
  does not cross
