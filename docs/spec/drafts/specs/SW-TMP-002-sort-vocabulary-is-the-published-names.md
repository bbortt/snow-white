# A list read sorts by its published property names, never by the entity attribute behind them

<!-- markdownlint-disable MD036 -->

**Title**
A list read sorts by its published property names, never by the entity attribute behind them

**Lens**: SW

**Status**: planned

**Description**
The vocabulary of a list read's `sort` parameter is the set of property names that endpoint
**publishes** in its OpenAPI document, and each endpoint publishes which of them are sortable:

| List read                                      | Sortable published properties                           |
| ---------------------------------------------- | ------------------------------------------------------- |
| `report-coordinator-api` quality-gate reports  | `initiatedAt`, `qualityGateConfigName`, `calculationId` |
| `quality-gate-api` quality-gate configurations | `name`, `isPredefined`, `minCoveragePercentage`         |
| `api-index-api` ingested APIs                  | `serviceName`, `apiName`, `apiVersion`, `apiType`       |

Each name is translated to the entity attribute that backs it before the query is built —
`initiatedAt` to `createdAt`, `serviceName` to `otelServiceName`, the rest identical.

A published property that is a collection or a nested object (`interfaces`,
`calculationRequest`) is not sortable. `status` is not sortable on the report read, and
`description` is not sortable on a quality-gate configuration.

Every sortable property is backed by a non-nullable column, so no order this contract produces has
to define where a missing value sorts.

The published names are the **whole** vocabulary.
The entity attribute names behind the two
translated ones — `createdAt` and `otelServiceName` — are themselves rejected, and appear in no
OpenAPI document.

`apiVersion` orders as a string, not as a semantic version: `1.10.0` precedes `1.9.0`.

**Rationale**
A caller can only discover the names an API publishes.
Resolving `sort` against the entity instead
makes the working vocabulary an implementation detail that leaks through a query parameter —
undiscoverable from the document, and silently coupled to the persistence model, so renaming a
column becomes a breaking API change nobody notices.

Translating at the edge keeps that coupling in one declared place per endpoint, which is also where
the published-to-entity naming is already decided.

`status` is excluded rather than translated because the translation would misrepresent the result.
It is published as a string enum but persisted as a stable numeric code (`ARCH-006`) whose order —
`IN_PROGRESS`, `FAILED`, `PASSED`, `FINISHED_EXCEPTIONALLY`, `TIMED_OUT` — is neither alphabetical
nor a severity ranking.
Sorting by the column would claim to order by the published string while
ordering by an internal code; honouring the published order would need a `CASE` expression for a
sort nobody has asked for.

The entity names are not accepted alongside the published ones, even though the previous behaviour
resolved them.
Accepting both would leave two spellings of one order in the wild, neither
discoverable as the canonical one, and no release at which the second stops working.
The webapp is
the only known caller, it sent those two names until this story and sends the published ones after
it, and Snow-White has no external API consumer to protect — so the second vocabulary would have no
user.
A caller still sending an entity name gets the same `400` as any other unpublished property,
naming what to send instead, which is a better outcome than an order that quietly keeps working
under a name the document does not list.

`apiVersion`'s string order is stated rather than fixed because the column is a 16-character string
with no parsed form, and a correct version order is a feature in its own right, not a property of
this sort contract.

`description` is excluded because it is the only nullable candidate across the three reads, and
admitting it would force this contract to define where a row with no description sorts.
Answering
that with an explicit `NULLS` clause changes the order PostgreSQL's existing indexes provide, which
would cost the index-backed defaults `SW-TMP-001` relies on — a real price for ordering
quality gates by prose nobody has asked to sort by.

**Verification Description**
A test requests each list read with `sort` set to each of that endpoint's published sortable names
in both directions, and asserts the rows come back in that order of the **published** field — not
of any other field that would coincide with it on the seeded data.

For the two translated names, the test asserts specifically that the published name works
(`sort=initiatedAt,desc` orders reports newest first; `sort=serviceName,asc` orders the index by
OTel service name) and that the entity attribute name behind it is rejected, with the rejection
message naming the published names only.

A test asserts `sort=status,asc` on the report read is rejected rather than ordered, and that no
OpenAPI document in the three services mentions `createdAt` or `otelServiceName` as a sort value.

## Relations

**Related**

- [SW-TMP-001](SW-TMP-001-list-order-is-stable-by-default.md) — the default order and tiebreaker
  expressed in this same vocabulary
- [SW-TMP-003](SW-TMP-003-unusable-sort-is-rejected-not-ignored.md) — what happens to a name
  outside this vocabulary
- [ARCH-006](ARCH-006-report-status-persisted-as-stable-code.md) — the stable numeric code that
  makes `status` unsuitable to sort by
- [SW-028](SW-028-list-filters-match-by-case-insensitive-prefix.md) — the sibling query-parameter
  contract on the API-index listing
- [CON-010](CON-010-rest-responses-never-carry-null.md) — the other published-shape rule these
  reads follow
