# Prerelease specification ingestion, scoped to a single pipeline run

<!-- markdownlint-disable MD036 -->

**Title**
Prerelease specification ingestion, scoped to a single pipeline run

**Lens**: SYS

**Status**: active

**Description**
In addition to periodic sync, Snow-White accepts a specification submitted directly and marked as
a prerelease.
A prerelease is indexed under the same (service name, API name, API version) identity as a stable
specification but is clearly distinguished from it.
Submitting a prerelease under an identity that already holds a prerelease overrides it.
An identity that already holds a stable specification is never overridden this way.
A consumer resolves a prerelease only by explicitly opting in.
A lookup that does not opt in ignores it exactly as if it did not exist.

**Rationale**
In a monorepo, an API's specification and its implementation change in the same commit or merge
request.
The stable specification is then not yet published to the external source (e.g. Artifactory) when
a QA pipeline exercises the new behavior.
Without a prerelease path, that telemetry would be silently discarded for lack of a matching
indexed specification, and no analysis would run at all.
Restricting resolution to an explicit opt-in keeps a prerelease from unintentionally affecting an
unrelated analysis that only meant to look at stable specifications.

**Verification Description**
A test uploads a specification marked as a prerelease, under an identity with no existing stable
specification.
The test confirms the prerelease is not resolved by a lookup that does not opt in, and is resolved
by one that does.
A second prerelease upload under the same identity overrides the first, rather than being rejected
as a duplicate.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — specs must be available before
  they can be correlated with telemetry, even when the stable copy is not yet published

**Related**

- [SYS-001](SYS-001-periodic-spec-sync.md) — the pull-based counterpart this supplements
- [SYS-002](SYS-002-indexed-spec-lookup.md) — the identity and index scheme a prerelease
  shares with a stable specification
