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
  `otel-event-filter-stream`, `quality-gate-api`, `report-coordinator-api`) are unaffected: their
  `BP_JVM_VERSION` is `maven.compiler.release`, so the buildpack still compiles them with 25.
  Their `Dockerfile`s are a local JVM-image fallback, built by neither CI nor release.
- Because of that split, no module may compile with `--enable-preview`.
  Preview class files are
  version-locked: javac rejects the flag unless `--release` matches the compiling JDK, and a
  27 JVM refuses to load classes marked preview-for-25.
  This one is mechanically enforced — the
  `Reject Preview Features` step in the `Build and Test` job greps every `pom.xml` and `Dockerfile`
  for the flag — because the failure it prevents surfaces only when a published image starts, long
  after a green build.
- CI exercises both halves.
  The `Build and Test` job builds and runs the suites on JDK 25;
  the `Build and Test (JDK 27)` job repeats them on 27.
  Sonar analysis and all publishing stay on 25.
  Only `Build and Test` is in the default-branch ruleset's required status checks, so the 27 job
  is advisory until that ruleset gains it — a 27-only failure does not block a merge today.
- Spring Boot 4.1.0, via `spring-boot-dependencies` BOM import.
  Latest patch on this minor line
  at contract time is 4.1.1 — not a support gap, just worth picking up on a routine dependency
  bump.
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
