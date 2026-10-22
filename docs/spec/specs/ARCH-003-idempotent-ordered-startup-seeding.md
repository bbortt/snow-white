# Startup seeding is idempotent and strictly ordered: criteria before predefined gates

<!-- markdownlint-disable MD036 -->

**Title**
Startup seeding is idempotent and strictly ordered: criteria before predefined gates

**Lens**: ARCH

**Status**: active

**Description**
On every startup, `DatabaseInitializer` seeds two things in a fixed order: first, one
`OpenApiCoverageConfiguration` existence row per `OpenApiCoverageCriteria` enum constant not
already present; then the four predefined quality gates, upserted by name so an existing gate's
`id` (and any user-added association to it) is preserved rather than replaced.
Predefined-gate seeding attaches each gate's criteria by looking up the already-seeded criterion
rows by name, and fails fast (`IllegalStateException`) if a referenced criterion row does not yet
exist — so criteria seeding must run first, every time, not just on first boot.

**Rationale**
Both seeding steps must be safe to run on every restart, not just a fresh database, because there
is no separate one-time-migration mechanism for this data — the enum can grow, and a predefined
gate's criteria set is defined in code, so the running instance must reconcile the database to
match on every startup.
Ordering criteria before gates keeps the dependency explicit and fails loudly (rather than
silently skipping a criterion) if that ordering is ever broken by a future change.

**Verification Description**
A test starts the service against a database that already has one predefined gate with a
user-modified `id`, asserts the `id` is unchanged after seeding (upsert, not replace); a second
test asserts seeding predefined gates before criteria exist throws `IllegalStateException` rather
than silently producing a gate with missing criteria.

## Relations

**Related**

- [SW-011](SW-011-four-predefined-gates-fixed-composition.md) — the specific gates this
  seeding produces

## Changes

- **2026-09-17** — Set active: anchored against `quality-gate-api`'s existing implementation
  (`STR-009`).
