# Quality gates as code, with one writer per instance

<!-- markdownlint-disable MD036 -->

**Title**
Quality gates as code, with one writer per instance

**Status**: planned

**Business Value**
A quality gate decides whether a build passes, which makes it part of the build contract — and it is
the only part of that contract that does not live in a repository.
Today a gate change has no diff, no author, no PR and no history beyond whatever the database
retains; it cannot be promoted from staging to production except by re-entering it, and it cannot be
tried out without mutating state every pipeline shares.
Putting the gate in a file makes a stricter bar a reviewable change, and makes the instance that
enforces it reproducible from the repository rather than from whoever last used the web UI.

**Problem / Context**
`quality-gate-api` seeds four predefined gates on every startup
([ARCH-003](../specs/ARCH-003-idempotent-ordered-startup-seeding.md),
[SW-011](../specs/SW-011-four-predefined-gates-fixed-composition.md)) and otherwise accepts gate
definitions only through its REST surface
([SW-009](../specs/SW-009-quality-gate-crud-contract.md)), which the web UI drives.
The CLI's configuration carries which gate to _use_ (`"qualityGate": "sonarqube"`), never what that
gate contains.
No file-based gate definition exists anywhere.

Adding one is not the hard part — `QualityGateConfig` already exists as an OpenAPI component, so the
file's schema can be exactly the payload `POST /quality-gates` takes today.
The hard part is that a file plus the existing REST surface produces two writers and no arbiter,
which is worse than one bad writer: a gate can disagree with the repository that claims to define
it, and nothing says which one is wrong.

Seeding is a third writer, and the one that is easy to miss.
`initPredefinedQualityGates` upserts the four predefined gates by name on _every_ boot, so any
scheme where a file is applied and the seeded gates are then cleared is undone by the next restart.
That rules out the obvious compromise of seeding the defaults and treating them as file-managed.

**Solution Approach**
An instance's gates are file-owned exactly when that instance is deployed with a gate file — on
Kubernetes, a mounted `ConfigMap`
([ARCH-TMP-001](../specs/ARCH-TMP-001-gitops-mode-is-instance-configuration.md)).
There is no per-gate ownership flag and no separate mode boolean to disagree with the file, so an
operator answers "what owns my gates, and what are they?" from the deployment rather than by
querying a database.

The file is read at startup, in the slot the seeding step already occupies, and it replaces that
step's hardcoded definitions as its input: the gate set afterwards is exactly what the file says,
including the deletion of gates the file does not name
([SW-TMP-001](../specs/SW-TMP-001-startup-makes-the-gate-set-exactly-the-file.md)).
That makes startup the single writer rather than a third one to work around
([CON-TMP-001](../specs/CON-TMP-001-managed-instance-has-exactly-one-gate-writer.md)), and the CRUD
writes answer `409` to close the others
([SW-TMP-002](../specs/SW-TMP-002-managed-instance-rejects-crud-writes-with-conflict.md)).
A gate change is therefore a change to the file and a restart — the same event that ships every
other change to the instance, with the same diff, author and review.
An instance switched into the mode with predefined gates already in its database starts normally;
they are ordinary gates the file replaces or removes.
A configured file that is absent or unreadable fails startup rather than being read as an empty gate
set, because a failed mount must not delete every gate.
A file that is present and defines no gates leaves the instance empty, so the rejection it gives a
calculation names the file rather than blaming the gate name
([SW-TMP-004](../specs/SW-TMP-004-empty-file-owned-instance-names-its-file.md)).

Reads are untouched, and the mode travels on them so a client learns it before offering a control
that would be refused
([SW-TMP-003](../specs/SW-TMP-003-managed-mode-published-on-gate-reads.md)).
A UI that renders create and edit controls which then fail server-side is worse than one that
renders them disabled with a reason.

Nothing new is exposed on the network: no apply endpoint, no CLI command, and therefore no
authorisation question of its own.
The file's schema is the `QualityGateConfig` payload `POST /quality-gates` already takes, accepted
as either JSON or YAML, which costs no new dependency.
The UI's read-only state is outside clew's scope and carries no spec of its own; the server-side
mode it depends on is what this story specifies.

**Acceptance Criteria**

- A gate definition committed to a repository and deployed as an instance's gate file produces
  exactly that gate, and restarting against the same file changes nothing.
- An instance whose file names one of two gates it already holds keeps the named one as the file
  defines it and no longer holds the other.
- A file naming a criterion that does not exist fails startup, says which criterion it did not
  recognise, and leaves every gate record as it was.
- An instance configured with a gate file that is absent or unreadable fails startup and deletes no
  gate.
- On a managed instance, `POST`, `PUT` and `DELETE` on the gate paths answer `409` with an error
  code naming the mode, and the targeted record is unchanged.
- On a managed instance, every read — the gate list, a gate by name, and the criteria catalog —
  answers exactly as it does today.
- A managed instance restarted against a database holding only the gates its file defines still
  holds only those gates afterwards: no predefined gate reappears.
- A calculation naming a gate against a managed instance whose file defines no gates is rejected
  with a message stating that gates are file-defined and that the file defines none — not merely
  that the name does not exist.
  Naming an absent gate on a managed instance that _does_ hold gates is rejected exactly as it is
  today.
- A managed instance starts normally against a database that still holds predefined gates seeded
  before the switch was turned on.
  It neither refuses to come up nor treats them specially; they are ordinary gates the file replaces
  or removes.
- An unmanaged instance behaves exactly as it does today, seeding included, with no new status code
  reachable on any gate path.
- The web UI renders gate create, edit and delete controls disabled, with a reason, on a managed
  instance, and never issues a write it knows will be refused.

**Out of scope**

- Continuous reconciliation, and picking up a changed file without a restart.
  The deployment is the apply.
  Kubernetes updates a mounted `ConfigMap` in place, but the instance reads it only at startup; a
  gate change takes effect when the pod does.
- A per-team or per-repository gate file.
  The file belongs to the deployment, so a team's quality bar is changed in the platform's
  deployment repository rather than in their own.
  Moving that boundary needs a delivery mechanism this route deliberately does not build.
- Exporting existing database-defined gates into a file.
  A reasonable companion, and separable.
- Versioning or history of gate definitions inside Snow-White.
  Git is the point of the change.
- Migrating an instance into the mode.
  Predefined gates seeded before the switch are left for the file to deal with on the next start;
  nothing detects, converts or removes them on the way in.

## Relations

**Realizes**

- [ARCH-TMP-001](../specs/ARCH-TMP-001-gitops-mode-is-instance-configuration.md) — where the mode
  lives, and why not on the gate
- [SW-TMP-001](../specs/SW-TMP-001-startup-makes-the-gate-set-exactly-the-file.md) — what the
  startup read does to the gate set, and what it refuses
- [SW-TMP-002](../specs/SW-TMP-002-managed-instance-rejects-crud-writes-with-conflict.md) — how a
  managed instance refuses a REST write
- [SW-TMP-003](../specs/SW-TMP-003-managed-mode-published-on-gate-reads.md) — how a client learns
  the mode before it offers a control
- [SW-TMP-004](../specs/SW-TMP-004-empty-file-owned-instance-names-its-file.md) — what a managed
  instance says when its file defines no gates
- [CON-TMP-001](../specs/CON-TMP-001-managed-instance-has-exactly-one-gate-writer.md) — the
  invariant that makes the file a source of truth rather than a claim

**Related**

- [SYS-008](../specs/SYS-008-quality-gate-definitions.md) — the capability a file-defined gate is
  another way to reach
- [SW-009](../specs/SW-009-quality-gate-crud-contract.md) — the CRUD contract this story leaves
  intact on an unmanaged instance and suspends on a managed one
- [SW-010](../specs/SW-010-update-merge-patch-empty-criteria-clears.md) — the field-level update
  semantics the file read does not inherit, because a file is a whole definition
- [SW-011](../specs/SW-011-four-predefined-gates-fixed-composition.md) — the gates a managed
  instance stops seeding
- [ARCH-003](../specs/ARCH-003-idempotent-ordered-startup-seeding.md) — the startup writer this
  story repoints at the file instead of silencing, and the reason the obvious compromise does not
  work
- [CON-003](../specs/CON-003-predefined-quality-gates-are-immutable.md) — the immutability
  guarantee a managed instance narrows to its CRUD surface
- [CON-005](../specs/CON-005-api-created-gates-are-never-predefined.md) — the closure of the
  predefined set, which a managed instance keeps closed by seeding none
- [SW-013](../specs/SW-013-calculation-trigger-is-all-or-nothing.md) — the all-or-nothing
  precedent the file read follows across a multi-gate file
- [NF-005](../specs/NF-005-clear-failure-feedback.md) — the failure-feedback quality the empty
  instance's rejection has to hold up
- [STR-009](STR-009-quality-gate-configuration-and-criteria-management-api.md) — the increment
  that built the REST surface this one puts a second owner in front of
