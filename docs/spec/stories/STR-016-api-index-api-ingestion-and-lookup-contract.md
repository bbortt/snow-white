# Documented ingestion mutability, lookup filtering, and raw-content contract for `api-index-api`

<!-- markdownlint-disable MD036 -->

**Title**
Documented ingestion mutability, lookup filtering, and raw-content contract for `api-index-api`

**Status**: done

**Business Value**
`api-index-api` is one of two microservices still missing a retrace, alongside
`otel-event-filter-stream` (explicitly left out of scope by `STR-008`); `quality-gate-api`,
`report-coordinator-api`, `openapi-coverage-stream`, and `api-gateway` were already retraced
(`STR-008`, `STR-009`, `STR-011`, `STR-015`).
Its most consequential behaviors are exactly the ones a caller cannot see from the OpenAPI schema
alone: that a stable release, once indexed, can never be overwritten while a prerelease under the
same identity can be silently replaced by a later submission; that a stable submission carrying
prerelease content is rejected outright rather than silently accepted; that the raw-content lookup
only ever answers for a prerelease; and that the service/API name list filters match by prefix, not
substring or exact value.
Pinning these down gives `report-coordinator-api` (which resolves APIs through this service, per
`SW-013`), the CLI, and future changes something to check against instead of only the running code.

**Problem / Context**
`microservices/api-index-api` indexes ingested API specifications under the (service name, API
name, API version) triple `SYS-002` already names as Snow-White's addressing scheme, and answers
lookups against that index.
A source read of the module's Java backend (`ApiIndexService`, `ApiIndexResource`,
`ApiReferenceSpecification`, `ApiReference`, against the existing `ApiIndexResourceIT`/
`ApiIndexServiceUnitTest` black-box and unit suites) found no code anchored to any spec in the
corpus below `SYS-002`/`SYS-003` — this module's own ingestion and lookup behavior is currently
undocumented outside the code.

Several behaviors stood out as consequential enough to need a spec of their own:

- `ApiIndexService.persist` rejects (`409`) any ingestion attempt for an identity whose existing
  entry is a stable release, regardless of whether the new submission is itself stable or a
  prerelease — a stable entry is final once indexed.
- The same method replaces, rather than rejects, an existing **prerelease** entry when a new
  submission arrives for the same identity — deleting the old row before saving the new one.
- A submission marked stable (not a prerelease) that carries non-blank prerelease content is
  rejected (`400`), unless an existing stable entry for that identity already forces the `409`
  above — a stable release's own payload can never claim to be a prerelease body.
- `ApiIndexResource.getRawApiContent` answers `404` whenever the resolved entry is not itself a
  prerelease, and sniffs the response media type from the content's own shape (`application/yaml`
  if it starts with `openapi:`, `text/plain` otherwise) rather than trusting a stored content type.
- `ApiReferenceSpecification`'s `serviceName`/`apiName` list filters match a case-insensitive
  **prefix** ("starts with"), not a substring or an exact value.

**Solution Approach**
Add two constraint specs for the ingestion-mutability invariants (a stable entry is immutable; a
stable submission cannot carry prerelease content), one software spec for the prerelease-override
behavior, one software spec for the raw-content endpoint's prerelease-only scope and sniffed media
type, and one software spec for the prefix-match list filtering.
No code changes — this documents the existing, already-tested behavior and anchors it.

**Acceptance Criteria**

- A spec states that an ingestion attempt is rejected with `409` whenever the target identity's
  existing entry is a stable release, regardless of the incoming submission's own prerelease flag,
  and that no existing data is altered by the rejected attempt.
- A spec states that an ingestion attempt for an identity whose existing entry is a prerelease
  replaces that entry (the prior row is deleted, the new one persisted) rather than being rejected
  for already existing, regardless of the incoming submission's own prerelease flag, as long as the
  incoming submission is itself valid.
- A spec states that a stable (non-prerelease) submission carrying non-blank prerelease content is
  rejected with `400` whenever no existing stable entry already forces a `409` instead, and that the
  two invariants report only one reason per attempt.
- A spec states that the raw-content endpoint answers `404` for any identity that does not resolve
  to a prerelease (absent, or resolved but not a prerelease, or a prerelease with no stored
  content), and that a found prerelease's content is returned as `application/yaml` when it starts
  with `openapi:` and `text/plain` otherwise.
- A spec states that the `serviceName` and `apiName` list-filter query parameters match
  case-insensitively by prefix, so a filter value matches only entries whose corresponding field
  starts with it.

**Out of scope**

- Anchoring the system-level capability (`SYS-002`/`SYS-003`) this module realizes as the indexed
  lookup itself.
  That remains unanchored here, consistent with the `api-gateway`, `quality-gate-api`, and
  `report-coordinator-api` retraces, which likewise related to their `SYS`/`STK` parents without
  claiming them; closing that `SYS`/`STK` coverage gap is its own story.
- `ApiIndexPropertiesValidator`'s required-configuration fail-fast check.
  The same per-module `*PropertiesValidator` pattern exists across every microservice and none of
  the prior retraces speced it either; documented once, generically, if it is ever pulled out of
  this per-module repetition.
- The generic `{code, message}` error envelope shared cross-cutting convention, already explicitly
  left unclaimed by `STR-009`.
- `ApiReference`'s composite-key shape (the `(otelServiceName, apiName, apiVersion)` `@IdClass`)
  and `indexedAt` stamping — these implement `SYS-002`'s addressing scheme directly rather than
  pinning a decision of their own; `SYS-002` already states the triple is the addressing key.
- `OpenApiConfiguration`'s Springdoc server-URL wiring — generic per-module Springdoc bootstrap, not
  a decision specific to this module's behavior.
- PostgreSQL outage tolerance for this module's own persistence — `CON-002` already states this is
  not yet covered for any of the three microservices sharing this persistence shape, `api-index-api`
  included; unchanged by this retrace.

## Relations

**Realizes**

- [CON-007](../specs/CON-007-stable-api-reference-is-immutable-once-indexed.md) — the
  stable-entry immutability invariant
- [CON-008](../specs/CON-008-stable-submission-cannot-carry-prerelease-content.md) — the
  stable-payload data-integrity invariant
- [SW-026](../specs/SW-026-prerelease-resubmission-replaces-the-prior-entry.md) — the
  prerelease-override behavior
- [SW-027](../specs/SW-027-raw-content-endpoint-is-scoped-to-prereleases.md) — the
  raw-content endpoint's scope and media-type sniffing
- [SW-028](../specs/SW-028-list-filters-match-by-case-insensitive-prefix.md) — the
  prefix-match list filtering

**Related**

- [SYS-002](../specs/SYS-002-indexed-spec-lookup.md) — the indexed-lookup capability this
  module's ingestion and query behavior implement
- [SYS-003](../specs/SYS-003-openapi-as-indexed-format.md) — the format this module indexes
- [SW-013](../specs/SW-013-calculation-trigger-is-all-or-nothing.md) — the consumer that
  resolves APIs through this service's lookup
- [STR-009](../stories/STR-009-quality-gate-configuration-and-criteria-management-api.md) — a
  sibling retrace that likewise left its `*PropertiesValidator` and `SYS` anchoring out of scope
- [STR-015](../stories/STR-015-api-gateway-ingress-routing-security-and-spa-fallback.md) — a
sibling retrace with the same shape (undocumented existing behavior, no code changes)
</content>
