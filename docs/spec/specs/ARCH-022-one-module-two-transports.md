# One module serves both MCP transports, selected by configuration and mutually exclusive, with stdout reserved for the protocol in stdio mode

<!-- markdownlint-disable MD036 -->

**Title**
One module serves both MCP transports, selected by configuration and mutually exclusive, with
stdout reserved for the protocol in stdio mode

**Lens**: ARCH

**Status**: active

**Description**
A single Spring Boot module, `microservices/mcp-api`, builds one artifact that runs in one of two
transport modes, chosen by configuration rather than by shipping two artifacts:

- **stdio**, for a local agentic client: `spring.ai.mcp.server.stdio=true` with
  `spring.main.web-application-type=none`.
- **HTTP (streamable)**, for a hosted Snow-White: `spring.ai.mcp.server.protocol=STREAMABLE`,
  served by `spring-ai-starter-mcp-server-webmvc`.

The two are mutually exclusive: a process serves one transport or the other, never both.
The starter already enforces this itself — its HTTP autoconfiguration is conditional on stdio being
off — but the module does not rest the property on that, because it is the framework's internal
wiring rather than a contract, and this repository's Spring AI line is chosen for Spring Boot 4.1
compatibility rather than for that class staying where it is.
The module states the exclusivity as its own requirement and asserts it in configuration, so the
invariant survives the mechanism moving.

Because the choice is made by absence — the HTTP autoconfiguration applies when stdio is _off_ —
a stale `spring.ai.mcp.server.stdio=true` left in a shared or inherited configuration would
silently leave a deployed instance with no MCP endpoint at all, while its actuator health stayed
green because the web server itself is fine.
The deployed HTTP configuration therefore asserts stdio is off rather than assuming it, and fails
startup if it is not, so that misconfiguration surfaces as a failed deployment instead of a
server that answers probes and serves no tools.

In stdio mode, stdout carries only MCP protocol messages, per the MCP specification's requirement
that nothing else is written there.
Spring Boot's default console appender writes to stdout and
the startup banner is printed to stdout, so both are disabled for the stdio run mode: logging is
routed to stderr or to a file, and `spring.main.banner-mode` is not `console` (or the banner is
suppressed outright).
This constraint is specific to the stdio mode; the HTTP mode has no such
restriction, since its transport is the HTTP response body rather than the process's own stdout.

**Rationale**
Two transports are two ways of reaching the same read-only tool set, not two different
capabilities — a separate artifact per transport would duplicate the tool implementations, the
generated REST clients, and the allowlist test (`CON-014`) for no behavioral difference.
The starter's own conditional wiring already makes the choice exclusive, so building a second
artifact to enforce exclusivity the dependency already enforces would be redundant machinery
solving an already-solved problem — and asserting the property in the deployed configuration costs
one condition, against a second artifact's duplicated build, image and chart.

Reserving stdout for the protocol in stdio mode is not a style preference; it is the MCP
specification's own requirement, and violating it silently corrupts the protocol stream rather
than raising an error a developer could act on — a single unexpected log line to stdout is
indistinguishable, to the client's JSON-RPC parser, from a malformed protocol message.
The
failure mode this constraint prevents is a client wedged mid-session for a reason that looks
nothing like its cause.

Running stdio mode without a web server leaves it with no actuator surface at all, for the reason
`ARCH-018` records: `JmxAutoConfiguration` is gated on `spring.jmx.enabled`, which Spring Boot 4.1
leaves off, so there is not even a JMX fallback.
That is accepted here rather than worked around,
because `NF-001`'s gap is about a deployment whose probes need a health verdict, and nothing probes
a stdio process a developer's own agent client started and owns the lifetime of — it fails by
exiting, in front of the one consumer it has.
The HTTP mode is deployed and does need that surface,
but unlike the stream services `ARCH-018` governs it has a real business surface of its own, so it
follows the pattern the four API modules already use — a business port plus management on 8090 —
rather than `ARCH-018`'s management-port-only narrowing.
Which probes target it is settled by the
increment that adds the deployment, exactly as `ARCH-018` defers its own.

**Verification Description**
A context smoke test starts the module with stdio configuration (`web-application-type=none`,
`spring.ai.mcp.server.stdio=true`) and asserts the application context loads with no web server
bean present.
A context smoke test starts the module with HTTP configuration
(`spring.ai.mcp.server.protocol=STREAMABLE`) and asserts the application context loads with the
MCP HTTP endpoint registered.
A test asserts that same HTTP configuration with `spring.ai.mcp.server.stdio=true` added fails to
start, rather than starting without an MCP endpoint.
An `initialize` handshake test in each mode asserts a client can complete the MCP initialize
exchange and receive the server's capabilities.
A test launches the built artifact as a separate process in stdio mode, drives an `initialize`
exchange over its standard streams, and asserts every line that process wrote to stdout parses as
an MCP protocol frame — so the startup banner and the default console appender are both absent
from that capture.
This one is a process-level test rather than an in-JVM one: the logging system binds its appenders
to the real stdout at startup, so replacing `System.out` inside a test JVM afterwards misses
exactly the writes the constraint exists to catch.

## Relations

**Related**

- [SYS-015](SYS-015-agent-queryable-consumption-path.md) — the consumption path this
  module and its transports serve
- [ARCH-021](ARCH-021-mcp-surface-is-a-facade.md) — the façade decision implemented
  inside this module
- [CON-014](CON-014-mcp-tool-set-is-read-only.md) — the read-only invariant that holds
  in both transport modes, with no transport-specific exception
- [ARCH-018](ARCH-018-management-surface-without-a-business-one.md) — the management-surface
  pattern this module's HTTP mode departs from, having a business surface of its own
- [NF-001](NF-001-operational-observability.md) — the observability quality the deployed HTTP
  mode must reach, and that the stdio mode is out of reach of by construction
- [NF-011](NF-011-mcp-server-is-an-opt-in-deployment.md) — whether the HTTP mode is deployed at
  all, which is an operator's choice; the stdio mode is out of that setting's reach
