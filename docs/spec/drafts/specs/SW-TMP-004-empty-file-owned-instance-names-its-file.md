# A calculation against an empty file-owned instance names the file, not a wrong gate name

<!-- markdownlint-disable MD036 -->

**Title**
A calculation against an empty file-owned instance names the file, not a wrong gate name

**Lens**: SW

**Status**: planned

**Description**
When a calculation names a quality gate that does not exist, `report-coordinator-api` answers `404`
as it does today.
Where the gate could not exist because the instance is file-owned
([ARCH-TMP-001](ARCH-TMP-001-gitops-mode-is-instance-configuration.md)) and holds no gates at all,
the rejection says so: that this instance's gates come from a file, and that its file defines none.

The distinction is drawn from the ownership mode published on the gate read
([SW-TMP-003](SW-TMP-003-managed-mode-published-on-gate-reads.md)), which travels on the `404` like
any other response to that read.
No additional call is made to learn it, and no new endpoint, status code or exception type is
introduced: the status stays `404`, because the named gate genuinely does not exist.

A file-owned instance that holds gates but not the named one answers exactly as an unmanaged
instance does.
The mode is not an excuse to blur a plain typo.

**Rationale**
A file-owned instance seeds no gates of its own
([CON-TMP-001](CON-TMP-001-managed-instance-has-exactly-one-gate-writer.md)), so a file that
defines none — a placeholder mounted to get a deployment up, or one narrowed to nothing by an edit —
leaves the instance with no gates and nothing to fall back on.
That is a recoverable state, but it is indistinguishable from a misspelt gate name if the only thing
the operator is told is that the configuration does not exist.
The two have opposite fixes — put the gate in the file and restart, versus correct the name in your
pipeline — and the operator who has just stood up their first file-owned instance is the least
likely to guess which.

Reporting it accurately costs one branch on a response the service already receives and already
inspects for its status.

**Verification Description**
An application test drives a calculation naming an absent gate against a file-owned instance whose
file defines no gates, and asserts a `404` whose message states that gates are file-defined and that
the file defines none.
A second test does the same against a file-owned instance that holds gates, naming one that is not
among them, and asserts the existing "does not exist" rejection, unchanged.
A third asserts an unmanaged instance's rejection is byte-for-byte what it is today.

## Relations

**Realizes**

- [NF-005](NF-005-clear-failure-feedback.md) — the failure-feedback quality this case would
  otherwise breach at exactly the moment an operator is least able to diagnose it

**Related**

- [CON-TMP-001](CON-TMP-001-managed-instance-has-exactly-one-gate-writer.md) — the skipped seeding
  that makes the empty state reachable
- [SW-TMP-001](SW-TMP-001-startup-makes-the-gate-set-exactly-the-file.md) — the read that accepts an
  empty file as a statement, and refuses an absent one
- [SW-TMP-003](SW-TMP-003-managed-mode-published-on-gate-reads.md) — the published mode this
  rejection reads, on the same response it was already handling
- [ARCH-TMP-001](ARCH-TMP-001-gitops-mode-is-instance-configuration.md) — why "this instance has no
  gates" is a statement the instance can make about itself
- [SW-013](SW-013-calculation-trigger-is-all-or-nothing.md) — the calculation request this
  rejection belongs to
