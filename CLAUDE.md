# Snow-White

Snow-White correlates OpenAPI specifications with OpenTelemetry runtime traces to answer
"which parts of your API are actually being tested?" — as coverage, and quality-gate results.
It's an event-driven microservices system (Java/Spring Boot backend, Kafka for async work,
React/TypeScript frontend), built as a Maven multi-module reactor.

Full docs: `docs/_pages/*.md` (architecture, onboarding, requirements, workflows, CLI, deployment).
Human setup guide: `DEVELOPMENT.md`.
This file is for Claude Code sessions working in this repo.

## Skills

Four project-specific skills live in `.claude/skills/` in addition to the built-in ones.
Prefer
invoking these over improvising when the task matches:

| Skill          | Use for                                                                                                                                                                                  |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `snow-white`   | Reading a Snow-White JUnit XML quality-gate report and turning failures into concrete fixes.                                                                                             |
| `requirements` | **Before implementing any non-trivial new feature/behavior.** Drills to the root, non-technical need and drafts an `RQ-N` entry in `docs/_pages/requirements.md` before code is written. |
| `architect`    | Structural/cross-cutting decisions — where new functionality belongs, new services, layered-architecture questions, Kafka topic design.                                                  |
| `apptest`      | Writing or extending Citrus black-box tests under `src/apptest` for a microservice.                                                                                                      |
| `ui-expert`    | Reviewing or building the `api-gateway` React/TypeScript frontend — UX, accessibility, visual verification via `claude-in-chrome`.                                                       |

If a request looks like "add a new capability" rather than "fix/refactor/clean up", start with
`requirements`, not code.

## Module map

Maven reactor root modules: `examples/`, `microservices/`, `toolkit/`, `internal/`, `helm/`.
Key
artifacts (Maven `-pl :<artifactId>`):

| Module                                 | Responsibility                                                                                                                        |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `api-gateway`                          | Routes incoming HTTP to internal services; hosts the React web UI.                                                                    |
| `api-index-api`                        | Manages indexed API specifications (lookup by service/api name/version).                                                              |
| `api-sync-job`                         | Periodically syncs API specs from external sources (e.g. Artifactory).                                                                |
| `otel-event-filter-stream`             | Optional Kafka Streams filter — drops telemetry not relevant to Snow-White before it reaches coverage analysis.                       |
| `openapi-coverage-stream`              | Analyzes coverage of real API usage (InfluxDB/Tempo telemetry) against OpenAPI specs.                                                 |
| `quality-gate-api`                     | Quality-gate config and criteria management.                                                                                          |
| `report-coordinator-api`               | Coordinates calculation requests/results across services; owns the report domain.                                                     |
| `toolkit/cli`                          | Node/TypeScript CLI for triggering syncs, calculations, and gate checks (own test runner: `bun`, own build: `frontend-maven-plugin`). |
| `toolkit/spring-web-autoconfiguration` | Auto-enriches Spring Boot spans with API metadata (`@SnowWhiteInformation`).                                                          |
| `toolkit/openapi-generator`            | OpenAPI Generator plugin that emits code carrying the right Snow-White annotations.                                                   |
| `internal/commons`                     | Shared DTOs/events/utilities used across microservices.                                                                               |

Coverage calculation is **deliberately asynchronous**: Report Coordinator publishes to
`snow-white-calculation-request`, `openapi-coverage-stream` computes and publishes to
`snow-white-openapi-calculation-response`.
UI-facing calls through the gateway stay synchronous.
See `docs/_pages/architecture.md` for the full diagram and reasoning.

## Build & test

```shell
./mvnw -b smart package              # build everything
./mvnw -pl :<artifactId> -am test    # test one module (+ its dependencies)
./mvnw verify -T 1C                  # full unit/integration test + coverage aggregation
./mvnw -pl :<artifactId> -am -P apptest verify   # black-box Citrus tests for one service (needs Docker)
./mvnw -pl :api-gateway -am -P e2e test          # black-box Playwright UI tests (no Docker needed)
```

Frontend-only (from `microservices/api-gateway`): `npx jest --config jest.conf.cjs`,
`npx eslint <files>`, `npx webpack --config webpack/webpack.dev.cjs` to compile-check the webapp,
`pnpm run e2e` for the Playwright suite directly (see `src/apptest/e2e`; run `pnpm exec playwright
install chromium` once beforehand). That suite serves the built React app for real and mocks every
backend call at the network layer (`page.route`) — it does **not** use `webpack-dev-server`/`npm
run webapp:dev` (see the comment atop `webpack/e2e-server.cjs` for why: a worker pool and a
browser-auto-open path both throw when spawned non-interactively, e.g. by Playwright or CI).

This checkout's Java sources use **CRLF** line endings and Prettier enforces it — prefer the
`Edit`/`Write` tools over scripted/piped rewrites (e.g. Python `open(..., 'w')`) for `.ts`/`.tsx`
files, which have corrupted line endings into stray `\r` in this session before.
If you do use a
script, verify with `npx eslint --fix <file>` afterward.

Offline builds (`-o`) work for the default profile since dependencies are cached, but `-Pprod`
pulls in additional plugin transitives (e.g. `maven-jar-plugin`'s archiver deps) that may not be
cached — drop `-o` if a `-Pprod` build fails with "Cannot access central... offline mode".

## Resource usage

Keep compute/resource usage low wherever there's a choice with no real downside — green computing
matters here.
Concretely: in CI, start only the containers/services a given job or matrix entry
actually needs (e.g. each microservice's own `src/apptest/resources/docker-compose-apptest.yaml`
rather than one shared set of services for every matrix entry, and skip dev-convenience-only
containers like `kafka.ui` that tests never touch); prefer scoped/targeted builds and test runs
(`-pl :<artifactId> -am`) over full-reactor ones when only one module changed; don't leave
long-running processes (dev servers, `docker compose up`) running longer than needed to verify
something.
This is a default to apply opportunistically, not a constraint to chase at the expense
of correctness or clarity — never skip a health check, a needed dependency, or test coverage to
save compute.

## Coding conventions

- [Conventional Commits](https://www.conventionalcommits.org) (`feat:`, `fix:`, `refactor:`,
  `chore:`, with scopes like `refactor(deps):`).
- Java is Prettier-formatted (`// prettier-ignore` used sparingly for hand-aligned ArchUnit
  rules); static imports are generally preferred over qualified calls in tests
  (`mock(...)`/`when(...)` not `Mockito.mock(...)`).
- Lombok is used throughout (`@RequiredArgsConstructor`, `@Slf4j`, `@Builder`) — most
  constructors are Lombok-generated pure field assignment; check before assuming.
- Nullability: `org.jspecify.annotations.@NonNull`/`@Nullable`, sometimes with `@NullMarked` at
  class level.
  SonarCloud's `java:S2638` false-positives on overrides of Spring framework
  methods that are themselves `@Nullable`-annotated at the package level (e.g.
  `ResponseEntityExceptionHandler.handleExceptionInternal`) — this is a confirmed upstream Sonar
  bug ([SONARJAVA-5865](https://community.sonarsource.com/t/unresolvable-fp-java-s2638/151934)),
  not a real defect; don't "fix" it by changing working code, flag it for a false-positive
  resolution in SonarCloud instead.
- Don't guess at Sonar/lint rule fixes — verify against the actual rule semantics (`WebFetch`
  `https://sonarcloud.io/api/rules/show?key=...` or bytecode/type inspection for annotation
  mismatches) before changing behavior to silence a warning.
  Some warnings are legitimate false
  positives (see `S2638` above) or not worth the risk (e.g. migrating off a deprecated-but-still-
  working API whose replacement has a known upstream bug) — flag those instead of forcing a fix.
- i18n: frontend strings go through `react-jhipster`'s `Translate`/`translate`, with matching
  keys in both `i18n/en/*.json` and `i18n/de/*.json` — never hardcode user-facing text.

## Governance contract

`.claude/.ai-project-context/` holds the binding rules an agent session operates under —
loaded in numeric order per `000-agent-instructions.md`.
Testing rules (naming, ArchUnit layering,
`@InjectMocks` ordering hazard) live in `005-testing-contract.md`; stack/dependency rules live in
the per-module `004-technology-contract-*.md` files.
This file is the day-to-day quick reference;
the governance contract is authoritative on conflict.

## Requirements

`docs/_pages/requirements.md` intentionally describes **observable outcomes**, not
implementation — numbered `RQ-N[.M]` / `NFR-N` statements using SHALL, black-box testable, no
internal design details.
See the `requirements` skill for the process of extending it.
