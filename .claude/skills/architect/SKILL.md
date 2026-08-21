---
name: architect
description: Use for structural or cross-cutting decisions in Snow-White — where new functionality belongs (new microservice vs. extend an existing one), sync vs. async communication design, new Kafka topics, changes that touch a module's layered architecture, or any "where should X live" / "should this be its own service" question. Trigger before making a structural choice, not for routine within-layer code changes.
---

# Snow-White Architect

Snow-White is an event-driven microservices system: each service has a single, well-defined
responsibility (see the module table in `CLAUDE.md` / `docs/_pages/architecture.md`).
Adding
capability almost always means extending an existing service along its existing responsibility,
not adding a new one — new services are for a genuinely new responsibility, not a convenient
place to put code.

## Sync vs. async — this project has an explicit rule, not a vibe

From `docs/_pages/architecture.md`, stated as a deliberate decision:

> Coverage calculation is deliberately asynchronous...
> Keeping that work off the synchronous
> request path prevents gateway timeouts, allows the stream processor to scale independently,
> and lets the coordinator handle many concurrent calculation requests without blocking.

Concretely:

- UI-facing requests through `api-gateway` → synchronous, request-in/response-out, routed
  directly to the owning service.
  Keep it that way unless the work is genuinely unbounded.
- Anything that can take a while, scale independently, or shouldn't block a caller (large
  telemetry lookback windows, bulk sync jobs, coverage computation) → publish to Kafka, let a
  stream/consumer service pick it up asynchronously, publish a result event.

When adding a new capability, ask which bucket it falls into _before_ choosing where the code
goes — a slow operation bolted onto a synchronous gateway path will eventually cause the same
timeout/blocking problems the existing async paths were built to avoid.

## Layered architecture within a service

Every microservice has a `TechnicalStructureUnitTest` at its root package (e.g.
`microservices/<service>/src/test/java/.../TechnicalStructureUnitTest.java`) that enforces a
layered architecture via ArchUnit's `layeredArchitecture().consideringAllDependencies()`.
The
exact layers differ by service shape:

- REST/persistence services (e.g. `quality-gate-api`, `report-coordinator-api`): typically
  `Config / Init / Web (..api.rest..) / Service / Persistence (..repository..) / Domain`, with
  access only allowed "downward" (Web → Service → Persistence → Domain), never sideways or up.
- Stream-only services (e.g. `otel-event-filter-stream`): a simpler `Config / Kafka / Service`.

**Before adding a class**, check the module's own `TechnicalStructureUnitTest` to see its actual
layer names and access rules — don't assume the REST-service shape applies everywhere.
If a
change seems to require a dependency the rule doesn't currently allow, that's a signal to
reconsider the design (wrong layer for the new code, missing abstraction) before reaching for an
`ignoreDependency` exception — those exist in a few places for genuine Spring-generated-code
noise (AOT `__BeanDefinitions`, CGLIB proxy subclasses), not as a general escape hatch.

## Reuse the toolkit before building new plumbing

- `toolkit/openapi-generator` — OpenAPI Generator plugin that emits code carrying the right
  Snow-White annotations automatically.
  Use it for any new OpenAPI-spec-derived client/model
  code rather than hand-writing DTOs.
- `toolkit/spring-web-autoconfiguration` — auto-enriches Spring Boot spans with API metadata via
  `@SnowWhiteInformation`.
  A new Spring Boot service that exposes annotated endpoints should
  depend on this rather than re-implementing span enrichment.
- `internal/commons` — shared DTOs/events used across services (e.g. Kafka event payloads).
  New
  cross-service message contracts belong here, not duplicated per-service.

## Tie structural decisions back to a requirement

A new microservice, a new Kafka topic, or a new cross-cutting capability is exactly the kind of
"bigger change" that should trace back to an entry in `docs/_pages/requirements.md` (see the
`requirements` skill).
If the structural question you're being asked to resolve doesn't map to
an existing `RQ-N`, raise that before committing to a design — the requirement should usually be
settled first, since it can change which architecture is actually correct.

## Case study: a real pre-existing gap found in this codebase

`quality-gate-api`'s `TechnicalStructureUnitTest` currently fails its own layering check
(`QualityGateService`'s Spring AOT `__BeanDefinitions` and CGLIB proxy subclass both reference a
Web-layer mapper type through generated code, in a direction the existing `ignoreDependency`
rules don't cover).
This is real, pre-existing, and unrelated to any specific feature work — a
good example of the kind of structural finding this skill exists to catch, and a candidate fix
for whoever picks it up next: broaden the `__BeanDefinitions`/CGLIB ignore rules to be symmetric
(currently only one direction per generated-code category is excluded).
