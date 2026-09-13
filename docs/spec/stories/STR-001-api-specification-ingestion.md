# API specification ingestion and indexing

<!-- markdownlint-disable MD036 -->

**Title**
API specification ingestion and indexing

**Status**: done

**Business Value**
Coverage and quality-gate analysis cannot run without a specification to correlate telemetry
against.
Automated, indexed ingestion removes the manual step of registering each API with Snow-White
before it can be analyzed.
A pipeline-scoped, direct upload path additionally supports monorepos.
A merge request that changes the spec and the implementation together can still be analyzed,
without waiting for a separate publish step to catch up.

**Problem / Context**
API specifications live in external systems (a spec repository such as JFrog Artifactory) and
change independently of when a team wants to run an analysis.
Snow-White needs its own current, queryable copy rather than fetching a spec ad hoc on every
analysis request.
In a monorepo, a merge request can change an API's specification and its implementation in the
same commit.
The stable specification is then not yet published to the external source when the pipeline's QA
run produces telemetry against the new behavior.
The sync-based path alone would leave that telemetry with no specification to correlate against.

**Solution Approach**
A dedicated sync job periodically pulls specifications from configured external sources.
It indexes each one under (service name, API name, API version) — the triple every other part of
Snow-White uses to resolve "this exact API."
Alongside that pull-based path, a specification can also be submitted directly and marked as a
"prerelease", scoped to the pipeline that submitted it.
A downstream consumer resolves a prerelease only if it explicitly opts in, so it never affects an
analysis that expects only stable specifications.
OpenAPI is the supported format today; the criteria model is kept format-agnostic so a second
format is additive later.

**Acceptance Criteria**

- A specification published to a configured external source becomes indexed and queryable within
  one sync cycle, without manual intervention.
- Two specifications differing only in version index and resolve independently.
- A specification in an unsupported format is rejected with a clear error, not silently
  mis-indexed.
- A specification submitted directly as a prerelease becomes resolvable immediately, without
  waiting for a sync cycle, and only by a lookup that explicitly opts in to prereleases.
- Re-submitting a prerelease under the same identity overrides the previous one.
  A prerelease can never override an identity that already holds a stable, sync-indexed
  specification.

**Out of scope**

- Formats other than OpenAPI (planned, not yet supported).
- Any UI or dashboard listing of prerelease specifications.
- Automatic expiry of a prerelease.
  A stale one is cleaned up only by being overridden, or by the pipeline's own housekeeping — not
  by a Snow-White-managed retention period.

## Relations

**Realizes**

- [SYS-001](../specs/SYS-001-periodic-spec-sync.md) — the sync mechanism
- [SYS-002](../specs/SYS-002-indexed-spec-lookup.md) — the indexed lookup
- [SYS-003](../specs/SYS-003-openapi-as-indexed-format.md) — the supported format
- [SYS-013](../specs/SYS-013-prerelease-specification-ingestion.md) — the prerelease
  ingestion path for monorepositories
