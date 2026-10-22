# Criteria metadata is owned by the `OpenApiCoverageCriteria` enum, not duplicated in the database

<!-- markdownlint-disable MD036 -->

**Title**
Criteria metadata is owned by the `OpenApiCoverageCriteria` enum, not duplicated in the database

**Lens**: ARCH

**Status**: active

**Description**
The `OpenApiCoverageCriteria` enum in `internal/commons` is the single source of truth for every
criterion's label and description.
The database's `OpenApiCoverageConfiguration` table stores only a criterion's `name` — a
foreign-key-style existence marker that quality-gate configurations attach to — never a copy of
its label or description.
Any endpoint that returns criterion metadata resolves it by looking up
`OpenApiCoverageCriteria.valueOf(name)` at read time, not from a stored column.

**Rationale**
A criterion's label and description are fixed, code-derived facts about a calculation the
`openapi-coverage-stream` service implements — not user data.
Storing them a second time in the database would create two places that could drift, with no
mechanism keeping them in sync; resolving from the enum at read time makes drift structurally
impossible; the database row exists only so a gate can reference a criterion by a stable name.

**Verification Description**
A test reads the `OpenApiCoverageConfiguration` table schema (or entity) and confirms it has no
label/description column; a second test asserts the criteria catalog endpoint's label and
description for a given criterion match `OpenApiCoverageCriteria.valueOf(name)`'s own fields, not
a value that could be independently edited.

## Relations

**Related**

- [SW-012](SW-012-criteria-catalog-endpoint.md) — the endpoint that resolves this
  metadata at read time
- [SYS-006](SYS-006-criteria-based-evaluation.md) — the fixed, code-derived criteria set this
  enum defines
- [ARCH-016](ARCH-016-criteria-containment-declared-on-the-enum.md) — the containment relation the
  same enum comes to own
- [SW-042](SW-042-criterion-narrowing-reaches-the-containing-criteria.md) — the rule that needs that
  relation at runtime

## Changes

- **2026-09-17** — Set active: anchored against `quality-gate-api`'s existing implementation
  (`STR-009`).
- **2026-10-04** — Records `ARCH-016` (`STR-023`): criteria containment joins the metadata this enum
  owns.
  This is the same decision applied to a relation that had been left out of it.
  Containment between criteria existed only as English prose inside a description string and as an
  ASCII tree in `pages/_pages/quality-gate-criteria.md`, which made it unreadable to code — so
  `SW-042`'s narrowing rule had nothing to consult, and the documentation page was a second source
  rather than a view.
  Nothing about the existing metadata or the catalog read changes.
