# REST responses omit a property that has no value instead of serializing it as null

<!-- markdownlint-disable MD036 -->

**Title**
REST responses omit a property that has no value instead of serializing it as null

**Status**: active

**Business Value**
Every `null` a REST response carries is bytes that say nothing.
They are serialised, sent, parsed and discarded on every request.
Dropping them shrinks every response and removes that work from each call.
It costs no information: an optional property that is absent means the same as one that is `null`.
This follows the project's resource-usage rule: keep compute low where there is no real downside.

**Problem / Context**
`api-index-api`, `quality-gate-api` and `report-coordinator-api` serve their REST APIs with Spring
Boot's default Jackson mapper.
That mapper writes every property, so each unset optional property goes over the wire as `null`.

The most visible case is `FindingEvidence.testCaseName` on the single-report read (`SW-031`).
It is `null` on every evidence entry whose span carried no test identity.
Until `SW-032`'s convention is in use, that is every entry of every finding.
`ApiTestFinding`'s five discriminators (`httpPath`, `httpMethod`, `responseCode`,
`parameterName`, `contentType`) add more `null`s per finding.
A report can carry hundreds of findings, so the waste scales with the report.

`SW-031` currently requires the opposite for `testCaseName`: serialised as `null`, not omitted.
Its reason was that a consumer's name-or-trace-id fallback is exercisable from the first release.
That fallback works the same on an absent property.
Both generated clients already treat the two alike.
The webapp's `FindingEvidence` type declares `testCaseName?: string`.
The CLI's generated deserializer maps `null` to `undefined`.

None of the three services' OpenAPI documents declare a property `nullable` or
`type: [..., 'null']`.
A served `null` therefore already sits outside the published schemas; omission conforms to them.

Premises checked on 2026-09-30, in this session, not assumed:

- `SW-031` mandates explicit `null` — read at its Description and Verification Description.
- `ApiTestFinding.evidence` is `required` and empty on every non-covered finding — read in
  `ApiTestFinding.yml`.
  Also excluding _empty_ values was therefore rejected: it would drop a required property.
- No schema in the three services is nullable — grep over their `src/main/resources/openapi`.
- Only these three services serve REST resources of their own — grep for `*Resource` controllers
  over `microservices/*`.
  `api-gateway` has none; it relays the three services' bodies unchanged.
- Spring Boot `4.1.1` still offers `spring.jackson.default-property-inclusion` — read in
  `spring-boot-jackson-4.1.1.jar`'s configuration metadata.
- No consumer tests a served value with `=== null` — grep over the webapp and `toolkit/cli`
  sources.

**Solution Approach**
Set the default property inclusion of each service's web JSON mapper to non-null, through
configuration rather than per-class annotations.
Configuration covers every current and future response type, including generated models.
It also leaves nothing to forget on the next component someone adds.

Only `null` is dropped.
Empty collections and empty strings stay on the wire.
`SW-031`'s empty-never-absent rule for `findings` and the required `evidence` array depend on it.

`SW-031` is amended so that `testCaseName` is omitted rather than `null` where the span carried no
test identity.
The OpenAPI descriptions that say "null where …" are reworded to "absent where …".

**Acceptance Criteria**

- A response from each of the three services omits a property whose value is unset, and never
  carries a `null` value.
- A single-report read omits `testCaseName` on an evidence entry whose span carried no test
  identity.
  It still carries `testCaseName` where the span did.
- A finding's `evidence` is still present, as `[]`, on every non-covered finding.
- A result's `findings` is still present, as `[]`, on a report written before findings existed.
- `SW-031`'s Description and Verification Description state omission, not explicit `null`.
- The OpenAPI component descriptions in the three services no longer promise a `null`.
- The webapp's findings drilldown and the CLI's findings output still fall back to the trace id
  when `testCaseName` is absent.
- Existing tests that read an unset discriminator or `testCaseName` as a JSON `null` node assert
  its absence instead.

**Out of scope**

- Kafka messages.
  They are written by Spring Kafka's own serializer, not the web mapper, and `ARCH-011`'s event
  shape stays as it is.
- The JUnit XML export.
  `XmlMapperConfiguration` already excludes empty values and is unchanged.
- Actuator and `springdoc` responses.
  Both use mappers of their own.
- `api-gateway`, `api-sync-job`, `openapi-coverage-stream` and `otel-event-filter-stream`.
  None serves a REST resource of its own.
- Excluding empty collections or empty strings.
  Rejected, see Problem / Context.

## Relations

**Realizes**

- [CON-010](../specs/CON-010-rest-responses-never-carry-null.md) — the invariant this story
  establishes across the three services

**Related**

- [SW-031](../specs/SW-031-findings-served-with-the-report.md) — amended: `testCaseName` is
  omitted, not `null`; its empty-never-absent array rule is kept and relied upon
- [SW-032](../specs/SW-032-test-identity-on-the-span.md) — the convention that fills
  `testCaseName`; until it is in use, every entry is the absent case
- [ARCH-011](../specs/ARCH-011-evidence-captured-at-the-match.md) — the `(traceId, testCaseName)`
  evidence pair; its domain and Kafka shapes are unchanged
- [NF-003](../specs/NF-003-scalability-without-redesign.md) — the low-resource-footprint target
  this contributes to
