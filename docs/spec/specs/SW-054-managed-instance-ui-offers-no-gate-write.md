# A file-owned instance's UI offers no gate write, by control or by deep link

<!-- markdownlint-disable MD036 -->

**Title**
A file-owned instance's UI offers no gate write, by control or by deep link

**Lens**: SW

**Status**: active

**Description**
When the gate reads report file ownership
([SW-053](SW-053-managed-mode-published-on-gate-reads.md)), the web UI's quality gate
screens offer no way to create, edit or delete a gate.

On the gate list, the create control and every row's edit and delete control render disabled,
carrying the reason, and the screen states once that this instance's gates come from its gate file.
The reason is published at the instance level and not per row, because unlike a predefined gate —
whose row shows `isPredefined` and whose controls are omitted on that basis today — file ownership
is a fact about the instance that no row can explain.

The `new`, `edit` and `delete` routes under the gate screens refuse on a file-owned instance: they
render the same reason instead of a form or a confirmation dialog.
Each of those routes is reachable by deep link, bookmark or browser history without the list ever
being rendered, so suppressing the controls alone leaves a reachable form whose submit earns a
`409` ([SW-051](SW-051-managed-instance-rejects-crud-writes-with-conflict.md)).
No `POST`, `PUT` or `DELETE` on a gate path is issued from any of these screens while the mode is
on.

A gate's detail screen and the list itself are unchanged, including pagination, sorting and the
criteria they display.
Read access is not what the mode restricts.

With the mode off, every one of these screens behaves exactly as it does today, the omission of a
predefined gate's edit and delete controls included.

**Rationale**
The server-side refusal is what makes the file authoritative; it is not what makes the product
usable.
An operator who is shown a create button, fills in a gate and is handed a `409` has learned the
rule by breaking it, and has learned nothing about where this instance's gates actually come from.
Stating the ownership on the screen that would otherwise offer the write turns the same rule into
an explanation, and it is the only place the operator is looking at the time.

Disabled and not hidden, for the one case the codebase already answers the other way: a predefined
gate's controls are omitted, and that is right because the row says `isPredefined` beside them.
Nothing on the screen would account for controls that vanish on a file-owned instance, so hiding
them there trades a confusing failure for a confusing absence.

The routes and not only the controls, because an edit URL outlives the list that produced it.
A bookmarked `/quality-gate-config/{name}/edit` from before the switch, or a link pasted in a
ticket, reaches the form directly; `new` needs no gate to exist at all.
Leaving those routes to the server's `409` is exactly the outcome publishing the mode was meant to
avoid, and it is reachable without the client ever having been careless.

**Verification Description**
Webapp component tests, with the gate reads stubbed to report file ownership, assert the list
renders the create, edit and delete controls disabled with the reason and states the instance's
gates are file-defined; that the `new`, `edit` and `delete` routes rendered directly — not navigated
to from the list — render that reason and no form or dialog; and that no `POST`, `PUT` or `DELETE`
on a gate path is issued by any of them.
The same tests with the reads reporting no file ownership assert today's markup on every one of
those screens, including that a predefined gate's edit and delete controls stay omitted and a
non-predefined gate's stay enabled.

## Relations

**Realizes**

- [CON-016](CON-016-managed-instance-has-exactly-one-gate-writer.md) — keeps the client
  from attempting the writes the invariant forbids

**Related**

- [SW-053](SW-053-managed-mode-published-on-gate-reads.md) — the header this behaviour reads
  the mode from
- [SW-051](SW-051-managed-instance-rejects-crud-writes-with-conflict.md) — the refusal these
  screens exist to never earn
- [ARCH-025](ARCH-025-gitops-mode-is-instance-configuration.md) — the switch the published
  mode reflects
- [SW-009](SW-009-quality-gate-crud-contract.md) — the write contract these screens drive with the
  mode off
- [CON-003](CON-003-predefined-quality-gates-are-immutable.md) — the per-gate immutability whose
  existing treatment in this UI the mode does not change
- [NF-005](NF-005-clear-failure-feedback.md) — the feedback quality a stated reason holds up where a
  bare `409` does not

## Changes

- **2026-11-16** — Set active: implementation of `STR-029` began.
