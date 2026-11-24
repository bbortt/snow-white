# Citrus JUnit Jupiter Extension

Makes a JUnit-driven test suite name itself on every HTTP request it issues, so the findings in a Snow-White report
read `com.example.OrderResourceIT#shouldRejectUnknownSku` instead of a trace id.

> **Use at your own risk.**
> This module is published so that suites outside this repository can use it, but it is deliberately kept off the
> documentation site and carries no compatibility promise.
> It exists first to serve Snow-White's own application tests, and its API will move when those need it to.
> Read [Limits](#limits) before you depend on it.

## What it does

A test's identity is published on two channels for the duration of that test:

- an OpenTelemetry **baggage entry**, `test.case.name`, which an instrumented in-process caller picks up for free;
- one **span** per test, current for the test's duration, carrying the same identity and the test's verdict.

Nothing here requires an OpenTelemetry SDK.
Baggage is part of `opentelemetry-api`, so a suite gets named requests with no SDK, no exporter and no agent on its
test classpath.

## Install

The artifact is published to GitHub Packages, not Maven Central.

```xml
<repositories>
  <repository>
    <id>snow-white</id>
    <url>https://maven.pkg.github.com/bbortt/snow-white</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>io.github.bbortt.snow-white.toolkit</groupId>
    <artifactId>citrus-junit-jupiter-extension</artifactId>
    <version>${snow-white.version}</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

GitHub's Maven registry requires authentication to download even from a public repository, so the dependency does not
resolve until `~/.m2/settings.xml` carries a matching server entry with a `read:packages` token.
The `<server><id>` must equal the `<repository><id>` above — `snow-white` here.
Reusing this repository's own `distributionManagement` id, `github`, is the natural guess and produces a 401 that
reads like a token-scope problem.

`junit-jupiter-api` is `provided`, so your suite's own JUnit version wins.
Nothing below comes with the artifact.
Two of them are declared `<optional>` on it and the third is not a dependency of it at all; you add whichever the
features you want require:

| Dependency                                                         | Needed for                                                |
| ------------------------------------------------------------------ | --------------------------------------------------------- |
| `org.springframework:spring-web`                                   | `TestIdentityBaggageInterceptor` (attaching to a request) |
| `io.opentelemetry:opentelemetry-sdk-extension-autoconfigure`       | recording test spans through an autoconfigured SDK        |
| an `io.opentelemetry` exporter, e.g. `opentelemetry-exporter-otlp` | the autoconfigured SDK to start at all                    |

The exporter is not optional once you add the SDK.
`AutoConfiguredOpenTelemetrySdk` defaults every exporter to `otlp`, so an SDK on a classpath with no matching exporter
throws `ConfigurationException`, which this extension swallows to a single `WARN` — leaving you with a configured
suite and zero spans.
This repository's own application tests add `opentelemetry-exporter-otlp` beside the autoconfigure artifact for
exactly that reason.

## Register the extension

Either let the service loader find it — the artifact ships a
`META-INF/services/org.junit.jupiter.api.extension.Extension` entry — by putting this in
`src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.extensions.autodetection.enabled = true
```

Or register it per class, if you would rather not turn autodetection on for the whole module:

```java
@ExtendWith(SnowWhiteOpenTelemetryExtension.class)
class OrderResourceIT {
  /* ... */
}
```

Autodetection registers this extension for **every** test in the module, including those that have no OpenTelemetry
anywhere.
For this extension that is intended and safe — see
[It never changes a test's verdict](#it-never-changes-a-tests-verdict).

The switch is not scoped to this extension, though: it activates every JUnit extension any library on your test
classpath declares through the same service-loader entry.
If you do not know what else is on that classpath, prefer `@ExtendWith`.

## Attach the identity to outgoing requests

Publishing the identity does not put it on the wire.
A test client that carries no OpenTelemetry instrumentation — which is the normal case for Citrus over
`httpclient5` — has to be told to send it.

For any client built on Spring's `ClientHttpRequestInterceptor`, wire the interceptor once where the client is
constructed:

```java
var endpointConfiguration = new HttpEndpointConfiguration();

endpointConfiguration.setClientInterceptors(
  List.of(new TestIdentityBaggageInterceptor())
);
```

Citrus's `HttpEndpointConfiguration` applies those interceptors to the `RestTemplate` it builds, so a Citrus endpoint
and a plain `RestTemplate` are wired identically, and Citrus's OpenAPI-generated actions ride the same endpoint and
need nothing of their own.
The interceptor adds nothing when no test identity is current, so wiring it unconditionally is safe — including on a
client used outside a test.
It appends to a `baggage` header the request already carries rather than replacing it.

For any other client, read the value yourself:

```java
SnowWhiteTestIdentity.current(); // Optional<String>, the raw identity

SnowWhiteTestIdentity.currentBaggageHeader(); // Optional<String>, a ready-to-send `baggage` header value
```

Use `currentBaggageHeader()` rather than formatting the header by hand.
Encoding goes through OpenTelemetry's own `W3CBaggagePropagator`, so a class named in a non-ASCII script travels
correctly instead of producing a header the receiving end silently drops.
That propagator percent-encodes `#`, so the header reads `test.case.name=com.example.OrderResourceIT%23shouldReject`.
This is correct — the receiving propagator decodes it, and the span carries the unescaped name.

## Configure the service under test

The identity reaches a report only if the service being tested copies the baggage entry onto its own server span.
That is configuration you own, not something this artifact can do for you:

```properties
otel.java.experimental.span-attributes.copy-from-baggage.include=test.case.name
```

This property is read by the **OpenTelemetry Java agent** only.
A service instrumented by `opentelemetry-spring-boot-starter` alone ignores it silently, and every finding falls back
to a trace id with nothing anywhere reporting why.

See [`semantic-convention/test.md`](../../semantic-convention/test.md) for the attribute and the full round trip.

## Test spans

Spans are a bonus on top of the identity, and the extension never starts an SDK your suite did not ask for.
It resolves one, lazily and at most once, in this order:

1. an `OpenTelemetry` already registered globally — something else configured it, so that one is used;
2. nothing configured at all — a no-op, which is the ordinary state of most tests and costs nothing;
3. something configured, meaning any `otel.*` system property or `OTEL_*` environment variable is set — an SDK is
   autoconfigured from it.

Checking for configuration before autoconfiguring is what keeps case 2 free.

Each test span is `INTERNAL`, named after the test, and carries `test.case.name`.
Its status is `OK` for a pass, `ERROR` with the exception recorded for a failure, and `UNSET` for an aborted test —
a failed assumption means the test declined to judge, and recording that as an error would make the span claim the
service misbehaved when the suite merely skipped the case.

## The identity

The value is `<fully qualified class>#<method>`, plus `[<n>]` where JUnit distinguishes one invocation of a method
from another (`@ParameterizedTest`, `@RepeatedTest`, any `@TestTemplate`).
Qualifying it matters: two tests resolving to one identity is worse than none, because the reading side is forbidden
from parsing the value to disambiguate it.

A name longer than 1024 UTF-8 bytes is **dropped entirely** rather than truncated, and the finding falls back to its
trace id.
No identity is an ordinary outcome and never an error.

## It never changes a test's verdict

An absent SDK, an unconfigured exporter, an unreachable collector or a bug in this extension costs you telemetry and
nothing else.
`Exception` and `LinkageError` are caught and logged; an `Error` that is not a `LinkageError` is not this extension's
business and propagates.

Log levels are deliberate:

- **DEBUG** — nothing configured, or no identity could be derived.
  The normal state of most tests, and noise otherwise.
- **WARN** — OpenTelemetry _was_ configured and did not work.
  If you wired up a suite and see no spans, look here.

Per-test state lives in the `ExtensionContext.Store` that JUnit already scopes to the test, never in a field or a
`ThreadLocal`, so two tests in flight on two threads cannot see each other's span.

## Limits

- **No compatibility promise.** The module is published but intentionally undocumented on the site; treat its API as
  internal to Snow-White and pin a version.
- **Half the round trip is yours.** Without `copy-from-baggage` on the service under test, the identity never reaches
  a span, and this artifact cannot detect that for you.
- **The interceptor needs Spring.** Any other HTTP client has to attach the header itself from
  `SnowWhiteTestIdentity`.
- **Licensed under the [PolyForm Small Business License 1.0.0](../../LICENSE)**, like the rest of this repository.
  That is not a permissive open-source licence — check it covers your use.

## Design

The decisions behind this module, and the reasons for them:

- [ARCH-013](../../docs/spec/specs/ARCH-013-test-identity-travels-as-baggage.md) — how a test identity reaches a span
- [ARCH-020](../../docs/spec/specs/ARCH-020-test-identity-published-not-propagated.md) — why the identity is published
  and attached rather than propagated
- [SW-046](../../docs/spec/specs/SW-046-test-case-name-is-the-qualified-invocation.md) — the naming rule
- [SW-047](../../docs/spec/specs/SW-047-one-current-span-per-test.md) — one current span per test
- [CON-013](../../docs/spec/specs/CON-013-extension-never-changes-a-test-verdict.md) — the never-fail-a-test constraint
