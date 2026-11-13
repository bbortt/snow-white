# A file-owned instance has exactly one gate writer, and it is startup

<!-- markdownlint-disable MD036 -->

**Title**
A file-owned instance has exactly one gate writer, and it is startup

**Lens**: CON

**Status**: planned

**Description**
While an instance's gates are file-owned
([ARCH-TMP-001](ARCH-TMP-001-gitops-mode-is-instance-configuration.md)), the startup read of its
gate file ([SW-TMP-001](SW-TMP-001-startup-makes-the-gate-set-exactly-the-file.md)) is the only
thing that creates, changes or deletes a gate record.
The REST writes are refused
([SW-TMP-002](SW-TMP-002-managed-instance-rejects-crud-writes-with-conflict.md)), and nothing else
writes gates.

Startup is that writer rather than a second one to be silenced.
The seeding step keeps its place in [ARCH-003](ARCH-003-idempotent-ordered-startup-seeding.md)'s
ordering; what changes is where its definitions come from — the file, instead of the four
hardcoded ones ([SW-011](SW-011-four-predefined-gates-fixed-composition.md)) — and that it also
removes the gates the file does not name.
Criteria seeding is unaffected and still runs first: the criteria catalog is a catalog of what can
be measured, not a gate, and a gate definition cannot be resolved without it.

A gate that was seeded as predefined before the switch was turned on is an ordinary gate to the
file: replaced if the file names it, deleted if it does not.
Startup does not refuse to come up because such gates are present.
No new predefined gate can appear on a file-owned instance, so the set stays closed as
[CON-005](CON-005-api-created-gates-are-never-predefined.md) requires, and
[CON-003](CON-003-predefined-quality-gates-are-immutable.md) continues to hold as stated for the
CRUD surface, which refuses every write here regardless.

**Verification Description**
A test starts a file-owned instance against an empty database with a file naming two gates and
asserts the instance holds exactly those two afterwards, with the criteria rows present.
A second restarts it unchanged and asserts the gate set is identical.
A third starts a file-owned instance against a database already holding seeded predefined gates,
with a file naming none of them, and asserts they are gone and stay gone across a further restart.
A review confirms the only remaining code path that writes a gate record is the startup file read.

**Rationale**
Without startup in scope the mode would be cosmetic, and on this route it is in scope for free: the
only writer that ever ran at startup is the seeding step, and the file replaces that step's input
rather than competing with it.

Leaving the seeding in place and reconciling around it does not survive a restart.
`initPredefinedQualityGates` carries over an existing gate's `id` but takes the criteria set and
threshold from code, so a file naming a gate whose name collides with a predefined one has its
definition reverted on the next boot, with the gate's identity intact and nothing to show a change
occurred.
The four predefined names are unusable on a file-owned instance until startup stops writing them
from code.

Letting the file own a leftover predefined gate is the only reading that leaves one writer.
The alternative — refusing to start, or protecting the rows `CON-003` covers — either turns a
setting into a migration or leaves an instance with gates nothing can change and a file unable to
describe the instance it governs, which is the two-writer problem with the second writer replaced by
nobody.
`CON-003`'s reason for existing is that a pinned gate name must not move under a pipeline; on a
file-owned instance it moves only through a reviewed change to a committed file, which serves that
reason better than the database ever did.

The cost is that a gate cannot change without a restart, and that an instance whose file defines no
gates has none ([SW-TMP-004](SW-TMP-004-empty-file-owned-instance-names-its-file.md)).
Both are visible, both are fixed by shipping the file, and neither is a gate reappearing unattended
after a restart.

## Relations

**Related**

- [ARCH-TMP-001](ARCH-TMP-001-gitops-mode-is-instance-configuration.md) — the switch whose value
  this invariant is stated against
- [SW-TMP-001](SW-TMP-001-startup-makes-the-gate-set-exactly-the-file.md) — the one writer
- [SW-TMP-002](SW-TMP-002-managed-instance-rejects-crud-writes-with-conflict.md) — how the REST
  writers are closed
- [SW-TMP-004](SW-TMP-004-empty-file-owned-instance-names-its-file.md) — how the empty state this
  invariant allows explains itself
- [ARCH-003](ARCH-003-idempotent-ordered-startup-seeding.md) — the ordered startup step this
  invariant repoints at the file, and leaves intact for criteria
- [SW-011](SW-011-four-predefined-gates-fixed-composition.md) — the definitions that stop being
  seeded from code
- [CON-003](CON-003-predefined-quality-gates-are-immutable.md) — the immutability guarantee this
  narrows to the CRUD surface
- [CON-005](CON-005-api-created-gates-are-never-predefined.md) — the closure of the predefined set,
  which seeding none keeps closed
- [SYS-008](SYS-008-quality-gate-definitions.md) — the capability this invariant scopes
