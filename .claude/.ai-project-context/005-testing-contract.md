# Testing Contract

Mandatory testing rules for the Java backend (`microservices/*`, `internal/commons`,
`toolkit/spring-web-autoconfiguration`, `toolkit/openapi-generator`).
Frontend/CLI test tooling is
listed in their own technology contracts (`004-technology-contract-webapp.md`,
`004-technology-contract-cli.md`); this file's naming/design rules apply to Java only.

## 1. Test files & naming

- `*UnitTest.java` — Mockito (`@ExtendWith(MockitoExtension.class)`), no Spring context.
- `*IT.java` — integration tests: real Spring context / Testcontainers / WireMock.
- `*AppTest.java` (under `src/apptest`) — Citrus black-box tests against the fully packaged,
  running application, through real Kafka topics and HTTP endpoints — never through mocked
  internals.
  See the `apptest` skill.
- Every microservice has a `TechnicalStructureUnitTest` at its root package enforcing a layered
  architecture via ArchUnit (`layeredArchitecture()...consideringAllDependencies()`).
  Layers
  differ per service (e.g. `Config/Init/Web/Service/Persistence/Domain` for REST services,
  `Config/Kafka/Service` for stream-only ones) — check the module's own test before adding a
  class, to make sure its package/layer is allowed to depend on what it needs.

## 2. Running & coverage

- `./mvnw -pl :<artifactId> -am test` — unit tests for one module (+ its dependencies).
- `./mvnw verify -T 1C` — full unit/integration test run with coverage aggregation (JaCoCo).
- `./mvnw -pl :<artifactId> -am -P apptest verify` — black-box Citrus tests for one service (needs
  Docker).
  Each microservice starts only its own `src/apptest/resources/docker-compose-apptest.yaml`
  services, not a shared set — see the resource-usage guidance in `CLAUDE.md`.
- Coverage is aggregated via JaCoCo into `target/jacoco-aggregate/jacoco.xml`, read by SonarCloud.
- `.github/scripts/pitest-changed-classes.sh [base-ref]` — PIT mutation testing scoped to the
  classes changed against `base-ref` (default `main`), rather than the full-reactor run CI does.
  Run it locally before pushing Java changes; CI enforces 80% mutation score and 80% test strength
  per module, and waiting for that feedback costs ~20 minutes per attempt.
  See the "Build & test"
  section in `CLAUDE.md` for the scoping caveats.

## 3. Test design

- Tests shall be independent, deterministic, and not rely on execution order.
- Test names describe expected behavior, not internal method calls.
- Prefer testing observable behavior over implementation details; avoid unnecessary mocking.
- Prefer `@InjectMocks` over manually constructing the fixture in `@BeforeEach` — **except** when
  the constructor eagerly calls a method on one of the mocks to cache a field (e.g. reads a
  config value once at construction time). `@InjectMocks` always constructs before the test
  class's own `@BeforeEach` body runs, so a stub set up there would not yet be in place — that
  ordering hazard silently breaks the test (it caches an unstubbed mock default).
  Keep manual
  construction in that case, with a comment explaining why.
- Static imports are preferred over qualified calls (`mock(...)`/`when(...)`, not
  `Mockito.mock(...)`).
