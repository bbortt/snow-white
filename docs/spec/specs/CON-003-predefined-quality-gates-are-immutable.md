# Predefined quality gates cannot be modified or deleted by users

<!-- markdownlint-disable MD036 -->

**Title**
Predefined quality gates cannot be modified or deleted by users

**Lens**: CON

**Status**: active

**Description**
A quality gate Snow-White ships as predefined cannot be renamed, have its criteria or thresholds
changed, or be deleted through the API.
A user extends the criteria set only by defining an additional, user-defined gate — never by
mutating or removing a predefined one.

**Rationale**
Predefined gates are a shared, known baseline teams reference by name across projects and
pipelines.
If any user could change or remove a predefined gate, its name would stop being a reliable
reference.
A pipeline pinned to that name could then pass or fail for a reason unrelated to the analysis
result it is meant to gate.

**Verification Description**
A test attempts to update and to delete a predefined quality gate by name.
Both attempts are rejected with a defined error — not a silent no-op, and not a mutated or deleted
predefined record.
The same two operations against a user-defined gate succeed.

## Relations

**Related**

- [SYS-008](SYS-008-quality-gate-definitions.md) — the gate definition mechanism this
  invariant constrains
