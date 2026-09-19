# A stable submission cannot carry prerelease content

<!-- markdownlint-disable MD036 -->

**Title**
A stable submission cannot carry prerelease content

**Lens**: CON

**Status**: active

**Description**
An ingestion request that marks itself as a stable (non-prerelease) release must not carry
non-blank prerelease content.
`api-index-api` rejects such a submission whenever the identity's existing entry does not already
force a rejection of its own — i.e. whenever there is no existing entry, or the existing entry is a
prerelease.
Where an existing entry is already **stable**, that takes precedence: the attempt is rejected as
already-indexed ([CON-007](CON-007-stable-api-reference-is-immutable-once-indexed.md),
`409`) rather than as invalid content (`400`), even if the new submission's content is also
invalid — the two invariants are checked in that order, and only one rejection reason is ever
reported for a given attempt.

**Rationale**
Prerelease content is a distinct, separately-exposed payload (see
[SW-027](SW-027-raw-content-endpoint-is-scoped-to-prereleases.md)) that only makes sense
attached to a prerelease entry — a caller retrieves it precisely because the reference is not yet
stable and its content may still change.
A stable submission carrying that field would be a self-contradicting record: an entry declared
final, holding content whose entire purpose is to describe something not yet final.
Rejecting it at the door keeps that contradiction out of the index rather than indexing it and
leaving the raw-content endpoint to decide what a stable-but-prerelease-flagged record means.

**Verification Description**
A test submits an ingestion request marked stable but carrying non-blank prerelease content, once
with no existing entry for that identity and once with an existing **prerelease** entry — both
rejected with `400`.
A third case, with an existing **stable** entry for that identity, is rejected with `409` instead
(per [CON-007](CON-007-stable-api-reference-is-immutable-once-indexed.md)), confirming the
precedence.

## Relations

**Related**

- [SW-027](SW-027-raw-content-endpoint-is-scoped-to-prereleases.md) — the endpoint this
  invariant keeps meaningful, by ensuring prerelease content is never attached to a stable entry

## Changes

- **2026-09-19** — Set active: implementation of `STR-016` began.
</content>
