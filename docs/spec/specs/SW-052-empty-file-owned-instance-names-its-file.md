# A calculation against an empty file-owned instance names the file, not a wrong gate name

<!-- markdownlint-disable MD036 -->

**Title**
A calculation against an empty file-owned instance names the file, not a wrong gate name

**Lens**: SW

**Status**: active

**Description**
When a calculation names a quality gate that does not exist, `report-coordinator-api` answers `404`
as it does today.
Where the gate could not exist because the instance is file-owned
([ARCH-025](ARCH-025-gitops-mode-is-instance-configuration.md)) and holds no gates at all,
the rejection says so: that this instance's gates come from a file, and that its file defines none.

That sentence is written by `quality-gate-api`, on the `404` of its own gate read, and
`report-coordinator-api` surfaces it.
The gates are `quality-gate-api`'s to own, so whether a name could have matched at all is a
question only it can answer — it is the side that can see the file.
`report-coordinator-api` keeps its own "does not exist" wording for the one case it is still the
author of: a `404` that carried no body to surface, which is also what a rejection from anything
but `quality-gate-api` itself amounts to.

No additional call is made, and no new endpoint, status code or exception type is introduced: the
status stays `404`, because the named gate genuinely does not exist.
Nothing is read off the ownership header for this, either — the mode is published for a client that
has to decide what to render ([SW-053](SW-053-managed-mode-published-on-gate-reads.md)), not so a
second service can reassemble a sentence the first one could have written.

A file-owned instance that holds gates but not the named one answers exactly as an unmanaged
instance does.
The mode is not an excuse to blur a plain typo.

**Rationale**
A file-owned instance seeds no gates of its own
([CON-016](CON-016-managed-instance-has-exactly-one-gate-writer.md)), so a file that
defines none — a placeholder mounted to get a deployment up, or one narrowed to nothing by an edit —
leaves the instance with no gates and nothing to fall back on.
That is a recoverable state, but it is indistinguishable from a misspelt gate name if the only thing
the operator is told is that the configuration does not exist.
The two have opposite fixes — put the gate in the file and restart, versus correct the name in your
pipeline — and the operator who has just stood up their first file-owned instance is the least
likely to guess which.

Reporting it accurately costs one branch, in the service that already knows the answer, on a
rejection it was already writing.
Placing that branch in the caller instead would put the sentence in the one service that cannot
check whether it is true, and would have it describe a file it has never read.

**Verification Description**
Two tests, one per side of the hand-off.
In `quality-gate-api`, a test reads an absent gate on an instance whose file defines no gates and
asserts the `404` body states that gates are file-defined and that the file defines none; a second
reads an absent gate on a file-owned instance that holds gates, and on an unmanaged one, and
asserts the message is the plain "does not exist" with nothing appended.
In `report-coordinator-api`, a test asserts the rejection it raises carries the owner's message
verbatim when the `404` had one, and its own "does not exist" wording when the `404` carried no
readable body — so a rejection from anything but `quality-gate-api` reads as it does today.

## Relations

**Realizes**

- [NF-005](NF-005-clear-failure-feedback.md) — the failure-feedback quality this case would
  otherwise breach at exactly the moment an operator is least able to diagnose it

**Related**

- [CON-016](CON-016-managed-instance-has-exactly-one-gate-writer.md) — the skipped seeding
  that makes the empty state reachable
- [SW-050](SW-050-startup-makes-the-gate-set-exactly-the-file.md) — the read that accepts an
  empty file as a statement, and refuses an absent one
- [SW-053](SW-053-managed-mode-published-on-gate-reads.md) — the same instance fact, published
  for a client to render from rather than for a service to re-explain
- [ARCH-025](ARCH-025-gitops-mode-is-instance-configuration.md) — why "this instance has no
  gates" is a statement the instance can make about itself
- [SW-013](SW-013-calculation-trigger-is-all-or-nothing.md) — the calculation request this
  rejection belongs to

## Changes

- **2026-11-16** — Set active: implementation of `STR-029` began.
