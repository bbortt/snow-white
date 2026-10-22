# The extension never changes a test's verdict, whatever the telemetry configuration does

<!-- markdownlint-disable MD036 -->

**Title**
The extension never changes a test's verdict, whatever the telemetry configuration does

**Lens**: CON

**Status**: active

**Description**
A suite's verdicts must be identical with the extension registered and with it absent.
Nothing about the telemetry configuration, and no failure inside the extension, may fail, abort,
skip, or otherwise alter a test.

The rule binds at four points, each of which can realistically go wrong:

- **Acquiring an OpenTelemetry instance.**
  No SDK, no configured exporter, an exporter named in configuration whose artifact is not on the
  classpath, an invalid `OTEL_*` value — each yields a no-op `OpenTelemetry` and a registered
  extension that does nothing.
  It never propagates a configuration error.
- **Exporting.**
  An unreachable collector, a refused connection, a full queue, or an export that times out is the
  exporter's problem and is never surfaced as a test outcome.
- **Any callback.**
  A throwable raised inside a before- or after-test callback is swallowed, so a bug in the
  extension costs telemetry rather than a red suite.
- **Acquisition happening at most once, and lazily enough to be skippable.**
  A suite that has no telemetry configured must not pay SDK start-up on every forked JVM, and a
  failed acquisition must not be retried per test.

A failure at any of these points is reported once, as a log record, naming what could not be done.
It is logged at `WARN` when something was configured and did not work, and at `DEBUG` when nothing
was configured at all — the second is the ordinary state of every unit test in this repository and
is not a problem to report.
Silence is not acceptable in either case: a suite that expected named findings and got none needs
somewhere to look.

**Rationale**
The extension is registered through `junit.jupiter.extensions.autodetection.enabled`, which is a
blunt instrument: it registers for **every** test in the module, including the thousands of plain
unit tests that have no OpenTelemetry anywhere and want none.
That is the right trade for the goal — no test author has to remember anything — but it only holds
if the extension is inert by default.
Autodetection without this constraint means one unconfigured exporter takes a module's whole suite
down, for a reason that has nothing to do with any test in it.

This is not hypothetical, which is why it is a constraint rather than a note.
The first cut of the extension called `AutoConfiguredOpenTelemetrySdk.initialize()` from its
constructor.
That defaults every exporter to `otlp`, and no module in this repository that auto-registers the
extension has `opentelemetry-exporter-otlp` on its test classpath.
Running it on that classpath, in this session on 2026-10-07, produced
`ConfigurationException: otel.metrics.exporter set to "otlp" but opentelemetry-exporter-otlp not
found on classpath` — which with autodetection on is every application test in five modules
failing at extension instantiation.
It went unnoticed because application tests only run behind the `include:apptests` label, so the
blast radius was real and the signal was absent.

A test's verdict is the one output a test framework must not corrupt.
Everything else here — a name, a span, a trace — is diagnostics, and diagnostics that can fail the
thing they are diagnosing invert the value of the tool.
This is the same bargain `CON-002` strikes for the services, applied where the cost of breaking it
is highest: a developer debugging why their suite went red has no reason to suspect a tracing
extension, and the error they are shown names an artifact they never asked for.

Logging the degraded cases at two different levels is what keeps the rule from hiding a real
misconfiguration.
"Nothing configured" is the normal state of most of this repository's tests and would be pure noise
at `WARN`; "configured and broken" is exactly what someone wiring up `#2082` needs to see on the
first run.

**Verification Description**
A test runs a suite of passing, failing, and aborted tests through a real JUnit engine with the
extension registered and with no OpenTelemetry configuration whatsoever, and asserts the verdicts
are identical to the same suite run without the extension — the same count of each outcome, and
the same throwable from the failing one.
A test asserts the same on a classpath where configuration names an exporter whose implementation
is absent, which is the condition that actually broke, and asserts the extension's no-op path is
taken rather than an exception escaping.
A test asserts a throwable raised from inside the extension's own before- and after-test callbacks
does not reach the test outcome.
A test asserts an exporter that throws on export does not reach the test outcome.
A test asserts acquisition is attempted once across many tests, and once more never after it has
failed.
A test asserts the unconfigured case logs at `DEBUG` and the misconfigured case at `WARN`, and that
each logs once rather than per test.
A test asserts acquisition is attempted once when many threads reach it at the same time, so the
at-most-once guarantee is a property of the code and not of sequential execution.

## Relations

**Related**

- [ARCH-020](ARCH-020-test-identity-published-not-propagated.md) — the two channels that
  simply publish nothing when this rule engages
- [SW-046](SW-046-test-case-name-is-the-qualified-invocation.md) — the absent identity this
  rule makes an ordinary outcome
- [SW-047](SW-047-one-current-span-per-test.md) — the span that is not produced when no
  tracer could be acquired
- [CON-002](CON-002-tolerate-dependency-outages.md) — the same degrade-rather-than-fail bargain the
  services strike for their own dependencies
- [NF-005](NF-005-clear-failure-feedback.md) — the clarity the two log levels exist to preserve
  when a suite silently produces no identity

## Changes

- **2026-10-07** — Set active: implementation of `STR-026` began.
