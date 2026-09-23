# Snow-White

Snow-White correlates OpenAPI specifications with OpenTelemetry runtime traces to answer
"which parts of your API are actually being tested?" — as coverage, and quality-gate results.
It's an event-driven microservices system (Java/Spring Boot backend, Kafka for async work,
React/TypeScript frontend), built as a Maven multi-module reactor.

Full docs: `pages/_pages/*.md` (architecture, onboarding, requirements, workflows, CLI, deployment).
Human setup guide: `DEVELOPMENT.md`.
This file is for Claude Code sessions working in this repo.

## Skills

Six project-specific skills live in `.claude/skills/` in addition to the built-in ones.
Prefer
invoking these over improvising when the task matches:

| Skill           | Use for                                                                                                                                            |
| --------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| `premise-check` | Verifying the factual claims a request rests on, before drafting, speccing, or coding — runs first whenever a request asserts a fact.              |
| `snow-white`    | Reading a Snow-White JUnit XML quality-gate report and turning failures into concrete fixes.                                                       |
| `requirements`  | Drills to the root, non-technical need behind a feature request — invoked through `clew-draft` for in-scope Java source (see below), not directly. |
| `architect`     | Structural/cross-cutting decisions — where new functionality belongs, new services, layered-architecture questions, Kafka topic design.            |
| `apptest`       | Writing or extending Citrus black-box tests under `src/apptest` for a microservice.                                                                |
| `ui-expert`     | Reviewing or building the `api-gateway` React/TypeScript frontend — UX, accessibility, visual verification via `claude-in-chrome`.                 |

`premise-check` runs before all the others, on any request — feature, fix, or refactor — that
asserts a fact you would build on: what Tempo or Kafka can do, what a limit is, why something is
slow, what the calculators read.
The rest of this file's workflow takes the request's facts as given; that skill is the only step
that checks them.
It is cheap by design — most requests have nothing to verify, and saying so is a valid outcome.

For a request outside clew's scope (the `api-gateway` webapp, `toolkit/cli`) that looks like "add
a new capability" rather than "fix/refactor/clean up", start with `requirements`, not code.
For anything inside clew's scope, see "Spec-driven development" below instead.

## Spec-driven development (clew)

[clew](https://www.npmjs.com/package/clew) governs Java source in this repo: `internal/commons/`,
every `microservices/*` module, and `toolkit/*` except `toolkit/cli`.
(`toolkit/cli` and the `api-gateway` webapp are outside clew's scope today — its only configured
generator targets Java, so it cannot anchor TypeScript.)
Config: `.clewrc.json`.
Corpus: `docs/spec/` (stories in `docs/spec/stories`, specs in `docs/spec/specs`, drafts in
`docs/spec/drafts`).

Any feature request or observable-behavior change to in-scope source must go through clew before
code is written — do not implement it directly, even a small one.
The flow, in order:

1. `clew-draft` drafts the story and its specs, delegating the actual authoring to the
   `requirements` skill.
2. `clew-context` grounds the draft against the existing corpus.
3. The user reviews and approves the draft.
4. `clew-promote` binds real ids and moves the draft into the spec tree.
5. `clew-implement` sets the spec active, delegates the actual coding to the project's own skills
   (`architect`, `apptest`, `ui-expert`, etc.), then anchors the result via `clew-anchor` and
   verifies coverage.

A pure fix, refactor, or dependency bump that changes no observable behavior needs no new spec.
If it touches code already anchored to a spec, read that spec first (`clew-context`) and work
from its intent — the anchor is a claim someone made, not proof the code is correct.

`pages/_pages/requirements.md` predates clew and is not where new requirements go.
Treat it, and the running code, as source material for reverse-engineering the existing system
into `docs/spec/` — an ongoing effort tracked outside this file.

`snow-white` is also published to consumers as an APM package, so its source of truth is
`.apm/skills/snow-white/` — `.claude/skills/snow-white/` is a generated copy.
Edit the `.apm/` copy, then run `pnpm run skill:sync`; CI fails if the two drift apart.

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
See `pages/_pages/architecture.md` for the full diagram and reasoning.

## Build & test

Build inside the development container (`.devcontainer/`) whenever one can be started —
`npx @devcontainers/cli up --workspace-folder .`, then `exec` into it.
A build here runs a lot of
unreviewed third-party code (Maven plugins, pnpm and Bun lifecycle scripts, Testcontainers images)
with whatever the session's user can reach, which is a worse deal for an agent than for a human.
Where no container runtime is available, build on the host and say so in the summary rather than
leaving it implied.
Setup and platform caveats: `DEVELOPMENT.md#development-container`.

```shell
./mvnw -b smart package              # build everything
./mvnw -pl :<artifactId> -am test    # test one module (+ its dependencies)
./mvnw verify -T 1C                  # full unit/integration test + coverage aggregation
./mvnw -pl :<artifactId> -am -P apptest verify   # black-box Citrus tests for one service (needs Docker)
./mvnw -pl :api-gateway -am -P e2e test          # black-box Playwright UI tests (no Docker needed)
.github/scripts/pitest-changed-classes.sh        # PIT mutation testing, changed classes only
```

Mutation testing is a CI gate (`Mutation Testing (PIT)`, 80% mutation score _and_ 80% test
strength per module) — don't push Java changes and let CI tell you about it twenty minutes later.
That job is skipped entirely when a branch changes no `*.java` file against its branch point, so a
docs, Helm or TypeScript change never waits for it.
Two things still pull it in without Java: a branch or pull-request name containing `pitest` (a
bump of the engine moves the verdict on its own), and the `include:mutation` label, which forces
the run the way `include:apptests` forces the application tests.
The label needs no push behind it — its own workflow (`.github/workflows/mutation-testing.yml`,
hence a `Mutation Testing` run rather than an `Application CI` one) also triggers on a labelled
pull request, so sticking it on an open one starts mutation testing right there.
Run `.github/scripts/pitest-changed-classes.sh [base-ref]` locally instead (base defaults to
`main`).
It diffs the branch against the base — uncommitted and untracked files included — and
re-invokes the `mutation` profile once per affected module with `-DtargetClasses` set to just
those classes, which is the difference between minutes and mutating the whole reactor.
Survivors
are listed in each module's `target/pit-reports/index.html`.

Three things to know about a scoped run: the 80/80 thresholds then apply to the changed classes
alone, so a failure there is sharper than the module-wide CI gate and doesn't always mean CI would
go red; a changed test file only scopes in its subject when the naming convention derives it
(`FooUnitTest`/`FooTest` → `Foo`), so a changed fixture or helper needs a manual `-DtargetClasses`
run; and the base ref is resolved locally — pass `origin/main` when the local `main` is behind, or
every class merged since scopes in as "changed".

Frontend-only (from `microservices/api-gateway`): `npx jest --config jest.conf.cjs`,
`npx eslint <files>`, `npx webpack --config webpack/webpack.dev.cjs` to compile-check the webapp,
`pnpm run e2e` for the Playwright suite directly (see `src/apptest/e2e`; run `pnpm exec playwright
install chromium` once beforehand). That suite serves the built React app for real and mocks every
backend call at the network layer (`page.route`) — it does **not** use `webpack-dev-server`/`npm
run webapp:dev` (see the comment atop `webpack/e2e-server.cjs` for why: a worker pool and a
browser-auto-open path both throw when spawned non-interactively, e.g. by Playwright or CI).

Every source file in this checkout uses **LF** line endings — Prettier enforces it
(`endOfLine: 'lf'`), and the only CRLF file is `mvnw.cmd`, pinned that way by `.gitattributes`.
Prefer the `Edit`/`Write` tools over scripted/piped rewrites (e.g. Python `open(..., 'w')`) for
`.ts`/`.tsx` files, which have corrupted line endings into stray `\r` in this session before.
If you do use a script, verify with `npx eslint --fix <file>` afterward.

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
  rules); static imports are preferred over qualified calls throughout — main code and tests
  alike (`mock(...)`/`when(...)` not `Mockito.mock(...)`; `isEmpty(...)` not
  `CollectionUtils.isEmpty(...)`).
- Prefer `java.util.Objects.isNull(x)`/`nonNull(x)` (statically imported) over `x == null`/
  `x != null` — matches existing usage throughout the backend.
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

`pages/_pages/requirements.md` intentionally describes **observable outcomes**, not
implementation — numbered `RQ-N[.M]` / `NFR-N` statements using SHALL, black-box testable, no
internal design details.
It predates clew and is being reverse-engineered into `docs/spec/` (see "Spec-driven
development" above); new requirements are drafted there, not added to this page.
