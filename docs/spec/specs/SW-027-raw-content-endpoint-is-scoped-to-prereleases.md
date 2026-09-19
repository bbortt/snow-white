# Raw-content lookup only ever answers for a prerelease, with a sniffed media type

<!-- markdownlint-disable MD036 -->

**Title**
Raw-content lookup only ever answers for a prerelease, with a sniffed media type

**Lens**: SW

**Status**: active

**Description**
`api-index-api`'s raw-content endpoint answers `404` for any identity that does not resolve to a
**prerelease** with stored content — whether the identity is not indexed at all, resolves to a
stable entry, or resolves to a prerelease with no content.
When it does resolve, the response's media type is sniffed from the content itself rather than
trusted from a stored field: `application/yaml` if the content starts with `openapi:`,
`text/plain` otherwise.

**Rationale**
Raw content exists so a caller can inspect a prerelease's actual payload while it is still subject
to change; a stable release has no equivalent need, since its content is immutable
([CON-007](CON-007-stable-api-reference-is-immutable-once-indexed.md)) and reachable
through the indexed-lookup contract instead.
Scoping the endpoint to prereleases keeps its purpose — inspecting a draft — from drifting into a
general content-retrieval endpoint.
Sniffing the media type from the content's own shape, rather than a stored content-type field,
avoids a second place where the two could disagree — the response always describes what the bytes
actually are.

**Verification Description**
A test requests raw content for: an unindexed identity, an identity resolving to a stable entry,
and a prerelease entry with no stored content — each asserted `404`.
A further test requests raw content for a prerelease whose content starts with `openapi:` and
asserts `application/yaml`; and for one that does not, asserting `text/plain`.

## Relations

**Related**

- [CON-008](CON-008-stable-submission-cannot-carry-prerelease-content.md) — the
  ingestion-side invariant that keeps prerelease content from ever attaching to a stable entry

## Changes

- **2026-09-19** — Set active: implementation of `STR-016` began.
</content>
