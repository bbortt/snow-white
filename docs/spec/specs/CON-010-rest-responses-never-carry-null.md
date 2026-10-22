# A REST response never carries a null-valued property; an unset property is omitted

<!-- markdownlint-disable MD036 -->

**Title**
A REST response never carries a null-valued property; an unset property is omitted

**Lens**: CON

**Status**: active

**Description**
Every JSON body served by the REST APIs of `api-index-api`, `quality-gate-api` and
`report-coordinator-api` omits a property whose value is unset.
No such body carries a property whose value is `null`.
This holds for success and error bodies alike, and for every response type, generated or
hand-written.

Only `null` is omitted.
An empty array and an empty string are values, and stay on the wire.
`SW-031`'s rule that `findings` and `evidence` are empty, never absent, therefore still holds.

The rule is set once per service, as the web JSON mapper's default property inclusion.
It is not set per class or per property.
A response type added later is covered without anyone having to remember it.

**Rationale**
An absent optional property and a `null` one mean the same thing to every consumer in this
repository.
The webapp declares such properties optional (`?:`), and the CLI's generated deserializer maps
`null` to `undefined`.
None of the three services' OpenAPI documents declares a property nullable, so a served `null`
sits outside the published schema anyway.
The `null` is therefore pure overhead: serialised, sent, parsed and discarded on every call.
On the single-report read it repeats per evidence entry and per finding, so it grows with the
report.

Omitting empty values as well was considered and rejected.
It would drop `ApiTestFinding.evidence`, a `required` property, on every non-covered finding.
It would also reintroduce the "absent means not loaded" ambiguity `SW-031` removed for `findings`.

A per-class `@JsonInclude` was rejected too.
It covers only the classes someone annotated, and the models are generated from OpenAPI.

**Verification Description**
In each of the three services, an integration test fetches a response whose model has an unset
optional property.
It asserts the property is absent from the raw JSON body, not merely that it deserialises to
`null`.
Which optional property each service's test leaves unset is chosen when the test is written.
The rule applies to every property alike, so any one unset optional property proves the mapper
setting.
In `report-coordinator-api`, a test asserts an evidence entry without test identity has no
`testCaseName` key.
The same test asserts a non-covered finding still carries `"evidence": []`.
A review confirms no service opts back into `null` through a per-class or per-property inclusion.

## Relations

**Related**

- [SW-031](SW-031-findings-served-with-the-report.md) — its empty-never-absent array rule bounds
  this constraint to `null` alone; its explicit-`null` `testCaseName` clause is amended to omission
- [ARCH-011](ARCH-011-evidence-captured-at-the-match.md) — the evidence pair whose unnamed case is
  now served as an absent `testCaseName`
- [NF-003](NF-003-scalability-without-redesign.md) — the low-resource-footprint target this
  serves

## Changes

- **2026-09-30** — Set active: implementation of `STR-020` began.
