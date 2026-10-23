# No MCP tool mutates anything, the exposed set is an explicit allowlist a test asserts, and gate configuration and waivers stay permanently out of reach

<!-- markdownlint-disable MD036 -->

**Title**
No MCP tool mutates anything, the exposed set is an explicit allowlist a test asserts, and gate
configuration and waivers stay permanently out of reach

**Lens**: CON

**Status**: planned

**Description**
No tool the MCP server exposes can change any state: not a quality gate's configuration, not a
waiver, not a calculation trigger, not any other write.
The exposed tool set is an explicit
allowlist, and a test asserts the server advertises exactly that set and no more — so a future
tool added to the module cannot silently join the exposed set, and "we did not write a write
tool" stays a property the test enforces rather than an incidental fact about what nobody has
added yet.

Gate configuration (`quality-gate-api`'s CRUD contract, `SW-009`) and waiver management stay out
of reach of any MCP tool permanently, not only for as long as v1 happens to be read-only.
A
waiver has only one entry point in the whole system — the `waivers` array on a calculation
request (`SW-040`), not a separate endpoint — so this constraint binds a future tool narrower
than "do not add a waiver-CRUD tool": if a future version of this server exposes a
`trigger_calculation` tool at all, that tool's input must not accept a `waivers` field, even
though the `QualityGateCalculationRequest` contract it calls through accepts one.
An agent able
to waive a finding or edit a gate can make its own work look finished — the exact failure mode
Snow-White exists to catch rather than enable.

**Rationale**
The gate's credibility rests on no consumer being able to influence its own verdict.
A human
operator editing a gate or applying a waiver is accountable in a way an autonomous agent is not:
a waiver an agent applies to its own output is a verdict the agent produced evidence for and then
graded itself, which is indistinguishable, from the outside, from an agent that simply suppressed
an inconvenient finding.
Read-only in v1 is the smaller, defensible first ship of the MCP surface
generally; the permanent exclusion of gate configuration and waivers is a narrower and stronger
rule that holds even if v1's general read-only posture is ever relaxed for some other tool,
because the two invariants protect different things — one protects "nothing mutates yet," the
other protects "nothing an agent can call ever decides its own grade."

An allowlist asserted by a test, rather than a style convention followed by every tool added so
far, is what makes the invariant survive a change nobody reviewing it for intent thinks to
re-check: a new tool added in a later PR that happens to call a write endpoint fails the
allowlist test immediately, rather than depending on every future author independently
remembering a rule from a design discussion.

**Verification Description**
A test calls the server's tool-listing capability and asserts the returned tool names equal exactly
the allowlisted set, in both transport modes.
The expected set is written out literally in the test — `list_apis`, `explain_criterion`,
`get_coverage`, `list_findings` for v1.
The test must not read that set from the same constant the production code registers its tools
from: an assertion comparing a value to itself passes however that value changes, and a mutation
applied to the one constant both sides read changes both sides identically, so neither the test nor
the mutation gate would register the difference.
Adding a tool therefore fails this test until someone edits that literal, which is the point at
which the read-only question gets asked.
Separately, an ArchUnit rule asserts that no generated-client method for a non-`GET` operation is
reachable from the tool-handler package.
The verb is visible to that rule, but not through ArchUnit's fluent DSL: the generated `restclient`
code passes it as a static field read — `apiClient.invokeAPI(path, HttpMethod.POST, ...)` — so the
rule is a custom `ArchCondition` that takes each call the handler package makes into a generated API
class and rejects the target if its body reads any `HttpMethod` constant other than `GET`.
That is one hop rather than a transitive walk, because the handler calls the generated method
directly.
Whoever implements this should not conclude from the fluent DSL's silence that the verb is
unreachable and fall back to a list of operation names, which is the thing this rule exists to avoid.
The rule is stated over the HTTP verb rather than over a list of endpoint names, because a list has
to be maintained against evolving OpenAPI documents and silently stops covering whatever was added
since: `quality-gate-api`'s gate CRUD (`SW-009`), `api-index-api`'s `ingestApi`,
`report-coordinator-api`'s `calculateQualityGate` — whose request carries the only `waivers` field
in the system (`SW-040`) — and its `housekeeping` operation, which times stale reports out rather
than deleting them (`SW-018`), are each caught by the verb without anyone having to list them.
Generation is one execution per OpenAPI document, so a write operation becomes reachable either
because it shares a document with a read this module needs — `ingestApi` sits in the very document
`list_apis` reads from — or because someone adds an execution for a document that is all writes.
The verb catches both, and neither depends on a reviewer noticing.
This has to be a rule over call targets rather than an assertion about the tool listing, because a
listing exposes a tool's name and input schema and says nothing about what its handler calls.

## Relations

**Related**

- [SYS-015](SYS-015-agent-queryable-consumption-path.md) — the consumption path this
  invariant bounds
- [ARCH-021](ARCH-021-mcp-surface-is-a-facade.md) — the façade decision this invariant
  narrows: every call this façade makes is additionally read-only
- [ARCH-022](ARCH-022-one-module-two-transports.md) — the module and both transport
  modes this invariant holds in, without exception
- [SW-009](SW-009-quality-gate-crud-contract.md) — the gate-configuration write surface this
  invariant keeps permanently out of reach
- [SW-040](SW-040-waiver-enters-only-on-the-calculation-request.md) — the only entry point a
  waiver has, which is what a future `trigger_calculation` tool's input must exclude
- [SW-013](SW-013-calculation-trigger-is-all-or-nothing.md) — the calculation-trigger contract a
  future `trigger_calculation` tool would call through, narrower than what it accepts
