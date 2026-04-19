<h1 align="center">Snow-White 🍎</h1>

<p align="center">
    <img src=".github/logo.png" alt="Snow-White Logo" width="100px" />
</p>

<p align="center">
    <i>OpenAPI coverage and quality insights powered by OpenTelemetry.</i>
</p>

Snow-White connects your OpenAPI specifications with runtime telemetry data to answer a simple question: **which parts of your API are actually being tested?**

It correlates [OpenTelemetry (OTEL)](https://opentelemetry.io) traces emitted by your application with the endpoints declared in your API specifications - then validates coverage against configurable quality gates.

Snow-White works with **black-box test suites** (system tests, integration tests) as well as **live production traffic**.

It currently provides insights into:

- **Coverage** - which endpoints were exercised and which were not
- **API Performance** - response time analysis across operations

## Installation (for providers)

Snow-White can be installed easily using Helm.
Detailed instructions and available options are described in ["Deployment"](https://bbortt.github.io/snow-white/deployment).

Individual images could also be used for a custom installation.
The Docker compose file `dev/docker-compose.yaml` may be used as a starting point.

## Onboarding (for users)

See ["Onboarding"](https://bbortt.github.io/snow-white/onboarding) for a step-by-step guide to integrating your service with Snow-White.

## General Concept

Here's a high-level overview of Snow-White.
Detailed [architecture is depicted in `DEVELOPMENT.md`](./DEVELOPMENT.md#architecture-overview).

1. **Synchronize API Specs** - Snow-White imports OpenAPI specifications from your central interface repository.
2. **Ingest Telemetry** - It listens to OpenTelemetry tracing data emitted by your applications.
3. **Correlate & Analyze** - Traces are matched with API operations using service, API name, and version metadata.
4. **Evaluate Quality Gates** - Results are validated against your configured thresholds.
5. **Visualize or Export Results** - Coverage and performance insights can be viewed in reports or integrated into CI pipelines.

### Annotating APIs

The core idea behind Snow-White is that it can only interpret runtime telemetry (e.g. traces) if it knows which API the data belongs to.
This is achieved by a common annotation model applied both to the specifications and to the telemetry data.

Every API must declare a few key identifiers:

- **Service Name**: Identifies the logical unit or application that exposes the API.
  - _Required_, because different applications may have identically named endpoints (e.g. `/health`).
- **API Name**: Identifies the API within the service.
- **Version**: Specifies the API version.

With OpenAPI, this is expressed using vendor extensions:

```yaml
openapi: 3.1.0
info:
  title: Ping-Pong API
  description: A simple API for ping-pong interactions to demonstrate OpenAPI coverage calculation
  version: 1.0.0
  x-api-name: ping-pong
  x-service-name: example-application
```

These annotations are the **linking key** between specifications and runtime telemetry.

### Making Specifications Available

Snow-White needs access to the API specifications.
This is typically done by publishing them to a central HTTP-based service interface repository, where Snow-White can fetch and synchronize them.

👉 For details, see the [`api-sync-job` documentation](./microservices/api-sync-job) (handles synchronization and version management).

### Connecting Runtime Data

Next, the running service must provide OpenTelemetry (OTEL) tracing data.

At the moment, only tracing is relevant (metrics and logs are not used yet).
The traces must be enhanced with the same annotation information used in the specifications:

- Service Name: [`service.name` attribute](https://opentelemetry.io/docs/specs/semconv/registry/attributes/service)
- API Name: [`api.name` attribute](./semantic-convention/openapi.md)
- API Version: [`api.version` attribute](./semantic-convention/openapi.md)

This enrichment ensures that Snow-White can correlate a runtime span to the correct API specification.

To simplify this, the project provides:

- [**OpenAPI Generator Plugin**](./toolkit/openapi-generator) - Generates code that automatically attaches the right annotations.
- [**Spring Web Autoconfiguration**](./toolkit/spring-web-autoconfiguration) - Automatically enriches spans with API metadata in Spring-based services.

### Coverage Calculation

Once both inputs are in place (specifications + traces), you can trigger a coverage calculation.
This can be done via the [CLI](./toolkit/cli).

The CLI allows you to synchronize APIs, trigger coverage analysis, and validate results against quality gates directly from your local environment or CI pipeline.

Coverage results are validated against Quality-Gate Configurations.
These configurations define the rules and thresholds that your API data must satisfy.
Snow-White comes with predefined quality gates to get started quickly, and you can customize them later.

## Development & Contributing

We welcome contributions!
Whether you're fixing a bug, adding a feature, or improving the documentation - thank you 🙌

To get started:

1. Read our [`DEVELOPMENT.md`](./DEVELOPMENT.md) for a step-by-step guide on setting up your local environment.
2. Open a Pull Request or file an issue if you’ve found something worth improving.
3. Make sure to follow the coding style and commit conventions described in [`DEVELOPMENT.md`](./DEVELOPMENT.md).

### Local Setup (Quick Start)

**Prerequisites:** Java 25, Node.js 22, Docker or Podman (with Compose).

```shell
git clone https://github.com/bbortt/snow-white.git
cd snow-white

# Build all modules (including the CLI and generated sources)
./mvnw -Pnode package

# Start the development environment
docker compose -f dev/docker-compose.yaml up -d
```

> A manual InfluxDB token setup step is required before traces can flow.
> See [`DEVELOPMENT.md`](./DEVELOPMENT.md) for the full walkthrough.

### Contributing Guidelines

- Keep commits small and focused.
  Follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`, etc.).
- All new features should include tests.
- Run the full build before opening a PR: `./mvnw -Pnode verify`
- Open an issue first for larger changes so we can align on direction before you invest time coding.

Please be respectful and constructive in all interactions.

## License

This project is licensed under the [Polyform Small Business License 1.0.0](https://polyformproject.org/licenses/small-business/1.0.0).

This means you can freely use, modify, and distribute the software **if your company generates less than $1 million USD in annual revenue** and meets other conditions in the license.

If your company exceeds this threshold or you intend to use Snow-White commercially, please reach out for licensing options: [timon.borter@gmx.ch].

See the [LICENSE](./LICENSE) file for full details.
