# The conformance verdict is harvested from the test framework's own report, never recomputed

<!-- markdownlint-disable MD036 -->

**Title**
The conformance verdict is harvested from the test framework's own report, never recomputed

**Lens**: ARCH

**Status**: planned

**Description**
Snow-White contains no schema validator and adds none.
The conformance verdict
([SW-049](SW-049-conformance-verdict-attributes.md)) is read out of the validation the test
framework already performed, at the framework's own extension point, and translated into attributes.

Concretely, in `toolkit/citrus-junit-jupiter-extension`: Citrus's OpenAPI integration already
validates every response against the generated document, and
`OpenApiResponseValidator.validateResponseToReport` returns that outcome as a
`com.atlassian.oai.validator.report.ValidationReport` **without throwing**.
The harvest registers through Citrus's schema-validator SPI
(`META-INF/citrus/message/schemaValidator/`), where the report and Citrus's
`OperationPathAdapter` are both in hand, and maps the report's messages to pointers.

Three things are excluded, each a route a reader might otherwise assume:

- **Not an HTTP interceptor.** `TestIdentityBaggageInterceptor` sits on Spring's
  `ClientHttpRequestInterceptor` and sees a `ClientHttpResponse`; reading a body there consumes the
  stream and requires `BufferingClientHttpRequestFactory`.
  Citrus's verdict lives on Citrus's own message inside its validation pipeline, so harvesting at
  the SPI needs no buffering and no second read of any payload.
- **Not an independent validator.** Re-validating would be portable to non-Citrus consumers and is
  rejected anyway: it would mean a second validator disagreeing with the one the test already ran,
  so a test could pass while the report said the response was broken.
  One verdict per response, from the party that already computed it.
- **Not in the application.** No validation runs in the service under test, under any flag.
  It would need the document at runtime, add latency to production traffic, and make Snow-White a
  participant in the behaviour it observes.

No request or response body, and no validator message, leaves the test JVM — only the boolean and
the pointers.
Harvesting is subordinate to [CON-013](CON-013-extension-never-changes-a-test-verdict.md): a report
Citrus treated as a pass stays a pass, one it treated as a failure stays a failure, and every failure
of the harvest itself — an SPI that does not load, a report shape the mapper does not recognise, an
unreachable exporter — costs the verdict and never the test.

The cost accepted in exchange is coverage: a consumer whose tests are not Citrus-driven publishes no
verdicts, and the criterion is inapplicable for them
([SW-030](SW-030-unjudged-target-is-not-applicable.md)).

**Rationale**
The validation already happens; what is missing is a path from it to a quality gate.
Writing a validator to report on a validator would be the largest possible version of this feature
and the only version that can contradict itself.

Harvesting is also what keeps the feature honest about its own strength, which the documentation
must state: coverage cannot be faked by editing a test file, because the spans either exist or they
do not, whereas harness-side conformance is defeated by removing the extension.
That is a weaker claim than coverage and a far better one than no check, and it is weaker precisely
because the verdict comes from the harness — a property of this decision, not an accident to be
patched later.

Choosing the SPI over the interceptor is the difference between a reporting change and a behaviour
change.
`BufferingClientHttpRequestFactory` alters the test client for every test in the repository in order
to re-read something another component already parsed; the SPI reaches the parsed result directly.
The issue that proposed this work named the interceptor, and that was the wrong seam.

Accepting the Citrus coupling rather than paying for portability is a judgement that
[SW-030](SW-030-unjudged-target-is-not-applicable.md) makes safe: an unjudged response is reported as
unjudged, so a non-Citrus consumer gets an honest "nothing to report" instead of a wrong number.
Reaching consumers' own tests is a separate audience and a later decision.

**Verification Description**
A unit test on the harvesting validator asserts a `ValidationReport` with no messages yields a
conforming verdict, and one with messages yields a non-conforming verdict whose pointers derive from
those messages.
A test asserts the mapper produces no attribute carrying a validator message or any payload
fragment.
A test asserts a report shape the mapper cannot interpret yields a non-conforming verdict rather
than an exception escaping into the test.
A test asserts the test outcome is unchanged in all of the above, including when publication throws,
and that nothing wraps the response in a buffering request factory.
A review check asserts no production source outside the toolkit references a schema validator.

## Relations

**Realizes**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the evaluation this verdict feeds

**Related**

- [SW-049](SW-049-conformance-verdict-attributes.md) — the attributes this produces, and
  the capture-side bound it applies
- [SW-048](SW-048-response-schema-conformance-criterion.md) — the criterion that consumes
  them
- [ARCH-024](ARCH-024-harness-publishes-under-the-api-identity.md) — how the harvested
  verdict is exported
- [CON-013](CON-013-extension-never-changes-a-test-verdict.md) — the guarantee every failure path
  here is subordinate to
- [ARCH-020](ARCH-020-test-identity-published-not-propagated.md) — the module this harvest joins,
  and the interceptor seam it deliberately does not use
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — what a non-Citrus consumer's responses
  become
