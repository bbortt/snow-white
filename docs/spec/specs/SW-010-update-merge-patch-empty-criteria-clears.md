# Quality-gate update is a merge-patch: unsent fields are preserved, but an explicit empty criteria list clears existing criteria

<!-- markdownlint-disable MD036 -->

**Title**
Quality-gate update is a merge-patch: unsent fields are preserved, but an explicit empty criteria
list clears existing criteria

**Lens**: SW

**Status**: active

**Description**
`PUT /api/rest/v1/quality-gates/{name}` applies the request as a merge-patch: the service maps
the request to a "delta" entity, then copies only its non-null fields onto the persisted record
field-by-field, so a field the request omits keeps its current persisted value.
This applies to `description` and `minCoveragePercentage` as expected — but the request's criteria
list is always mapped to a non-null `Set` (empty if the request sends no criteria), so an update
that sends no criteria clears every criterion the gate previously had, rather than leaving them
untouched.
Referencing a criterion name that does not exist in the fixed catalog is rejected with `400`
before any field is applied.

**Rationale**
Merge-patch semantics let a caller update one field (say, `description`) without having to resend
the gate's entire current state — but only literal `null` is treated as "the caller didn't touch
this," which the criteria field can never actually be given how the request is mapped.
This is a real trap for a caller that updates a gate expecting only the field it set to change;
naming it precisely is what lets that caller avoid it (always resend the current criteria list
unless the intent is to clear it).

**Verification Description**
A test updates only the `description` of an existing user-defined gate, sending the criteria
field as an empty array, and asserts the gate's previously-configured criteria are gone
afterward; a second test resends the gate's existing criteria list alongside the new description
and asserts the criteria are unchanged; a third test references an unknown criterion name and
asserts `400` with no field applied.

## Relations

**Related**

- [SW-009](SW-009-quality-gate-crud-contract.md) — the update operation this refines

## Changes

- **2026-09-17** — Set active: anchored against `quality-gate-api`'s existing implementation
  (`STR-009`).
