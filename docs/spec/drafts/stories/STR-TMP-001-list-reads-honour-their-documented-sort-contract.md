# List reads honour their documented sort contract instead of leaking entity names and failing with 500

<!-- markdownlint-disable MD036 -->

**Title**
List reads honour their documented sort contract instead of leaking entity names and failing with
500

**Status**: planned

**Business Value**
Three published list endpoints document a `sort` parameter that does not work.
A caller who follows the documentation gets a `500`; a caller who guesses right has to guess an
internal column name that appears nowhere in the API.
Both outcomes cost support time and neither is discoverable, so the parameter is effectively
unusable by anyone outside this repository.

Unordered pagination is the quieter half of the cost.
Without an `ORDER BY`, PostgreSQL makes no promise about row order between two requests, so a
client paging through a list can see the same row twice or miss one entirely — and the result looks
like data loss rather than a missing sort.

**Problem / Context**
`api-index-api`, `quality-gate-api` and `report-coordinator-api` each expose a paginated list read
whose `sort` parameter is documented as `property,(asc|desc)`, "Defaults to `initiatedAt,desc`".
All three hand the raw value to `PaginationUtils.toPageable(page, size, sort)` in
`internal/commons`, which:

- defaults to `Sort.unsorted()`, so there is no default order and no stable pagination;
- passes the property name straight to Spring Data, which resolves it against the **entity**, not
  the published DTO;
- silently ignores anything it cannot parse, rather than rejecting it.

Premises checked on 2026-10-01, in this session, not assumed:

- `PaginationUtils.toPageable` behaves as described — read at
  `internal/commons/src/main/java/io/github/bbortt/snow/white/commons/web/PaginationUtils.java:25-49`.
  The silent-ignore case is wider than "malformed": the value is split on `,` and only a
  two-element result is honoured, so `foo` (no direction) and `a,asc,b,desc` (multi-key) are both
  dropped to `Sort.unsorted()` without an error.
- All three call sites pass the raw parameter — `ReportResource:153`, `QualityGateResource:134`,
  `ApiIndexResource:116`.
- All three specs carry the identical `initiatedAt,desc` text — `v1-report-api.yml:31-37`,
  `v1-quality-gate-api.yml:31-37`, `v1-api-index-api.yml:63-71`.
  `initiatedAt` is a published property of the report read only; it exists in neither of the other
  two APIs, so that sentence was copied, not written.
- The `500` is structural, not incidental: all three `ApiExceptionHandler`s are byte-identical
  copies carrying no `@ExceptionHandler` methods, only a `handleExceptionInternal` override.
  `PropertyReferenceException` comes from `spring-data-commons` and is not in
  `ResponseEntityExceptionHandler`'s known set, so it reaches the default `500` in all three.
- Neither the report list nor the api-index list documents a `400` at all today (only `200` and
  `500`); `quality-gate-api`'s `400`s belong to its other operations.
- The published-to-entity gaps are `initiatedAt` → `createdAt` and `serviceName` →
  `otelServiceName`; `quality-gate-api`'s published names already match its entity.
- `ApiReference.indexedAt` exists and is indexed but is not a published property of
  `ApiInformation.yml`, so it is not a candidate sort name.
- Every default order this story introduces is already index-backed:
  `idx_quality_gate_created_at`, `quality_gate_configuration`'s unique constraint on `name`, and
  `api_reference`'s primary key `(otel_service_name, api_name, api_version)` — which is exactly the
  api-index default order.
  No migration is needed.
- `toolkit/cli` never sends `sort`: the parameter appears only in its generated clients
  (`ReportApi.ts:58`, `ApiIndexApi.ts:67`), passed by no command.
- The webapp is the only known caller and sends entity names today — `createdAt,desc`
  (`quality-gate.tsx:49`) and `otelServiceName,asc` (`api-index.tsx:53`); `quality-gate-config.tsx:32`
  sends `name`, which is already a published name.

`report-coordinator-api`'s own integration test encodes the defect as the contract:
`ReportResourceIT:536` sorts by `createdAt,desc`, the entity name.

**Solution Approach**
Make the code match the documentation rather than the reverse, because the documented vocabulary is
the only one a caller can discover.

Each list read declares a **sort definition**: the published property names it accepts, the entity
attribute each maps to, its default order, and a unique tiebreaker appended to every order so
pagination is stable.
Parsing stays in `internal/commons` so the grammar is defined once; the
per-endpoint vocabulary is declared beside the resource that publishes it, because the
published-to-entity naming is that service's concern and not a shared one.

A value naming a property outside that definition, or one that does not parse, is rejected with
`400` carrying the existing `{code, message}` error body, whose message names the accepted
properties.
This is the half that contradicts `SW-014`, which currently calls the report listing an
"always-`200`" surface; that spec is amended rather than worked around.

The entity names the webapp sends today (`createdAt`, `otelServiceName`) are **not** also accepted.
They are the only two spellings any caller can be using, the webapp is the only known caller, and it
moves to the published names in this story — so accepting them would preserve a vocabulary with no
user and no release at which it stops working.
Snow-White has no external API consumer to protect
here, and the owner confirmed the break is acceptable; a caller still sending an entity name gets the
same `400` as any other unpublished property, naming what to send instead.

`status` is excluded from the report read's sortable set.
It is published as a string enum but
persisted as a stable numeric code (`ARCH-006`) whose order — `IN_PROGRESS`, `FAILED`, `PASSED`,
`FINISHED_EXCEPTIONALLY`, `TIMED_OUT` — is neither alphabetical nor a severity ranking, so sorting
by it would order by an internal code while appearing to order by the published string.

**Acceptance Criteria**

- A list read with no `sort` returns its documented default order: `initiatedAt,desc` for reports,
  `name,asc` for quality-gate configurations, `serviceName,asc` then `apiName`, `apiVersion` for
  the API index.
- Every order a list read applies ends in a unique tiebreaker, so two requests for the same page of
  an unchanged data set return the same rows in the same order.
- A `sort` naming a published property orders by it: `initiatedAt` on the report read orders by the
  report's creation instant, and `serviceName` on the API index orders by the OTel service name.
- A `sort` naming a property that is not in the endpoint's sortable set, that does not parse, or
  that names a direction other than `asc`/`desc`, answers `400` with a `{code, message}` body whose
  message names the accepted properties.
  It never answers `500` and never silently returns an
  unordered page.
- `status` is not accepted as a sort property on the report read, and is rejected like any other
  unknown name.
- `createdAt` and `otelServiceName` are rejected like any other unpublished property, and appear in
  no OpenAPI document.
- Each of the three OpenAPI documents states that endpoint's real default order, lists the
  properties it accepts, and declares the `400` response.
- The webapp's three list pages request published property names, and their column headers sort by
  them.
- A request whose `sort` is absent, empty, or blank is treated as "no sort" and gets the default
  order, not a `400`.

**Out of scope**

- Multiple sort keys.
  The parameter is typed as a single string in all three published documents,
  and a multi-key value is silently dropped today, so no caller can be relying on it.
  The implicit
  tiebreaker covers the stability need that multi-key requests are usually about.
- Publishing `indexedAt` on the API index so it can carry a newest-first default.
  The default stays
  `serviceName,asc`, matching what the UI already shows.
- Sorting by a nested or collection property (`interfaces`, `calculationRequest`,
  `openApiCoverageConfigurations`).
  Only scalar published properties are sortable.
- Sorting a quality-gate configuration by `description`.
  It is the only nullable sortable candidate
  across the three reads, and admitting it would force a null-ordering decision whose explicit
  `NULLS` clause changes the order the existing indexes provide — see `SW-TMP-002`.
- Semantic version ordering for `apiVersion`.
  It sorts as a string, so `1.10.0` precedes `1.9.0`;
  the specs say so rather than the code working around it.
- The `page` and `size` parameters, and the `X-Total-Count` header.
  Unchanged.
- Collapsing the three byte-identical `ApiExceptionHandler` copies into one shared advice.
  Each
  gains the same new mapping here, which makes the duplication more visible, but merging them
  changes error handling for every endpoint in all three services and belongs in its own increment.

## Relations

**Realizes**

- [SW-TMP-001](../specs/SW-TMP-001-list-order-is-stable-by-default.md) — the default order and the
  tiebreaker that makes pagination stable
- [SW-TMP-002](../specs/SW-TMP-002-sort-vocabulary-is-the-published-names.md) — the published
  property names as the sort vocabulary
- [SW-TMP-003](../specs/SW-TMP-003-unusable-sort-is-rejected-not-ignored.md) — the `400` that
  replaces the `500` and the silent no-op

**Related**

- [SW-014](../specs/SW-014-in-progress-report-answers-accepted.md) — amended: the report listing is
  no longer an "always-`200`" surface, because an unusable `sort` is now rejected
- [SW-009](../specs/SW-009-quality-gate-crud-contract.md) — amended: the quality-gate list entry
  gains its default order and the `400` for an unusable `sort`
- [SW-028](../specs/SW-028-list-filters-match-by-case-insensitive-prefix.md) — the sibling
  query-parameter contract on the same API-index listing, unchanged here
- [ARCH-006](../specs/ARCH-006-report-status-persisted-as-stable-code.md) — the stable numeric
  status code that makes `status` unsuitable as a sort property
- [NF-005](../specs/NF-005-clear-failure-feedback.md) — the naming-the-actual-cause principle the
  rejection message follows
