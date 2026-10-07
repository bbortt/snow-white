# Kafka drives OpenAPI coverage calculation as an asynchronous request/response exchange

<!-- markdownlint-disable MD036 -->

**Title**
Kafka drives OpenAPI coverage calculation as an asynchronous request/response exchange

**Lens**: SW

**Status**: active

**Description**
`openapi-coverage-stream` has no synchronous entry point of its own — it is driven entirely by a
Kafka Streams topology acting as an asynchronous request/response pair.
It consumes `QualityGateCalculationRequestEvent` messages from the configured calculation-request
topic (`snow-white-calculation-request`) and filters the stream to `ApiType.OPENAPI` — the topic
carries requests for `ApiType`'s other, currently unimplemented formats too (`ASYNCAPI`, `GRAPHQL`),
so the filter scopes this processor to its own concern on a format-agnostic topic rather than
assuming every message on it is one this processor should handle.
For each matching message it resolves the indexed OpenAPI specification, correlates it with
telemetry from the configured backend, calculates coverage, and publishes one
`OpenApiCoverageResponseEvent` per request to the calculation-response topic
(`snow-white-openapi-calculation-response`).
The request's key is preserved through to the response, so the original requester (Report
Coordinator) correlates a response back to its request without a separate reply-address mechanism.

A failure during processing never leaves a request without a response: a telemetry-backend outage
(`TelemetryBackendUnavailableException`) and any other processing exception are both caught and
turned into a response event carrying an error message, rather than propagating and stalling the
stream or dropping the request.

**Rationale**
Coverage calculation can take considerable time over large datasets or long lookback windows;
Kafka's request/response topic pair keeps this work off the Report Coordinator's synchronous HTTP
request path, so a slow calculation cannot cause a gateway timeout, and this stream processor scales
independently of the services that request calculations (`pages/_pages/architecture.md`).
Because the caller is a Kafka producer rather than an HTTP client, there is no request-scoped
connection to fail the caller through — every code path here must resolve to a published response
event, which is why the outer exception handling turns even an unexpected failure into a response
rather than letting the record be skipped silently.

**Verification Description**
A Citrus black-box test publishes a `QualityGateCalculationRequestEvent` for an OpenAPI-typed
request to the calculation-request topic and asserts exactly one `OpenApiCoverageResponseEvent`
carrying the same key is published to the calculation-response topic; a second run publishes a
non-OpenAPI-typed request and asserts nothing is published by this service.
A third run induces a telemetry-backend failure and asserts a response event with a non-null error
message is still published, rather than the request being left unanswered.

## Relations

**Realizes**

- [SYS-004](SYS-004-telemetry-ingestion-and-correlation.md) — the ingestion and
  correlation this processor drives

**Related**

- [NF-003](NF-003-scalability-without-redesign.md) — the async split this component is
  the concrete instance of
- [CON-002](CON-002-tolerate-dependency-outages.md) — the outage-tolerance guarantee
  this processor's exception handling implements
- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the backend this
  processor queries without knowing which one is active
