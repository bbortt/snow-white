# Test

## Test Attributes

This group describes attributes identifying the test that exercised an API, so that a finding can name it.

Snow-White **adopts** these attributes from the [OpenTelemetry semantic conventions' test registry](https://opentelemetry.io/docs/specs/semconv/registry/attributes/test/) rather than defining them.
It only ever reads them: any test harness, CI vendor or instrumentation library already emitting `test.case.name` is understood without a mapping step.

| Attribute                                                          | Type   | Description                                                                                   | Examples                                                                         | Stability                                                      |
| ------------------------------------------------------------------ | ------ | --------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| <a id="test-case-name" href="#test-case-name">`test.case.name`</a> | String | Fully qualified human-readable name of the test case that exercised the API this span serves. | `org.example.PetstoreIT.shouldRejectUnknownPet`, `example/tests/TestCase1.test1` | ![Development](https://img.shields.io/badge/-development-blue) |

The value is read as an opaque label: never parsed into suite and case, never truncated, never lowercased, never split, and never used as a correlation key.
A span carrying it and a span without it are matched identically, so enabling the convention changes what a report _says_ and never what it _scores_.
Absent, blank and whitespace-only all mean the same thing - no test identity - and consumers fall back to the trace id.

Emitting the attribute is worthwhile before Snow-White reports it: the convention is fixed here so that a suite adopting it now needs no change once findings carry the name.

`test.case.result.status`, `test.suite.name` and `test.suite.run.status` - the rest of the upstream group - are deliberately not read.

## Getting the attribute onto a span

The name lives in the test runner; the span is created by the system under test.
The test runner puts the name into [OpenTelemetry baggage](https://www.w3.org/TR/baggage/), which travels with the trace context, and the system under test copies that baggage entry onto its own server span.

Set one HTTP header per request from the test runner:

```http
baggage: test.case.name=org.example.PetstoreIT.shouldRejectUnknownPet
```

For a service instrumented by the OpenTelemetry Java agent, the copy is configuration and no application code:

```properties
otel.java.experimental.span-attributes.copy-from-baggage.include=test.case.name
```

or, as an environment variable:

```shell
OTEL_JAVA_EXPERIMENTAL_SPAN_ATTRIBUTES_COPY_FROM_BAGGAGE_INCLUDE=test.case.name
```

Only the listed baggage keys are copied, so unrelated baggage a request happens to carry never reaches the span.
See [`examples/example-spring-boot`](../examples/example-spring-boot) for a working setup.
