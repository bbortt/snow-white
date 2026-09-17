# Criteria catalog endpoint returns every coverage criterion, enriched from the enum, sorted by name

<!-- markdownlint-disable MD036 -->

**Title**
Criteria catalog endpoint returns every coverage criterion, enriched from the enum, sorted by
name

**Lens**: SW

**Status**: active

**Description**
`GET /api/rest/v1/criteria/openapi` returns exactly one entry per `OpenApiCoverageCriteria` enum
constant — currently 14 — sorted by name, each with its `id`, `name`, and `description` resolved
from the enum at read time (see
[ARCH-002](ARCH-002-criteria-metadata-owned-by-enum.md)).
The endpoint takes no parameters and returns the full catalog every time; it exists so a caller
building or editing a gate's criteria list — a human in the UI, or a script — knows the valid
criterion names and what each one means without hardcoding them.

**Rationale**
A gate's criteria are referenced by name (see
[SW-009](SW-009-quality-gate-crud-contract.md)'s unknown-criterion `400`), so a caller
needs a way to discover the valid names and their meaning without reading source code or a
separate document that could drift from what the API actually accepts.

**Verification Description**
A test calls the endpoint and asserts the response contains exactly
`OpenApiCoverageCriteria.values().length` entries, sorted by name, with each entry's fields
matching the corresponding enum constant — matching `CriteriaApiAppTest`'s existing black-box
coverage.

## Relations

**Related**

- [ARCH-002](ARCH-002-criteria-metadata-owned-by-enum.md) — where this endpoint's data
  comes from
- [SW-009](SW-009-quality-gate-crud-contract.md) — the gate contract this catalog
  supports

## Changes

- **2026-09-17** — Set active: anchored against `quality-gate-api`'s existing implementation
  (`STR-009`).
