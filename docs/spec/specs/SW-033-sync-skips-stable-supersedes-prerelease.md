# A sync cycle skips an identity already indexed as stable, and supersedes a prerelease one

<!-- markdownlint-disable MD036 -->

**Title**
A sync cycle skips an identity already indexed as stable, and supersedes a prerelease one

**Lens**: SW

**Status**: active

**Description**
Before publishing a loaded specification, `api-sync-job` asks the index whether the (service name,
API name, API version) identity is already held — and asks it with prereleases **excluded**.
An identity the index answers for is skipped: the cycle does not re-submit it, so running the sync
again over an unchanged source publishes nothing.
An identity held **only** by a prerelease is reported absent, so the cycle publishes for it.
Every specification the sync publishes is submitted as a stable release; the job never submits a
prerelease.
Together this means a prerelease entry is superseded by the stable specification as soon as that
specification appears in the external source, while a stable entry is never re-submitted.

**Rationale**
The sync is a repeating, unattended pull, so it must be idempotent: without the existence check
every cycle would re-submit every specification and be rejected by the index's own immutability
invariant ([CON-007](CON-007-stable-api-reference-is-immutable-once-indexed.md)), turning normal
operation into a stream of conflicts that hides a real one.
Excluding prereleases from that check is the deliberate half.
A prerelease exists precisely because the stable specification was not published yet
([SYS-013](SYS-013-prerelease-specification-ingestion.md)); treating it as "already indexed" would
make the pipeline-scoped stand-in permanently shadow the real specification it stood in for.
Asking the question without prereleases lets the stable specification take over the identity on the
first cycle after publication, with no operator action and no cleanup step.

**Verification Description**
A test indexes an identity as a stable entry, then runs a cycle over a source offering the same
identity, and asserts nothing is submitted for it.
A second test indexes the same identity as a **prerelease only**, runs the cycle, and asserts the
specification is submitted and that the identity afterwards resolves to the stable entry rather
than the prerelease.
The second test fails if the existence check is asked with prereleases included.

## Relations

**Related**

- [SYS-001](SYS-001-periodic-spec-sync.md) — the repeating pull this idempotence makes safe
- [SYS-002](SYS-002-indexed-spec-lookup.md) — the identity triple the existence check is asked in
- [SYS-013](SYS-013-prerelease-specification-ingestion.md) — the prerelease path whose entries a
  stable sync supersedes
- [CON-007](CON-007-stable-api-reference-is-immutable-once-indexed.md) — the index-side invariant
  that makes re-submitting a stable identity a conflict
- [SW-026](SW-026-prerelease-resubmission-replaces-the-prior-entry.md) — the index-side replacement
  this cycle relies on to take over a prerelease identity

## Changes

- **2026-09-29** — Set active: implementation of STR-019 began.
