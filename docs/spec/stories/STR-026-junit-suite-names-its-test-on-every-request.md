# A JUnit-driven suite names its own test on every request it makes, without being instrumented

<!-- markdownlint-disable MD036 -->

**Title**
A JUnit-driven suite names its own test on every request it makes, without being instrumented

**Status**: active

**Business Value**
`STR-017` built the whole evidence path for a test identity and left the producing end empty.
A finding carries `testCaseName`, the report API publishes it, and the webapp renders it — but the
only thing in this repository that ever sets the value is one application test whose purpose is to
prove the mechanism (`TestIdentityBaggageAppTest`), by writing the `baggage` header out by hand.

That bill comes due with `#2082`.
Snow-White is about to score Snow-White, using its own Citrus application tests and its Playwright
suite as the traffic source.
Every finding in that first dogfooded report would name no test and fall back to a trace id, which
is the one thing a developer cannot act on: a trace id does not tell you which test to go and
extend.
The report would demonstrate the feature it is least able to show off.

Making a JUnit-driven suite name itself turns that report from "this target was exercised" into
"this target was exercised by `ReportResourceAppTest.shouldRejectUnknownGate`", for every suite in
the repository, without a test author remembering anything.
It is also the ergonomics half of `#2011`: the same extension is what a consumer's own Spring Boot
integration test uses to get named findings at unit-test speed.

**Problem / Context**
`ARCH-013` fixed how a test identity reaches a span: the runner puts `test.case.name` into
OpenTelemetry baggage, W3C `baggage` propagation carries it over the wire, and the system under
test copies that entry onto its own server span through agent configuration.
`SW-032` fixed the attribute and the opaque-label rules, and `semantic-convention/test.md`
documents both.
None of that is reopened here.
What is missing is the first step for a runner that is not itself instrumented, and that gap is
specific:

**Putting a value into the test JVM's OpenTelemetry baggage does not put it on the wire.**
This was verified in this session rather than assumed.
`examples/example-spring-boot`'s application-test classpath carries `citrus-http` over
`httpclient5` with no OpenTelemetry instrumentation on it, no
`micrometer-tracing-bridge-otel`, and no `-javaagent` or `argLine` on the Failsafe JVM in any of
the five modules that run application tests.
Nothing injects a `baggage` header.
`TestIdentityBaggageAppTest` sets the header literally, with a `format(...)` call, for exactly this
reason — and its Javadoc says so.
So the runner-side half of `ARCH-013` cannot be delivered by context propagation alone; the
identity has to be handed to the runner in a form its own HTTP client can send.

A first cut of the extension exists on this branch (`db38a371`,
`toolkit/citrus-junit-jupiter-extension`), registered through
`junit.jupiter.extensions.autodetection.enabled` in those same five modules.
Three things about it are load-bearing for this story, each verified by running it rather than by
reading it:

- **It throws before it does anything.**
  Its constructor calls `AutoConfiguredOpenTelemetrySdk.initialize()`, which defaults every
  exporter to `otlp`, and no module that auto-registers the extension has
  `opentelemetry-exporter-otlp` on its test classpath.
  Running it on that classpath produces
  `ConfigurationException: otel.metrics.exporter set to "otlp" but opentelemetry-exporter-otlp not
found on classpath`.
  With autodetection on, that is every application test in five modules failing at extension
  instantiation, for a reason that has nothing to do with any of them.
  It has gone unnoticed because application tests only run behind the `include:apptests` label.
- **It writes the identity only to baggage, so by the paragraph above it reaches nothing.**
  It also starts a span and never makes it current, so the span parents no request and arrives as
  one orphan trace per test.
- **It keeps its per-test state in two `static ThreadLocal` fields**, paired across
  `BeforeEachCallback` and `TestWatcher`.
  That is both a parallel-execution hazard and the reason the class cannot be unit-tested without
  leaking state between cases.

The same commit also sets `junit.jupiter.execution.parallel.enabled` in the root POM's Surefire
block as bare element text, where Surefire expects a `java.util.Properties`.
That breaks `./mvnw` for every module that runs Surefire
(`Cannot assign configuration entry 'properties' ...`) and is repaired here as a prerequisite, not
as part of the decision.

**Solution Approach**
The extension derives one value — the test's `test.case.name` — and publishes it through two
channels at once, because the two settings this has to serve are different (`ARCH-020`).
For a runner that is instrumented, or a whitebox test sharing the process with the service, it
becomes current OpenTelemetry baggage and needs nothing further.
For a runner that is not, it is readable as a ready-to-send header value **and** the extension ships
the `ClientHttpRequestInterceptor` that attaches it, so a suite wires its client once instead of
every test writing a header.
Snow-White's own suites are the second case.
Spring's interface is what makes that one class enough: Citrus's `HttpEndpointConfiguration` takes a
`List<ClientHttpRequestInterceptor>` and applies it to the `RestTemplate` it builds, so every
application test here is covered — Citrus's OpenAPI-generated actions included, since they ride the
same endpoint — without the extension knowing Citrus exists.

The value is the test's fully qualified method name, with the invocation distinguished where JUnit
distinguishes it, bounded so it can actually be stored (`SW-046`).
Each test also gets one span, made current for its duration and carrying its own identity as an
attribute, so an instrumented in-process caller nests under it and a human reading Tempo sees one
trace per test rather than a loose server span (`SW-047`).

Because the artifact is published and lands on a consumer's test classpath, its dependencies are
part of the deliverable rather than an implementation detail.
It pins `opentelemetry-bom` itself instead of drifting with whatever Spring Boot the reactor builds
against, marks the SDK autoconfigure dependency `optional` so a consumer that configures
OpenTelemetry its own way does not inherit one, and drops the two dependencies the first cut
declared and never used — Lombok, and an `opentelemetry-semconv` that cannot supply
`test.case.name` at all, since `test.*` is a Development-stability group and ships only in
`opentelemetry-semconv-incubating`.

The governing rule is that none of this may ever change a test's verdict (`CON-013`).
The extension is auto-registered for every test in a module, including unit tests that have no
OpenTelemetry anywhere; an absent SDK, an unconfigured exporter, an unreachable collector or a
failure inside any callback degrades to doing nothing.
That inverts what the current code does, and it is the difference between a piece of test
infrastructure and a liability.

**Acceptance Criteria**

- On a test classpath with no OpenTelemetry exporter and no `OTEL_*` configuration — the classpath
  of all five modules that auto-register the extension today — a test suite passes, and passes with
  the same verdicts it produces with the extension absent.
- With the extension registered, a test can read its own `test.case.name` as a value and as a
  `baggage` header fragment, and that value is the fully qualified `<package>.<Class>#<method>` name
  of the running test.
- Two invocations of the same `@ParameterizedTest` or `@RepeatedTest` method yield different
  values, and the value of a plain `@Test` contains no invocation suffix.
- A value that would exceed `FindingEvidence.MAX_TEST_CASE_NAME_BYTES` is not produced; the
  extension yields no identity rather than one that the storing side will drop.
- Within a test, `Baggage.current()` carries `test.case.name` with that same value, and after the
  test the entry is gone — asserted on the same thread the test ran on.
- Each test produces exactly one span, which is current for the body of the test, carries
  `test.case.name` as an attribute, and ends exactly once with `OK` for a passing test, `ERROR`
  plus a recorded exception for a failing one, and `UNSET` for an aborted or skipped one.
- The shipped interceptor adds the current identity as a `baggage` header to an outgoing request,
  appends to a `baggage` header the request already carries rather than replacing it, and adds
  nothing when there is no identity.
- A request made by a suite whose Citrus HTTP client carries that interceptor reaches a service
  configured per `ARCH-013` and produces a server span carrying that test's name — demonstrated
  against a real example application, not a mock.
- Each of `api-index-api`, `quality-gate-api` and `report-coordinator-api` sends the header from its
  application tests, wired once in that service's own `CitrusUtils.getHttpEndpoint`.
- `./mvnw package` succeeds from the repository root.

**Out of scope**

- **Reopening the convention.**
  `ARCH-013` owns how the identity reaches a span and `SW-032` owns the attribute and its
  opaque-label rules.
  This story consumes both and changes neither.
- **The webapp's Playwright suite.**
  It is TypeScript and constructs no Java HTTP client, so it needs its own way to set the header.
  The convention is the same and `semantic-convention/test.md` already documents it; the extension
  has nothing to offer there.
- **Turning the dogfooded report on.**
  Wiring the interceptor into the three REST services' `CitrusUtils` factories is in scope here and
  is what makes their application tests name themselves.
  Calibrating a threshold, choosing the in-scope APIs and making `#2082`'s check non-blocking are
  that issue's work, not this story's.
- **Instrumenting the test JVM.**
  Putting `-javaagent` or an OTLP exporter on a Failsafe JVM would make in-process propagation work
  and is a legitimate alternative for some consumer.
  It is not adopted here, because it makes every suite that wants named findings pay for an agent,
  and `ARCH-020` records why.
- **Calculating coverage from inside an in-process test**, and the per-test-versus-per-suite
  lookback question `#2011` raises.
  Those depend on a calculation trigger, not on a test identity.
- **The sibling upstream attributes.**
  `test.suite.name`, `test.case.result.status` and `test.suite.run.status` stay unread
  (`SW-032`) and therefore unwritten.
- **Actually running the unit tests concurrently.**
  The broken Surefire configuration that arrived with the extension is repaired and
  `junit.jupiter.execution.parallel.enabled` stays on, but it is inert on its own: the pinned
  engine defaults `junit.jupiter.execution.parallel.mode.default` to `same_thread`
  (`DefaultJupiterConfiguration:191`), so nothing runs concurrently until that is set too.
  Setting it is deliberately left to its own change, because this story introduces an extension
  that is auto-registered into every one of those tests and holds JVM-global state
  (`GlobalOpenTelemetry`); flipping both at once would make a failure ambiguous between the two.
  What this story does owe is that the extension be safe when that switch is thrown, which
  `SW-047` and `CON-013` require.
  Surefire here runs unit tests only — the root POM excludes `**/*AppTest.java` and `**/*IT.java`
  from it and gives `**/*IT.java` to Failsafe — so the switch never reaches a Spring-context,
  Testcontainers or WireMock test.

## Relations

**Realizes**

- [ARCH-020](../specs/ARCH-020-test-identity-published-not-propagated.md) — why the
  identity is handed to the runner instead of relying on in-process propagation
- [SW-046](../specs/SW-046-test-case-name-is-the-qualified-invocation.md) — the value the
  extension produces
- [SW-047](../specs/SW-047-one-current-span-per-test.md) — the span each test gets
- [CON-013](../specs/CON-013-extension-never-changes-a-test-verdict.md) — the rule that
  keeps test infrastructure from becoming a liability

**Related**

- [ARCH-013](../specs/ARCH-013-test-identity-travels-as-baggage.md) — the mechanism this story
  delivers the runner-side half of
- [SW-032](../specs/SW-032-test-identity-on-the-span.md) — the attribute and the opaque-label rules
  the produced value has to satisfy
- [STK-002](../specs/STK-002-black-box-tests-as-telemetry-source.md) — the black-box setting in
  which the runner and the span sit in different processes at all
- [STK-003](../specs/STK-003-no-app-code-changes-beyond-otel.md) — the promise that keeps this a
  test-side concern rather than a service dependency
- [STR-017](STR-017-api-test-findings-back-every-criterion-coverage.md) — the story that built the
  evidence path and explicitly left the producing end to `#2011`
- [SW-031](../specs/SW-031-findings-served-with-the-report.md) — how the value this story produces
  is served once it reaches a finding
- [NF-005](../specs/NF-005-clear-failure-feedback.md) — the feedback quality a trace id instead of
  a test name undermines
