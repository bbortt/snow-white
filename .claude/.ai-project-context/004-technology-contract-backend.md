# Technology Contract — Backend (Java)

The stack the Java side of Snow-White is built on: every `microservices/*` service,
`internal/commons`, `toolkit/spring-web-autoconfiguration`, `toolkit/openapi-generator`, and the
`examples/*` reference modules.
Covers the Maven reactor's Java modules; the React/TypeScript
webapp and the standalone CLI have their own contracts (`004-technology-contract-webapp.md`,
`004-technology-contract-cli.md`).

## 1. Runtime

- Java 25 — current LTS (released September 2025).
  Pinned via `java.version` /
  `maven.compiler.release` in the root `pom.xml`.
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
