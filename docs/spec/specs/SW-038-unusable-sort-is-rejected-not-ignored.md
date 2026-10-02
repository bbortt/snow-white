# A `sort` value a list read cannot honour is rejected with `400`, not ignored and not a `500`

<!-- markdownlint-disable MD036 -->

**Title**
A `sort` value a list read cannot honour is rejected with `400`, not ignored and not a `500`

**Lens**: SW

**Status**: active

**Description**
A list read answers `400` with the services' `{code, message}` error body when its `sort` value
cannot be honoured, and the message names the properties that endpoint accepts.
This covers:

- a property name outside the endpoint's sortable set, including a name that exists on the entity
  but is not published — `createdAt` and `otelServiceName` are rejected like any other;
- a published property that is not sortable, such as `status` on the report read;
- a direction that is neither `asc` nor `desc`, case-insensitively;
- a value that does not parse as `property,direction` — no direction, more than one key, or an
  empty property name.

A `sort` parameter that is absent, empty, or blank is **not** an error: it means "no preference" and
gets the endpoint's default order.

A list read never answers `500` for a `sort` value, and never answers `200` carrying a page that
silently ignored one.

**Rationale**
The two failure modes this replaces are both undiagnosable from the response.

An unknown property reached Spring Data and raised `PropertyReferenceException`, which no handler
mapped, so a caller following the published documentation received a `500` — a status that says the
server is broken and invites a retry, for a request that will fail identically every time.

An unparsable value was dropped to "no sort" and answered `200`.
The caller received a page that
looked successful but was ordered differently from what it asked for, which is worse than an error:
nothing in the response distinguishes it from an honoured request, so the defect surfaces later as
unstable paging rather than at the call that caused it.

Naming the accepted properties in the message makes the response self-correcting, so a caller does
not have to read the OpenAPI document to recover from the error — the same principle `NF-005`
applies to analysis preconditions.

Blank is treated as absent rather than rejected because a client that builds the query string
unconditionally sends `sort=` when it has no preference, and failing that request would punish a
caller who expressed none.

**Verification Description**
A test requests each of the three list reads with: a property name that exists on the entity but is
not published, a name that exists nowhere, `status` (on the report read), a valid property with the
direction `sideways`, a value with no comma, and a value with two keys.
Each returns `400`, the
body carries `code` and `message`, and the message names that endpoint's accepted properties.
No
case returns `500`, and no case returns `200`.

A companion test asserts the absent, empty, and blank cases return `200` in the default order, so
the rejection cannot be implemented by rejecting everything that is not a well-formed sort.

## Relations

**Related**

- [SW-037](SW-037-list-order-is-stable-by-default.md) — the default order the blank case
  falls back to
- [SW-036](SW-036-sort-vocabulary-is-the-published-names.md) — the vocabulary whose
  complement this rejects
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — amended: the report listing is no
  longer an "always-`200`" surface
- [SW-009](SW-009-quality-gate-crud-contract.md) — amended: the quality-gate listing gains this
  `400`
- [NF-005](NF-005-clear-failure-feedback.md) — the name-the-actual-cause principle the message
  follows

## Changes

- **2026-10-02** — Set active: anchored against `InvalidSortException` and the three
  `ApiExceptionHandler`s that map it (`STR-021`).
