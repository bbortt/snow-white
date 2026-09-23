# A test's identity reaches the server span as OpenTelemetry baggage, never by a second lookup

<!-- markdownlint-disable MD036 -->

**Title**
A test's identity reaches the server span as OpenTelemetry baggage, never by a second lookup

**Lens**: ARCH

**Status**: planned

**Description**
The test that exercised an API and the span Snow-White correlates against are produced by two
different processes: the identity lives in the test runner, the span is created by the system under
test.
Snow-White closes that gap in one direction only — the test runner puts its identity into
OpenTelemetry baggage, W3C `baggage` propagation carries it over the wire with the trace context,
and the system under test copies the baggage entry onto its own server span as an attribute
(`SW-032`).

For a service instrumented by the OpenTelemetry Java agent, that copy is configuration and no code:

```properties
otel.java.experimental.span-attributes.copy-from-baggage.include=test.case.name
```

Snow-White then reads the attribute from the very span it already fetched, inside the calculator,
at the match (`ARCH-011`).
The attribute key is operator-configurable on `openapi-coverage-stream`, following the existing
`OpenApiCoverageStreamProperties.operationIdAttribute` precedent, and joins the required
attribute-key set (`SW-021`) so the narrowed Tempo `select()` and InfluxDB projection actually
return it.

Three things this explicitly rules out:

- **Resolving the identity from the trace.** Fetching an evidencing trace in full to find a sibling
  span that names the test is a second telemetry query per trace, against `NF-006`'s fetch bound,
  and it only works where the test runner is itself instrumented into the same trace — which
  Citrus, `curl`, k6 and a browser driver are not by default.
- **A Snow-White-specific header.** A bespoke `X-Snow-White-Test` read by
  `toolkit/spring-web-autoconfiguration` would work for a Spring MVC service and for nothing else,
  and it would make the integration surface a Snow-White protocol rather than OpenTelemetry
  (`STK-003`).
- **Deriving a name in the UI.** There is nothing to derive from: a trace id is not a test name, and
  the only process that ever knew the name is the one that has already exited.

**Rationale**
Baggage is the ecosystem's answer to exactly this problem — a value that must travel with a request
across a process boundary and be attachable to spans downstream — and it is on by default: the
OpenTelemetry SDK's default propagator set is `tracecontext,baggage`, so a test runner already
speaking OTLP needs to set an entry, not enable a mechanism.
A test runner that is not instrumented at all needs one HTTP header.

Keeping the copy on the system-under-test side, via agent configuration, is what preserves
`STK-003`: adopting this costs a consuming service an environment variable and costs it no
Snow-White dependency, no interceptor and no code change.
That also makes the mechanism uniform across the two settings that matter — a black-box suite
driving a deployed service (`STK-002`) and the whitebox, in-process usage `#2011` is about, where
the same baggage entry is set from a JUnit extension against an in-process SDK.
`#2011` therefore consumes this decision rather than owning it; if the convention were deferred to
it, every criterion's matching logic would have to be reopened afterwards to thread an attribute
through, which is the expensive part of `STR-017` being paid twice.

Reading the attribute where the spans are held is not a preference either.
`openapi-coverage-stream`'s calculators are the only place in the system that ever holds a span
next to the target it satisfied; downstream of them a finding is a row, and the span is gone.
This is the same argument `ARCH-011` makes for evidence in general, applied to the one field on an
evidence entry that is not derivable from the correlation itself.

The cost is honest: a test identity only appears where the consumer opts in on both ends — the
runner sets baggage, the service copies it.
Snow-White cannot make a span carry a name that nobody put there, so `testCaseName` is nullable
forever, and every consumer of it falls back to the trace id.
That is the same bargain `@SnowWhiteInformation` already asks for, and the alternative — inferring
identity — produces evidence that is plausible rather than true, which `ARCH-011` rejects on
principle.

**Verification Description**
An application test drives `examples/example-spring-boot` — instrumented by the OpenTelemetry Java
agent with `otel.java.experimental.span-attributes.copy-from-baggage.include` set — with an HTTP
request carrying a `baggage: test.case.name=<name>` header, and asserts the exported server span
carries the configured attribute with that value, having added no application code to the example.
A negative test asserts a request without the header produces a span without the attribute, and
that the calculation over it yields evidence with `testCaseName` null rather than failing.
A test asserts the configured attribute key is present in the required attribute-key set handed to
the telemetry backend (`SW-021`), and therefore in the Tempo `select()` clause and the InfluxDB
projection, for every calculation.

## Relations

**Related**

- [SW-032](SW-032-test-identity-on-the-span.md) — the attribute and value convention this
  decision delivers a span
- [ARCH-011](ARCH-011-evidence-captured-at-the-match.md) — the match this attribute is read at,
  and the evidence entry it lands on
- [SW-021](SW-021-required-attribute-key-set-derivation.md) — the required-key enumeration the
  configured attribute joins; amended for it on 2026-09-23
- [ARCH-007](ARCH-007-required-attribute-keys-computed-once-by-caller.md) — the single place that
  computation happens, and the staleness trade-off this new key is the first real instance of
- [NF-006](NF-006-bounded-telemetry-fetch-footprint.md) — the fetch bound that rules out
  per-trace lookup as the alternative mechanism
- [STK-003](STK-003-no-app-code-changes-beyond-otel.md) — the no-code-changes promise agent
  configuration keeps and a bespoke header would not
- [STK-002](STK-002-black-box-tests-as-telemetry-source.md) — the black-box setting in which the
  runner and the span sit in different processes at all
- [SYS-004](SYS-004-telemetry-ingestion-and-correlation.md) — the ingestion path the attribute
  travels with the rest of a span's attributes
