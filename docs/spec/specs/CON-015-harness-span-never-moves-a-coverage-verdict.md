# A harness-exported span contributes to conformance and to nothing else

<!-- markdownlint-disable MD036 -->

**Title**
A harness-exported span contributes to conformance and to nothing else

**Lens**: CON

**Status**: active

**Description**
A span the test harness exported
([ARCH-024](ARCH-024-harness-publishes-under-the-api-identity.md)) is admitted to the
analysed dataset for one purpose: it supplies conformance verdicts to
[SW-048](SW-048-response-schema-conformance-criterion.md).
It must contribute nothing to any other criterion.

For any telemetry set, the result computed with the harness's spans present and the result computed
with them removed must be identical for all fourteen pre-existing criteria — the same statuses, the
same ratios, and the same findings — and must differ only in `RESPONSE_SCHEMA_CONFORMANCE`.

The criteria at risk are the telemetry-driven ones.
`PathCoverageCalculator` and its relatives iterate the document and look telemetry up per path, so a
span matching nothing is never consulted.
`NO_UNDOCUMENTED_RESPONSE_CODES` and its two variants invert that
([SW-003](SW-003-undocumented-response-code-detection.md)): they enumerate observed codes, and
`SW-003` states explicitly that telemetry resolving to no documented operation is judged and every
code on it is undocumented.
A harness span is therefore the one kind of telemetry that could register as undocumented traffic
the API never served — a criterion failing because conformance reporting was switched on.

What marks a harness span is the verdict attribute itself: a span carrying
`openapi.response.schema.valid` is one, and no other producer emits it — in-application validation
is out of scope by decision
([ARCH-023](ARCH-023-conformance-verdict-is-harvested-not-recomputed.md)), so nothing else
has a reason to.
The exclusion is therefore one predicate applied once, where
`OpenApiCoverageService.groupTelemetryByPath` already hoists attributes off a span, rather than a
rule each calculator has to remember; and it costs no second marker attribute.
Should the application ever become a producer of these attributes, this marker stops distinguishing
and this spec is what must be revisited first.

**Rationale**
This is what makes the whole story additive, and it is stated as an invariant rather than left to
each calculator because the failure it prevents is the worst kind available here: a consumer enables
conformance reporting and a _different_, already-green criterion turns red, on an API whose
behaviour did not change.
A consumer who meets that once will disable the feature and not come back, and they will be right
to.

It is also the condition under which the departure in `ARCH-024` is acceptable at all.
Letting the harness export into the analysed dataset breaks the clean rule that Snow-White analyses
only what the application under test emitted; that is tolerable exactly as far as the new spans are
inert everywhere except the criterion they exist for, and no further.

`SW-032` established the same shape for `test.case.name` — the attribute changes evidence and
nothing else, proved by comparing results over telemetry differing only by it.
The difference is that there the invariance concerned an attribute on a span Snow-White was already
analysing, and here it concerns a whole span that did not previously exist, which is a larger claim
and so needs its own statement rather than a sentence inside a behaviour spec.

Stating it as "identical for the other fourteen" rather than "excluded from the undocumented-code
calculators" is deliberate: the enumeration of at-risk calculators is today's reading of the code,
and a fifteenth calculator added later must inherit the guarantee without anyone remembering this
spec existed.

**Verification Description**
An integration test over every autowired calculator computes a result for telemetry containing
application spans plus harness spans, and for the same telemetry with the harness spans removed,
and asserts the two agree on every criterion except `RESPONSE_SCHEMA_CONFORMANCE` — statuses, ratios
and findings.
A second test guards that against passing vacuously by requiring the harness spans to carry codes
and paths that _would_ register as undocumented traffic if they were consulted, and asserting
`NO_UNDOCUMENTED_RESPONSE_CODES` is unchanged.
A third asserts the conformance criterion does differ between the two runs, so the spans were
genuinely present.

## Relations

**Related**

- [ARCH-024](ARCH-024-harness-publishes-under-the-api-identity.md) — the export this bounds
- [SW-048](SW-048-response-schema-conformance-criterion.md) — the one criterion these spans
  may reach
- [SW-003](SW-003-undocumented-response-code-detection.md) — the telemetry-driven criteria most at
  risk, and the rule that makes unmatched telemetry judged
- [SW-032](SW-032-test-identity-on-the-span.md) — the precedent invariance claim, at attribute scope
- [ARCH-011](ARCH-011-evidence-captured-at-the-match.md) — the match these spans must not widen
- [CON-001](CON-001-deterministic-analysis-results.md) — the determinism an admitted-but-inert span
  must not disturb
