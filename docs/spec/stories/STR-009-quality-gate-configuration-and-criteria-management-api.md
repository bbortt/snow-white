# Documented quality-gate configuration and criteria management for `quality-gate-api`

<!-- markdownlint-disable MD036 -->

**Title**
Documented quality-gate configuration and criteria management for `quality-gate-api`

**Status**: done

**Business Value**
`SYS-008` names "configurable quality-gate definitions, predefined and user-defined" as a system
capability, but nothing in the spec corpus pins down the component that actually owns this data:
what a client can create, read, update, or delete; which four gates ship built in and what each
one checks; or the two behaviors that most affect a caller's outcome — that a predefined gate's
name is a reliable, unspendable reference, and that an update silently wipes a gate's criteria if
the caller resends an empty list.
Pinning these down gives future changes (and future callers, like the `api-gateway` UI or the CLI)
something to check against instead of only the running code.

**Problem / Context**
`microservices/quality-gate-api` is the sole owner of quality-gate configuration and the fixed
criteria catalog; it has zero outbound calls to another service (no Kafka, no WebClient/REST
client) and is purely a synchronous CRUD/config surface consumed over REST.
A source read of the module (`QualityGateResource`, `CriteriaResource`, `QualityGateService`,
`OpenApiCoverageConfigurationService`, `DefaultOpenApiQualityGates`, `DatabaseInitializer`, both
mappers, and the black-box `QualityGateApiAppTest`/`CriteriaApiAppTest` suites) found no code
anchored to any spec in the corpus — this module's actual behavior is currently undocumented
outside the code and its OpenAPI contracts.
Two behaviors stood out as consequential enough to need a spec of their own rather than being
folded into a general CRUD description:

- A gate created through the API can never be persisted as predefined — `QualityGateService`
  forces `isPredefined = false` on every created record regardless of what the request body says.
  This is what makes `CON-003`'s guarantee (a predefined gate can't be changed or deleted) hold in
  practice: nothing a client sends can produce a new predefined gate to begin with.
- `PUT` on a gate applies merge-patch semantics via reflection (`ObjectUtils.copyNonNullFields`):
  only fields the caller actually sent overwrite the persisted record.
  But the mapper always produces a non-null (possibly empty) criteria `Set`, even when the request
  omits the criteria field entirely — so an update that doesn't intend to touch criteria at all
  still clears them if the mapper's default resolves to empty.

**Solution Approach**
Add one constraint spec for the "never predefined via the API" invariant, two architecture specs
for the two structural decisions this module makes about data it owns (criteria metadata lives in
the `OpenApiCoverageCriteria` enum, not the database; startup seeding is idempotent and ordered),
and four software specs for the module's observable REST behavior: the CRUD lifecycle and its
defined error cases, the merge-patch update semantics and its empty-criteria-clears edge case, the
fixed composition of the four predefined gates, and the criteria catalog endpoint.
No code changes — this documents the existing, already-tested behavior.

**Acceptance Criteria**

- A spec states that a quality gate created through the API is always user-defined, never
  predefined, regardless of the request body.
- A spec states that criteria metadata (label, description) is owned by the
  `OpenApiCoverageCriteria` enum and never duplicated in the database, and explains why.
- A spec states the startup seeding order (criteria existence rows before predefined gates) and
  that both seeding steps are idempotent, safe to run on every startup.
- A spec states the full CRUD contract for quality-gate configurations: create (201, 409 on
  duplicate name), list (paginated, `X-Total-Count`), get by name (200, 404), update (200, 404,
  400 for a predefined target or an unknown criterion name), delete (204, 404, 400 for a
  predefined target).
- A spec states that update applies merge-patch semantics field-by-field, and that an explicit
  empty (non-null) criteria list in the request clears all existing criteria rather than being
  treated as "unset."
- A spec states the four predefined gates' exact names, criteria sets, and coverage thresholds as
  they ship today.
- A spec states the criteria catalog endpoint's contract: every `OpenApiCoverageCriteria` enum
  constant, sorted by name, enriched with its label and description.

**Out of scope**

- Quality-gate _evaluation_ (checking an analysis result against a gate) — that is
  `STR-004`/`SYS-006`/`SYS-009`'s concern, realized elsewhere; this module only stores and serves
  configuration, it never evaluates anything.
- Changing the merge-patch-clears-criteria behavior — it is documented as-is; whether it should
  instead ignore an empty list is a product decision left for its own story if raised.
- The generic `{code, message}` error envelope `ApiExceptionHandler` normalizes Spring MVC's own
  exceptions into — this convention is shared by every microservice's own exception handler, not
  specific to this module, and belongs in a cross-cutting spec if and when another module's
  reverse-engineering pass picks it up.

## Relations

**Realizes**

- [CON-005](../specs/CON-005-api-created-gates-are-never-predefined.md) — the
  never-predefined-via-the-API invariant
- [ARCH-002](../specs/ARCH-002-criteria-metadata-owned-by-enum.md) — criteria metadata
  ownership
- [ARCH-003](../specs/ARCH-003-idempotent-ordered-startup-seeding.md) — the seeding
  order and idempotency
- [SW-009](../specs/SW-009-quality-gate-crud-contract.md) — the CRUD lifecycle and its
  error cases
- [SW-010](../specs/SW-010-update-merge-patch-empty-criteria-clears.md) — merge-patch
  update semantics
- [SW-011](../specs/SW-011-four-predefined-gates-fixed-composition.md) — the four
  predefined gates' exact composition
- [SW-012](../specs/SW-012-criteria-catalog-endpoint.md) — the criteria catalog endpoint

**Related**

- [SYS-008](../specs/SYS-008-quality-gate-definitions.md) — the system-level capability this
  module implements
- [CON-003](../specs/CON-003-predefined-quality-gates-are-immutable.md) — the immutability
  invariant this module's update/delete guards enforce
- [SYS-006](../specs/SYS-006-criteria-based-evaluation.md) — the fixed criteria set this
  module's catalog endpoint exposes
- [STR-004](STR-004-quality-gate-evaluation.md) — the sibling story covering evaluation, which
  this module's configuration feeds but does not perform
