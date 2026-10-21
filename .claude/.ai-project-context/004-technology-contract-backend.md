# Technology Contract — Backend (Java)

The stack the Java side of Snow-White is built on: every `microservices/*` service,
`internal/commons`, `toolkit/spring-web-autoconfiguration`, `toolkit/openapi-generator`, and the
`examples/*` reference modules.
Covers the Maven reactor's Java modules; the React/TypeScript
webapp and the standalone CLI have their own contracts (`004-technology-contract-webapp.md`,
`004-technology-contract-cli.md`).

## 1. Runtime

- Java 25 — current LTS (released September 2025) — is the **bytecode target**.
  Pinned via `java.version` /
  `maven.compiler.release` in the root `pom.xml`, and the JDK every published artifact and
  release build is compiled with.
- Java 27 is the **JVM-image runtime**: the `eclipse-temurin:27-jdk-alpine` base image in every
  `microservices/*` and `examples/*` `Dockerfile`, and the `jlink` toolchain deriving the custom
  JREs for the three services `build-oci-images.sh` publishes (`api-gateway`, `api-sync-job`,
  `openapi-coverage-stream`).
  Release-25 bytecode on a 27 JVM is forward-compatible, so this
  stays additive — consumers of the published Maven artifacts still only need a JDK 25.
  Non-LTS caveat: Adoptium's End of Availability for 27 is March 2027, so this pin needs
  revisiting before then, either forward to 28 or back to the 25 LTS.
- The four services published as **GraalVM native images** (`api-index-api`,
  `otel-event-filter-stream`, `quality-gate-api`, `report-coordinator-api`) carry no JDK in the
  published artifact at all — a native image embeds SubstrateVM — so the 27 JVM pin describes only
  their `Dockerfile` fallback, which does use `eclipse-temurin:27-jdk-alpine` like every other
  service but is built by neither CI nor release.
  What the image build pins instead is the compiler: their `BP_JVM_VERSION` is
  `native.java.version`, selecting the buildpack's build-time NIK, and it is **25**.
  The bellsoft-liberica buildpack ships `jdk`/`jre` for 27.0.0 but `native-image-svm` only up to
  25.0.4, so 27 fails dependency resolution — the ceiling is the buildpack's, not a choice.
  The property is separate from `maven.compiler.release` so it can be lifted in one edit once a 27
  NIK is published, without moving the bytecode target.
  A future JVM-runtime bump therefore has to touch three things, not one: the `Dockerfile` bases,
  the `jlink` toolchain, and — only when a matching NIK exists — this property.
- Because of the bytecode/runtime split, no module may compile with `--enable-preview`.
  Preview class files are
  version-locked: javac rejects the flag unless `--release` matches the compiling JDK, and a
  27 JVM refuses to load classes marked preview-for-25.
  Nothing enforces this by a dedicated check; the 27 leg of `Build and Test` is the detector, and
  it fails at compile time — `--release 25` with `--enable-preview` is rejected outright by a
  javac 27.
  What that leaves open: the 25 leg and `deploy.yml` — which publishes on 25 — both stay green, so
  a flag added without the 27 leg running goes unnoticed; and the detector only sees what `-Pprod`
  actually compiles, so a preview flag confined to the `native` or `apptest` profiles slips past
  every leg.
  (A `--enable-preview` on a `Dockerfile` `ENTRYPOINT` is harmless on its own — the launcher
  accepts the flag unconditionally; it is the class files that are version-locked.)
- CI covers both JDKs, not both sides of the split.
  `Build and Test` is a two-leg matrix: each
  leg builds the reactor and runs the unit and integration suites, one on a JDK 25 JVM and one on 27.
  Sonar analysis and all publishing stay on 25.
  Note that each leg pairs javac N with JVM N,
  so the combination that actually ships — a javac-25 jar on the temurin-27 runtime — is exercised
  by no job; `build.yml` says so at the `app-tests` JDK step.
  The legs report as the status checks
  `Build and Test (25)` and `Build and Test (27)`.
  The matrix **renamed** the context, and nothing in the repo can apply that rename — the
  default-branch ruleset lives in GitHub settings and has to be edited by hand.
  Until it is, a still-required plain `Build and Test` never reports and blocks every PR, while the
  two real legs are advisory.
  Same for a third JDK: add its context there, or the new leg does not gate.
- Spring Boot 4.1.1, via `spring-boot-dependencies` BOM import (`spring-boot.version` in the root
  `pom.xml`).
- Maven multi-module reactor (`./mvnw -b smart package`, `-pl :<artifactId> -am` for scoped
  builds).

## 2. Approved libraries

- Messaging: Apache Kafka (Spring Kafka) for async inter-service work.
- Observability: OpenTelemetry (spans enriched via `toolkit/spring-web-autoconfiguration`'s
  `@SnowWhiteInformation`), InfluxDB/Tempo as telemetry stores read by `openapi-coverage-stream`.
- Persistence: PostgreSQL where a service is stateful.
- Testing: JUnit 5 (Jupiter), Mockito, ArchUnit (`layeredArchitecture()`), Testcontainers,
  WireMock, Citrus (black-box `*AppTest` suites).
  See `005-testing-contract.md` for how these are
  used.
- Nullability: `org.jspecify.annotations` (`@NonNull` / `@Nullable`, `@NullMarked`).
- Lombok, for boilerplate reduction (`@RequiredArgsConstructor`, `@Slf4j`, `@Builder`).
- Code style: Prettier (Java sources use CRLF line endings; Prettier enforces it).
- Static imports are preferred over qualified calls throughout, main code and tests alike
  (`mock(...)` not `Mockito.mock(...)`, `isEmpty(...)` not `CollectionUtils.isEmpty(...)`).
- Prefer `java.util.Objects.isNull(x)` / `nonNull(x)` (statically imported) over `x == null` /
  `x != null`.

## 3. Requires justification

- Introducing a new dependency, framework, or build tool beyond the above.
- A version bump that changes behavior or compatibility (e.g. a Spring Boot minor/major, a Java
  LTS jump) — routine patch bumps within an already-approved minor line do not.

## 4. Out of scope

The architecture — module boundaries, dependency direction, Kafka topic design — is stated in
`docs/spec/architecture.md`.
Static analysis (SonarCloud), coverage thresholds, and further
libraries are the project's to add as its specs require.
This contract stays to the stack.
