# Startup makes the gate set exactly what the file says

<!-- markdownlint-disable MD036 -->

**Title**
Startup makes the gate set exactly what the file says

**Lens**: SW

**Status**: planned

**Description**
On a file-owned instance ([ARCH-TMP-001](ARCH-TMP-001-gitops-mode-is-instance-configuration.md)),
`quality-gate-api` reads its gate file during startup and makes the gate set exactly the set that
file defines.
A gate the file names and the database lacks is created; a gate both hold is replaced by the file's
definition; a gate the database holds and the file does not name is deleted.
Matching is by name, which is already the gate's unique identity.

A definition replaces the whole gate: the criteria set, the threshold and the description are taken
from the file as given, and a field the file omits is cleared rather than preserved.
This does not inherit [SW-010](SW-010-update-merge-patch-empty-criteria-clears.md)'s merge-patch
semantics, because a file is a statement of what the gate is, not a description of a change to it.

The read is all-or-nothing.
A file that cannot be understood — malformed, or naming a criterion that does not exist — fails
startup with an error naming what it could not resolve, and no gate record is written.

A gate file that is configured but absent or unreadable fails startup for the same reason.
An absent file is never read as an empty gate set: a volume that failed to mount would otherwise
delete every gate the instance has.
A file that is present and defines no gates is a valid statement, and leaves the instance holding
none ([SW-TMP-004](SW-TMP-004-empty-file-owned-instance-names-its-file.md)).

The file is accepted as either JSON or YAML, which costs no new dependency, and its schema is the
`QualityGateConfig` payload `POST /quality-gates` already takes.

**Rationale**
Full replacement is what makes the file readable as the instance's gate set.
An upsert that left unnamed gates alone would mean the file no longer answers "what does this
instance enforce?", which is the question the change exists to make answerable from a repository.

Deleting what the file does not name is safe here in a way it would not be for a file pushed at a
running instance.
The file arrives with the deployment as one artifact rather than as whatever a CI checkout happened
to contain, and the only way to narrow it is to edit it and ship it through the same review as any
other deployment change.
The hazard that would otherwise justify a non-destructive default — a partial or mis-pathed file
pruning gates nobody meant to touch — is answered by refusing an unreadable or absent file instead
of interpreting it.

Startup is the write moment because the deployment already defines it.
A gate change is a change to the file and a restart of the instance, which is the same event that
ships every other change to that instance's behaviour, carrying the same diff, author and review.
It also costs no new surface: the step runs in the slot
[ARCH-003](ARCH-003-idempotent-ordered-startup-seeding.md) already orders, after criteria seeding,
which a gate definition needs in place before it can be resolved.

**Verification Description**
An integration test starts a file-owned instance against a database holding gates `A` and `B`, with
a file naming a changed `A` and a new `C`, and asserts the instance afterwards holds exactly `A` as
the file defines it and `C`, with `B` gone.
A second restarts that instance against the same file and asserts nothing changed.
A third starts an instance whose file names an unknown criterion and asserts startup fails, the
error names the criterion, and every gate record is as it was.
A fourth starts an instance configured with a gate file that is not present and asserts startup
fails and no gate was deleted.

## Relations

**Realizes**

- [SYS-008](SYS-008-quality-gate-definitions.md) — the gate-definition capability, reached from a
  file instead of a form

**Related**

- [CON-TMP-001](CON-TMP-001-managed-instance-has-exactly-one-gate-writer.md) — the invariant that
  makes this read the only writer
- [SW-TMP-002](SW-TMP-002-managed-instance-rejects-crud-writes-with-conflict.md) — the writes this
  read replaces on a file-owned instance
- [SW-TMP-004](SW-TMP-004-empty-file-owned-instance-names-its-file.md) — what an instance whose file
  defines no gates tells a calculation
- [ARCH-003](ARCH-003-idempotent-ordered-startup-seeding.md) — the ordered startup step this read
  occupies, and the criteria seeding it depends on
- [SW-011](SW-011-four-predefined-gates-fixed-composition.md) — the hardcoded definitions the file
  replaces as that step's input
- [SW-009](SW-009-quality-gate-crud-contract.md) — the CRUD contract whose payload shape the file
  reuses
- [SW-010](SW-010-update-merge-patch-empty-criteria-clears.md) — the update semantics this
  deliberately does not inherit
