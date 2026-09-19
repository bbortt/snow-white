# Re-ingesting an API under an identity already holding a prerelease replaces it

<!-- markdownlint-disable MD036 -->

**Title**
Re-ingesting an API under an identity already holding a prerelease replaces it

**Lens**: SW

**Status**: active

**Description**
When `api-index-api` receives an ingestion request for an identity (service name, API name, API
version) whose existing entry is itself a **prerelease**, it deletes that entry and persists the
new submission in its place — it does not reject the request for already existing, regardless of
whether the new submission is itself stable or another prerelease.
This is the one case in which ingestion for an already-indexed identity succeeds rather than being
rejected for that reason (contrast
[CON-007](CON-007-stable-api-reference-is-immutable-once-indexed.md), which rejects every
such attempt once the existing entry is stable).
The replacement still only happens if the new submission itself is valid: a stable new submission
carrying prerelease content is rejected by
[CON-008](CON-008-stable-submission-cannot-carry-prerelease-content.md) before the old
prerelease entry is touched.

**Rationale**
A prerelease exists to be iterated on before it stabilizes; a caller re-uploading it under the same
identity is refining a draft, not corrupting a record.
Requiring a delete-then-reingest round trip from the caller would make the common iteration loop
clumsier for no integrity benefit, since nothing downstream depends on a prerelease's content
staying fixed the way it depends on a stable release's.

**Verification Description**
A test ingests a prerelease API reference, then ingests another **valid** submission (stable with
no prerelease content, or another prerelease) for the identical identity.
The request succeeds, the prior entry is gone, and a lookup for that identity resolves to the new
submission's content.

## Relations

**Related**

- [SYS-002](SYS-002-indexed-spec-lookup.md) — the addressing scheme this replacement behavior
  operates within
- [CON-007](CON-007-stable-api-reference-is-immutable-once-indexed.md) — the
  mirror-image invariant for a **stable** entry, which is never replaceable

## Changes

- **2026-09-19** — Set active: implementation of `STR-016` began.
</content>
