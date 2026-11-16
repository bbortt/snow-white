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

The second step's definitions are the four predefined gates only on an instance whose gates are not
file-owned.
On a file-owned one the step keeps this place in the ordering and reads the gate file instead
([CON-016](CON-016-managed-instance-has-exactly-one-gate-writer.md)); what this spec fixes is the
order and the idempotence, not where the definitions come from.
Criteria seeding is unconditional in either mode, and for the same reason.

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
- [CON-016](CON-016-managed-instance-has-exactly-one-gate-writer.md) — the other thing the second
  step's definitions can be, on a file-owned instance

## Changes

- **2026-09-17** — Set active: anchored against `quality-gate-api`'s existing implementation
  (`STR-009`).
- **2026-11-16** — Scoped the second step's definitions to an unmanaged instance, because
  `STR-029` repoints that step at a gate file on a file-owned one.
  The description read as though the four predefined gates were the only thing it could seed; the
  order and the idempotence this spec exists to fix are unchanged in either mode.
