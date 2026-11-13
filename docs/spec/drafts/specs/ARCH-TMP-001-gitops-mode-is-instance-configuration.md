# Gate ownership is instance configuration, never state on a gate record

<!-- markdownlint-disable MD036 -->

**Title**
Gate ownership is instance configuration, never state on a gate record

**Lens**: ARCH

**Status**: planned

**Description**
Whether a `quality-gate-api` instance's gates are defined by a file or through its REST surface is
one configuration switch on the instance, read at startup.
Either every gate on that instance is file-owned, or none is.

That switch is the gate file's own presence: an instance configured with a path to a gate file is
file-owned, and one configured without is not.
There is no second boolean that could disagree with the file, and no state to reconcile between
them.
On Kubernetes the file is a mounted `ConfigMap`; nothing in the service knows that.

No gate record carries an ownership or `managed` flag, and nothing in the `QualityGateConfig`
payload marks an individual gate as file-owned.
A mixed estate — some gates owned by a file, others editable through the API on the same instance —
is excluded.

**Rationale**
A per-gate flag is strictly more flexible and strictly worse at the only job the switch exists to
do, which is to produce exactly one writer.
With it, every instance still has two writers, and the arbiter between them is itself a piece of
state one of those writers has to set — whichever writer created a gate decides who may change it,
which is circular rather than authoritative.
An operator who inherits such an instance cannot tell what owns a gate without reading rows.

Instance configuration is answerable from the deployment that sets it, which is the same place the
rest of the instance's behaviour is declared and reviewed.
Making the file itself the switch keeps that answer to one artifact: the deployment either mounts a
gate file or it does not, and an operator reading it learns both that the instance is file-owned and
what it enforces.
It is also what lets a client disable write controls once per page rather than reason per row
([SW-TMP-003](SW-TMP-003-managed-mode-published-on-gate-reads.md)), and what makes
"this instance's gates are managed elsewhere" a true statement about the instance rather than about
a row ([SW-TMP-002](SW-TMP-002-managed-instance-rejects-crud-writes-with-conflict.md)).

Two costs are real and accepted.
A team that wants one hand-tuned experimental gate alongside its file-defined ones needs a second
instance, or needs to put that gate in the file; spending an instance is a smaller price than a
system that cannot say who owns a gate.
And because the switch is a mount, the gate file lives with the deployment rather than in the
repository of whoever the gate is about: a team changing its own quality bar changes a file in the
platform's deployment repository, and the change ships on a restart rather than on a merge of
theirs.
That is a worse boundary than a per-repository gate file would be, and strictly cheaper than any
mechanism that would deliver one.

**Verification Description**
A review confirms neither the gate entity nor the `QualityGateConfig` schema gained an ownership
field.
An integration test flips the switch and asserts the change in behaviour applies to every gate the
instance holds, including one created through the API before the switch was turned on — so the
mode is observably a property of the instance and not of the records.

## Relations

**Related**

- [CON-TMP-001](CON-TMP-001-managed-instance-has-exactly-one-gate-writer.md) — the invariant this
  structure exists to make enforceable
- [SW-TMP-002](SW-TMP-002-managed-instance-rejects-crud-writes-with-conflict.md) — the refusal this
  switch governs
- [SW-TMP-003](SW-TMP-003-managed-mode-published-on-gate-reads.md) — how the switch's value reaches
  a client
- [SYS-008](SYS-008-quality-gate-definitions.md) — the gate-definition capability this decision
  scopes to an instance
- [CON-005](CON-005-api-created-gates-are-never-predefined.md) — the existing precedent that a
  gate's provenance is decided by the write path, not by what a caller claims
