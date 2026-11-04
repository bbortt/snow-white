# Response schema conformance

<!-- markdownlint-disable MD036 -->

**Title**
Response schema conformance

**Status**: planned

**Business Value**
Snow-White answers "was this operation exercised?".
It does not answer "did it respond the way its specification says it would?".
A test that calls `GET /pet/{id}`, receives `200`, and gets back a payload with a missing required
field or a string where the document promises an integer produces full coverage and a green gate —
the response code was observed, so every criterion that judges it is satisfied.
An API can therefore be fully covered and still not conform to its own contract, which is a hole in
the ground-truth claim rather than a missing convenience.
Closing it turns the report from "every documented reach was taken" into "every documented reach was
taken and the answers matched the document", without asking a consumer to write a single assertion
it is not already writing.

**Problem / Context**
All 14 `OpenApiCoverageCriteria` constants judge reach: which paths, methods, status codes,
parameters and content types were touched.
None inspects a payload against a schema, and no microservice contains schema validation of any
kind.

The validation nonetheless already happens, one layer out: Citrus's OpenAPI integration validates
responses against the generated specification in every application test in this repository, and
`OpenApiResponseValidator.validateResponseToReport` returns that outcome as a
`com.atlassian.oai.validator.report.ValidationReport` **without throwing**.
So the verdict exists in the test JVM and is simply reported nowhere a quality gate can see it:
today a schema violation either fails the test locally or passes unnoticed, and either way it never
reaches a coverage report.

The work is therefore a reporting path rather than a validator — but the path is longer than it
looks, in two places the issue describing this work got wrong:

- The seam is not `TestIdentityBaggageInterceptor`.
  That class sits on Spring's `ClientHttpRequestInterceptor` and sees a `ClientHttpResponse`;
  Citrus's verdict lives on Citrus's own `HttpMessage` inside its validation pipeline, reachable
  through its `META-INF/citrus/message/schemaValidator/` SPI.
  Harvesting at the interceptor would mean re-reading the response stream, needing
  `BufferingClientHttpRequestFactory`; harvesting at the SPI needs none of that.
- Nothing carries a test-JVM span into the analysed dataset today.
  The correlation query is keyed on `resource.service.name`, `span.api.name` and `span.api.version`,
  and the first of those is always present because `ApiInformation.serviceName` is non-null.
  The runner's own span (`SW-047`) carries `test.case.name` and no API identity, and by `ARCH-013`
  and `ARCH-020` the _application_ exports the span while the test merely publishes baggage.
  A harness-published verdict is only analysable if the harness exports under the identity the
  specification already declares for the API (`x-service-name`, `ARCH-015`).
  The SDK side of that is cheap — `AutoConfiguredOpenTelemetrySource` already autoconfigures a real
  SDK the moment any `otel.*` or `OTEL_*` setting is present — but the identity is new, and so is
  the fact that Snow-White would then analyse a span it did not receive from the application under
  test.

Validating inside the application is rejected up front and that rejection is part of the context,
not a decision this story takes: it would require the specification at runtime, do validation work
on every production request, and change application behaviour, against Snow-White's whole
positioning as something that observes without participating.

**Solution Approach**
Harvest, publish, judge — three steps, none of which re-implements a validator.

Harvest Citrus's own `ValidationReport` through its schema-validator SPI, in
`toolkit/citrus-junit-jupiter-extension`, where one small class already reaches every application
test in this repository.
Publish the outcome as span attributes on a span the harness exports under the API's declared
identity — a boolean verdict plus a JSON Pointer per violation, which is the finding identity model
Snow-White already uses (`SW-029`).
No request or response body ever leaves the test JVM.
Add one criterion, `RESPONSE_SCHEMA_CONFORMANCE`, that reads those attributes and judges an observed
response against the schema its document promised.

Two properties hold the shape together.
A harness-published span must not move any existing criterion's number, which is what keeps this
additive for every consumer that does not opt in, and is stated as an invariant rather than left to
whichever calculator happens not to look.
And a response with no verdict is a target the criterion does not judge (`SW-030`), not a
violation — but an API with no verdict at all fails the criterion and says why, because an operator
who configured the check and received nothing has a broken setup, not a conforming API.

**Acceptance Criteria**

- An application test whose response violates its documented schema produces a report in which
  `RESPONSE_SCHEMA_CONFORMANCE` is below 100%, and the finding names the operation and the JSON
  Pointer of the offending field.
- The same test with a conforming response produces 100% for that criterion.
- A response without a verdict is not judged: a suite that publishes verdicts for some responses is
  measured over those alone.
- A test suite that publishes no conformance verdict at all — a REST-assured suite with no
  producer, say — produces a report in which `RESPONSE_SCHEMA_CONFORMANCE` fails at 0%, never a
  vacuous 100%, and explains why: no verdict was found, how verdicts are produced, and a link to the
  producer contract.
  The UI shows that case as a translated notice in place of the coverage bar, the way it shows
  "Fully waived", linking the same documentation.
  It decides a gate only where an operator included the criterion.
- Harvesting the verdict never changes a test's outcome: a validation report Citrus treated as a
  pass stays a pass, one it treated as a failure stays a failure, and a harness that cannot
  publish at all (no SDK configured, export unreachable) still runs the test to its own verdict.
- A non-conforming `200` still counts as response-code coverage for its operation.
  The two criteria disagree about the same response on purpose: one says it was reached, the other
  says the answer was wrong.
- Telemetry identical but for the conformance attributes produces identical results for all 14
  existing criteria — the same statuses, the same ratios and the same findings — so adopting this
  moves no number a consumer is already gating on.
- The attributes are documented in `semantic-convention/` as attributes Snow-White defines, with
  their stability stated, alongside `api.name`, `api.version` and `openapi.operation.id`.
- That documentation states the full producer contract (`SW-049`) — span shape, identity
  placement, verdict readings — so that someone could implement a producer for a test framework
  other than Citrus without reading Snow-White's source, and the user-facing pages link to it.

**Out of scope**

- Any validation running inside the application, under any flag, and any claim about production
  traffic.
  This criterion says "the API conformed under test", never "the API conforms in production".
- Request validation.
  A test sending an invalid request and getting a `200` is a real server bug, but the test is the
  party at fault for the request, which muddies whose finding it is; it deserves its own story.
- Semantic assertions beyond the schema — business-rule correctness.
  The document is the contract here, nothing more.
- Response-time and performance assertions; #1523 owns that axis.
- Reaching consumers' own tests through `spring-web-autoconfiguration`.
  That is a different audience and possibly also worth doing, but this story earns the shape
  against this repository's tests first.
- Shipping producers for test frameworks other than Citrus.
  The contract they would implement is in scope and framework-neutral (`SW-049`); only the Citrus
  producer ships with this story.
  A consumer on a framework without a producer is unjudged, and `SW-030` is what makes that safe
  rather than silently wrong.

## Relations

**Realizes**

- [SW-048](../specs/SW-048-response-schema-conformance-criterion.md) — the criterion
  this story adds and what it judges
- [SW-049](../specs/SW-049-conformance-verdict-attributes.md) — the attributes the
  verdict travels on, and how they are read
- [ARCH-023](../specs/ARCH-023-conformance-verdict-is-harvested-not-recomputed.md) — where
  the verdict comes from
- [ARCH-024](../specs/ARCH-024-harness-publishes-under-the-api-identity.md) — how a
  test-JVM span becomes analysable at all
- [CON-015](../specs/CON-015-harness-span-never-moves-a-coverage-verdict.md) — the
  invariant that keeps this additive

**Related**

- [SW-029](../specs/SW-029-finding-identified-by-spec-pointer.md) — the pointer model the
  violation reuses as its finding identity
- [SW-030](../specs/SW-030-unjudged-target-is-not-applicable.md) — what becomes of a response no
  harness judged
- [SW-003](../specs/SW-003-undocumented-response-code-detection.md) — the telemetry-driven
  criteria a harness span must not reach
- [SW-032](../specs/SW-032-test-identity-on-the-span.md) — the precedent for reading a
  test-produced attribute as an opaque label that correlates on nothing
- [SW-047](../specs/SW-047-one-current-span-per-test.md) — the runner's existing span, and why it
  is not analysable as it stands
- [ARCH-013](../specs/ARCH-013-test-identity-travels-as-baggage.md) — the publication model this
  story departs from, by exporting rather than only propagating
- [ARCH-020](../specs/ARCH-020-test-identity-published-not-propagated.md) — the same boundary,
  stated from the harness side
- [ARCH-015](../specs/ARCH-015-identity-declared-in-the-specification.md) — the declared service
  identity that makes the harness's export correlate without a new key
- [CON-013](../specs/CON-013-extension-never-changes-a-test-verdict.md) — the existing guarantee
  the harvest step must not break
- [STR-006](STR-006-result-consumption.md) — the reports this criterion appears in unchanged
