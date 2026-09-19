# A stable API reference is immutable once indexed

<!-- markdownlint-disable MD036 -->

**Title**
A stable API reference is immutable once indexed

**Lens**: CON

**Status**: active

**Description**
Once `api-index-api` has indexed a **stable** (non-prerelease) API reference for a given identity
(service name, API name, API version), no later ingestion attempt for that same identity can change
or replace it — not even another stable submission with different content, and not a prerelease
submission either.
Every such attempt is rejected; the existing entry is left untouched.

**Rationale**
The identity triple is Snow-White's addressing scheme (`SYS-002`): a coverage calculation, a
quality-gate evaluation, and a CLI lookup all resolve "this exact API, this exact version" through
it and expect a stable answer.
If a stable entry could be silently overwritten, a result computed minutes apart against the same
identity could silently be checked against different content, with no record that anything changed.
Rejecting the attempt outright — rather than accepting it as a no-op — makes an accidental
re-ingestion visible to its caller instead of swallowing it.

**Verification Description**
A test ingests a stable API reference, then attempts to ingest another submission (stable or
prerelease) for the identical identity.
The second attempt is rejected with `409` and the stored entry is unchanged.

## Relations

**Related**

- [SYS-002](SYS-002-indexed-spec-lookup.md) — the addressing scheme this invariant protects
- [SW-026](SW-026-prerelease-resubmission-replaces-the-prior-entry.md) — the
  mirror-image behavior for a **prerelease** entry, which is replaceable rather than immutable

## Changes

- **2026-09-19** — Set active: implementation of `STR-016` began.
</content>
