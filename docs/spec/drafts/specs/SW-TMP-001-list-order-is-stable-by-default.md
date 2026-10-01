# A list read is ordered by default and every order it applies ends in a unique tiebreaker

<!-- markdownlint-disable MD036 -->

**Title**
A list read is ordered by default and every order it applies ends in a unique tiebreaker

**Lens**: SW

**Status**: planned

**Description**
Every paginated list read declares a default order and applies it when the request carries no
`sort` value, an empty one, or a blank one:

| List read                                      | Default order                              |
| ---------------------------------------------- | ------------------------------------------ |
| `report-coordinator-api` quality-gate reports  | `initiatedAt,desc`                         |
| `quality-gate-api` quality-gate configurations | `name,asc`                                 |
| `api-index-api` ingested APIs                  | `serviceName,asc`, `apiName`, `apiVersion` |

Every order a list read applies — the default and any order a caller requests — ends in a property
combination unique per row, so no two rows can compare equal: the report's `calculationId`, the
configuration's `name`, and the API index's `(serviceName, apiName, apiVersion)` triple.

**Rationale**
PostgreSQL guarantees no row order for a query without an `ORDER BY`, and a `LIMIT`/`OFFSET` page
is taken from whatever order the plan happened to produce.
A caller paging through an unordered
list can therefore receive one row on two consecutive pages and never receive another, which reads
as missing data rather than as a missing sort.

A non-unique order has the same defect in a narrower form: rows that compare equal may be returned
in any order relative to each other, and nothing requires that order to be the same between two
requests.
Appending a unique tiebreaker makes each order total, which is what makes a page
reproducible.

The defaults are the orders each list is most useful in and already presented in: newest report
first, configurations alphabetically, APIs grouped by the service that owns them.
Each is backed by
an index that already exists — `idx_quality_gate_created_at`,
`quality_gate_configuration`'s unique constraint on `name`, and `api_reference`'s primary key,
whose column order is exactly the API-index default — so ordering by default costs no new index.

**Verification Description**
A test requests the first page of each list read with no `sort` parameter and asserts the returned
rows are in that endpoint's documented default order, against a data set seeded to distinguish it
from insertion order.
It repeats the request for `sort` present but empty and asserts the same
order, confirming a blank value is "no sort" rather than an error.

A further test seeds rows that compare equal under a requested non-unique order (two reports
sharing a creation instant, two APIs sharing a service name), requests the same page twice, and
asserts both responses return the same rows in the same order.

## Relations

**Related**

- [SW-TMP-002](SW-TMP-002-sort-vocabulary-is-the-published-names.md) — the vocabulary a requested
  order is expressed in, to which the tiebreaker is appended
- [SW-TMP-003](SW-TMP-003-unusable-sort-is-rejected-not-ignored.md) — why a blank value defaults
  rather than being rejected
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the report listing this orders
- [SW-009](SW-009-quality-gate-crud-contract.md) — the quality-gate listing this orders
- [SW-028](SW-028-list-filters-match-by-case-insensitive-prefix.md) — the filters that narrow the
  API-index listing this orders
