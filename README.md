<h1 align="center">Snow-White 🍎</h1>

<p align="center">
    <img src=".github/logo.png" alt="Snow-White Logo" width="100px" />
</p>

<p align="center">
    <i>An awesome pairing with - well, do you know Snow White and the Jaeger (Jaeger = hunter, and Jaeger also means Jaeger tracing 😉)?</i>
</p>

Snow-White connects API specifications with runtime telemetry data to provide insights into your API tests - such as coverage, performance, and usage behavior.

Snow-White makes such analysis effortless by leveraging API specifications and [OpenTelemetry (OTEL)](https://opentelemetry.io/) data.
OTEL is a standardized protocol that allows flexible data sourcing.

Most commonly, Snow-White listens to **black-box tests of your application**, such as system or integration tests.
However, it can also gather insights from a **live production environment**.

Snow-White provides valuable insights on:

- Coverage
- API Performance
- And more.

## General Concept

The core idea behind Snow-White is that it can only interpret runtime telemetry (e.g. traces) if it knows which API the data belongs to.
This is achieved by a common annotation model applied both to the specifications and to the telemetry data.

### Annotating APIs

Every API must declare a few key identifiers:

- **Service Name**: Identifies the logical unit or application that exposes the API.
  - Required, because different applications may have identically named endpoints (e.g. /health).
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

- Service Name: [`service.name` attribute](https://opentelemetry.io/docs/specs/semconv/registry/attributes/service/)
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

## How It Works

Here's a high-level overview of Snow-White.
Detailed [architecture is depicted in `DEVELOPMENT.md`](./DEVELOPMENT.md#architecture-overview).

1. **Synchronize API Specs** - Snow-White imports OpenAPI specifications from your central interface repository.
2. **Ingest Telemetry** - It listens to OpenTelemetry tracing data emitted by your applications.
3. **Correlate & Analyze** - Traces are matched with API operations using service, API name, and version metadata.
4. **Evaluate Quality Gates** - Results are validated against your configured thresholds.
5. **Visualize or Export Results** - Coverage and performance insights can be viewed in reports or integrated into CI pipelines.

## Development & Contributing

We welcome contributions!
Whether you're fixing a bug, adding a feature, or improving the documentation - thank you 🙌

To get started:

1. Read our [`DEVELOPMENT.md`](./DEVELOPMENT.md) for a step-by-step guide on setting up your local environment.
2. Open a Pull Request or file an issue if you’ve found something worth improving.
3. Make sure to follow the coding style and commit conventions described in [`DEVELOPMENT.md`](./DEVELOPMENT.md).

### Local Setup (Quick Start)

```shell
git clone https://github.com/bbortt/snow-white.git
cd snow-white

# Download dependencies and pack application
./mvnw -Pnode package
```

### Contributing Guidelines

<!-- TODO: Add these -->

Please review our contributing guide and code of conduct before submitting a PR.

## License

This project is licensed under the [Polyform Small Business License 1.0.0](https://polyformproject.org/licenses/small-business/1.0.0/).

This means you can freely use, modify, and distribute the software **if your company generates less than $1 million USD in annual revenue** and meets other conditions in the license.

If your company exceeds this threshold or you intend to use Snow-White commercially, please reach out for licensing options: [timon.borter@gmx.ch].

See the [LICENSE](./LICENSE) file for full details.
