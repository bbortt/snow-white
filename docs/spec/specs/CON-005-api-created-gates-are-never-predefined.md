# A quality gate created through the API is always user-defined, never predefined

<!-- markdownlint-disable MD036 -->

**Title**
A quality gate created through the API is always user-defined, never predefined

**Lens**: CON

**Status**: active

**Description**
Creating a quality gate through the REST API always persists it as user-defined
(`isPredefined = false`), no matter what the request body contains.
Only the service's own startup seeding can produce a predefined gate.

**Rationale**
[CON-003](CON-003-predefined-quality-gates-are-immutable.md) guarantees a predefined gate cannot
be changed or deleted through the API — but that guarantee only holds if the set of predefined
gates is closed to begin with.
If a client could mark its own gate as predefined, it could mint a new "unmodifiable" record at
will, defeating the reason predefined gates are locked down: that they are a shared, known
baseline a pipeline can reference by name and trust not to move.

**Verification Description**
A test creates a gate via the API with `isPredefined: true` in the request body and asserts the
persisted (and returned) record has `isPredefined: false`.

## Relations

**Related**

- [CON-003](CON-003-predefined-quality-gates-are-immutable.md) — the immutability guarantee
  this invariant makes possible

## Changes

- **2026-09-17** — Set active: anchored against `quality-gate-api`'s existing implementation
  (`STR-009`).
