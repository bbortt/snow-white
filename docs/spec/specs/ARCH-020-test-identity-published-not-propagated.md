# A test identity is published for the runner to send, never propagated out of the test process

<!-- markdownlint-disable MD036 -->

**Title**
A test identity is published for the runner to send, never propagated out of the test process

**Lens**: ARCH

**Status**: active

**Description**
`ARCH-013` gives the runner one job: put `test.case.name` into OpenTelemetry baggage, so W3C
`baggage` propagation carries it to the service.
That job has a hidden precondition — the runner's HTTP client must be OpenTelemetry-instrumented,
or the baggage entry never becomes a header.
A JUnit suite is usually not instrumented, and Snow-White's own suites are not.

So the extension publishes the identity through **two** channels, and treats neither as the
primary:

- **Current baggage.**
  For the duration of a test, `test.case.name` is an entry in `Baggage.current()`.
  An in-process caller that _is_ instrumented — a whitebox Spring Boot test sharing the JVM with
  the service, which is `#2011`'s setting — needs nothing else, and gets propagation for free.
- **A readable value.**
  The same name is retrievable as a plain string and as a ready-to-send `baggage` header fragment,
  without the caller holding an `ExtensionContext` or knowing JUnit's API.
  A suite whose client is not instrumented attaches that fragment to its client **once** and every
  test it runs is then named.

The second channel is **derived from the first**, not stored beside it: the readable value is
whatever `test.case.name` the current baggage holds.
There is therefore one source of truth and no second piece of state to keep in step, the accessor
is correct for any thread that inherited the context, and a caller that sets the entry itself —
without this extension — is served identically.

A published value that nobody attaches to a request delivers nothing, so the extension also ships
the attaching half: **a `ClientHttpRequestInterceptor` that adds the current identity as a
`baggage` header.**
It is expressed at Spring's interface, not at any one test framework's.
That is what makes one small class cover every HTTP client in reach here — Citrus's
`HttpEndpointConfiguration` takes a `List<ClientHttpRequestInterceptor>` and applies it to the
`RestTemplate` it builds, so a Citrus endpoint and a plain `RestTemplate` are wired the same way,
and Citrus's OpenAPI-generated actions ride the same endpoint and need nothing of their own.
The interceptor adds nothing when there is no current identity, so a suite can wire it
unconditionally.
`spring-web` is `optional` on the artifact: a consumer whose client is something else gets the
accessor and no Spring on its test classpath.

Three things this explicitly rules out:

- **Instrumenting the test JVM** so in-process propagation works — an OpenTelemetry Java agent or
  an OTLP exporter on the Surefire/Failsafe command line.
  It would make the baggage channel sufficient on its own, and it is a legitimate choice for a
  consumer who already runs an agent.
  It is not what the extension requires, because it makes every suite that wants named findings pay
  for agent attachment, agent startup on every forked JVM, and an export path the suite otherwise
  has no use for.
- **A Citrus-specific artifact or dependency.**
  Every application test in this repository runs on Citrus, so a Citrus-typed interceptor would
  work — and would be strictly narrower than the Spring interface Citrus itself accepts, for no
  gain.
  The extension therefore knows nothing about Citrus, and the webapp's Playwright suite stays
  reachable by the same convention over a header it sets itself, with no Java artifact involved.
- **A Snow-White-specific channel.**
  The published fragment is a W3C `baggage` header value and nothing else, so what a suite sends is
  the same thing `ARCH-013` already documents and the same thing the agent already copies.
  Wiring the interceptor stays an explicit act by whoever constructs the client — nothing is
  installed into a consumer's transport behind its back.

**Rationale**
The gap this closes is a verified fact about the runner, not a preference.
`examples/example-spring-boot`'s application-test classpath carries `citrus-http` over
`httpclient5` with no OpenTelemetry instrumentation, no `micrometer-tracing-bridge-otel`, and no
`-javaagent` or `argLine` on its Failsafe JVM — confirmed by resolving that classpath in this
session, on 2026-10-07.
`TestIdentityBaggageAppTest`, the test that exists to prove `ARCH-013`, writes the `baggage` header
with a `format(...)` call for exactly this reason, and says so in its own Javadoc.
A design that set baggage and stopped would therefore produce nothing at all, while looking
correct at every individual step.

Publishing both channels rather than choosing one is what makes a single extension serve the two
settings Snow-White actually has.
Black-box application tests (`STK-002`) drive a container over the network and need the header;
whitebox in-process tests share a `Context` with the service and need the baggage.
Picking either alone would leave one of them with a bespoke mechanism, and `#2011` and `#2082` are
both in flight.

Shipping the interceptor at Spring's interface rather than abstaining is the correction this spec
makes to its own first draft.
Publishing a value and leaving the attaching to "whoever owns the client" reads like clean
separation and is actually the same defect the hand-written header already has: if no component
owns that step, nothing happens, and the mechanism works at every individual stage while producing
nothing end to end.
The concern that motivated abstaining — not marrying the extension to one client stack — is better
served by `ClientHttpRequestInterceptor` than by silence.
It is Spring's own abstraction, Citrus accepts it by that type and applies it to the `RestTemplate`
it builds, so one class reaches every application test in this repository, Citrus's
OpenAPI-generated actions included, plus any `RestTemplate` consumer, while naming Citrus nowhere.
Marking `spring-web` optional keeps the cost where the benefit is.

This is `STK-003`'s argument applied to the test side: the integration surface stays
OpenTelemetry's, not Snow-White's.
A suite adopts this with one interceptor on the client it already constructs — in this repository,
five such sites, three of them the single `CitrusUtils.getHttpEndpoint` factory each REST service
already funnels its application tests through.

**Verification Description**
A test asserts that, inside a running test, `Baggage.current()` carries `test.case.name` with that
test's own identity, and that the entry is gone once the test has finished.
A test asserts the published value and the published header fragment describe the same identity,
and that the fragment parses as a W3C `baggage` member whose key is `test.case.name`.
A test asserts the accessor reads the entry a caller set itself, with no extension registered,
which is what "derived, not stored" means observably.
A test asserts the interceptor adds a `baggage` header carrying the current identity to an outgoing
request, that it appends to a `baggage` header the request already has rather than replacing it,
and that it adds no header at all when there is no current identity.
An application test wires one example application's Citrus HTTP client with the interceptor instead
of a literal header, drives it, and asserts the exported server span carries that test's name —
which demonstrates the channel against a service configured only per `ARCH-013`, with no
application code and no agent in the test JVM.
A review confirms the artifact names no test framework and no HTTP client beyond Spring's
`ClientHttpRequestInterceptor`, and that `spring-web` is declared optional.

## Relations

**Realizes**

- [STK-002](STK-002-black-box-tests-as-telemetry-source.md) — the black-box setting whose runner
  and span sit in different processes

**Related**

- [ARCH-013](ARCH-013-test-identity-travels-as-baggage.md) — the decision this supplies the
  runner-side half of, and whose agent-side copy stays untouched
- [SW-032](SW-032-test-identity-on-the-span.md) — the attribute the published value lands in
- [SW-046](SW-046-test-case-name-is-the-qualified-invocation.md) — the value both channels
  carry
- [SW-047](SW-047-one-current-span-per-test.md) — the span the baggage channel scopes
  itself to
- [CON-013](CON-013-extension-never-changes-a-test-verdict.md) — the rule that holds when
  neither channel can be established
- [STK-003](STK-003-no-app-code-changes-beyond-otel.md) — the keep-it-to-OpenTelemetry promise this
  extends from the service side to the test side

## Changes

- **2026-10-07** — Set active: implementation of `STR-026` began.
