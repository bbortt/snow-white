---
title: 'Onboarding'
permalink: /onboarding/
toc: true
toc_sticky: true
---

This guide walks you through integrating your service with Snow-White so it can measure OpenAPI coverage from your test suite.

**Prerequisite:** A Snow-White instance must already be running and reachable.
If you need to set one up first, see [Deployment](/deployment/).
{: .notice--info}

## How It Works

Snow-White correlates two things:

1. **Your OpenAPI specification** — describes what endpoints exist.
2. **OpenTelemetry traces** — records which endpoints were actually called.

The link between them is three identifiers that must appear in **both** places:

| Identifier   | In the OpenAPI spec   | In the OTEL span         |
| ------------ | --------------------- | ------------------------ |
| Service name | `info.x-service-name` | `service.name` attribute |
| API name     | `info.x-api-name`     | `api.name` attribute     |
| API version  | `info.version`        | `api.version` attribute  |

---

## Step 1 — Annotate Your OpenAPI Specification

Add `x-api-name` and `x-service-name` to the `info` block:

```yaml
openapi: 3.1.0
info:
  title: My Service API
  version: 1.0.0
  x-api-name: my-api
  x-service-name: my-service
```

Both values must be stable, lowercase, hyphen-separated identifiers.

See [`example-application/specs/ping-pong.yml`](https://github.com/bbortt/snow-white/blob/main/example-application/specs/ping-pong.yml) for a complete example.

---

## Step 2 — Instrument Your Application

### Option A: Spring Boot (recommended)

**1.
Add the `spring-web-autoconfiguration` dependency**

```xml
<dependency>
  <groupId>io.github.bbortt.snow-white.toolkit</groupId>
  <artifactId>spring-web-autoconfiguration</artifactId>
  <version>${snow-white.version}</version>
</dependency>
```

Automatically enriches every HTTP span with `api.name` and `api.version`.
`service.name` is picked up from `OTEL_SERVICE_NAME` (or `spring.application.name` as fallback).

**2.
Use the `snow-white-spring-server` code generator (optional but recommended)**

```xml
<plugin>
  <groupId>org.openapitools</groupId>
  <artifactId>openapi-generator-maven-plugin</artifactId>
  <dependencies>
    <dependency>
      <groupId>io.github.bbortt.snow-white.toolkit</groupId>
      <artifactId>openapi-generator</artifactId>
      <version>${snow-white.version}</version>
    </dependency>
  </dependencies>
  <executions>
    <execution>
      <goals>
        <goal>generate</goal>
      </goals>
      <configuration>
        <inputSpec>specs/my-api.yml</inputSpec>
        <generatorName>snow-white-spring-server</generatorName>
        <apiPackage>com.example.api</apiPackage>
        <modelPackage>com.example.model</modelPackage>
        <configOptions>
          <apiNameAttributeKey>info.x-api-name</apiNameAttributeKey>
          <interfaceOnly>true</interfaceOnly>
          <useSpringBoot3>true</useSpringBoot3>
        </configOptions>
      </configuration>
    </execution>
  </executions>
</plugin>
```

See [`example-application/pom.xml`](https://github.com/bbortt/snow-white/blob/main/example-application/pom.xml) for a working reference.

**3.
Configure the OTEL Java agent**

```shell
java \
  -javaagent:/path/to/opentelemetry-javaagent.jar \
  -jar app.jar
```

```shell
OTEL_SERVICE_NAME=my-service
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_EXPORTER_OTLP_PROTOCOL=grpc
```

### Option B: Manual OTEL Enrichment

Attach these three attributes to your HTTP spans manually:

```properties
service.name  = <your service name>
api.name      = <value of x-api-name in your spec>
api.version   = <value of info.version in your spec>
```

Refer to the [Snow-White semantic convention](https://github.com/bbortt/snow-white/blob/main/semantic-convention/openapi.md) for the full attribute specification.

---

## Step 3 — Publish Your Specification

### Manual (quick start)

```shell
curl -X POST http://<snow-white-host>/api/v1/apis \
  -H 'Content-Type: application/json' \
  -d '{
    "serviceName": "my-service",
    "apiName": "my-api",
    "apiVersion": "1.0.0",
    "specUrl": "https://your-spec-host/my-api.yml"
  }'
```

### Automatic (production)

Enable the bundled synchronization job in your Helm values:

```yaml
snowWhite:
  apiSyncJob:
    enabled: true
    artifactory:
      baseUrl: 'http://artifactory:8082/artifactory'
      repository: 'api-specs-local'
```

See [Deployment — API Indexation](/deployment/#api-indexation) for full options.

---

## Step 4 — Generate Traces

Run your test suite with the OTEL agent attached.
Each HTTP call produces a span that Snow-White can process.

Quick smoke test:

```shell
curl http://localhost:8080/your-endpoint
```

---

## Step 5 — Calculate Coverage

**Install the CLI** — pre-built binaries are available on [GitHub Releases](https://github.com/bbortt/snow-white/releases); there is no need to build from source.
See [CLI Reference — Installation](/cli/#installation) for download instructions and the OCI image option.
{: .notice--info}

Create a CLI config file:

```json
{
  "url": "http://<snow-white-host>",
  "qualityGate": "basic-coverage",
  "apiInformation": [
    {
      "serviceName": "my-service",
      "apiName": "my-api",
      "apiVersion": "1.0.0"
    }
  ]
}
```

Run the CLI:

```shell
snow-white calculate --config-file snow-white.json
```

The CLI exits `0` on pass, non-zero on failure — suitable for CI pipelines.
See [CLI Reference](/cli/) for all commands and options.

---

## Step 6 — Review Results

The Snow-White UI shows:

- **Coverage** — which endpoints were hit and which were not.
- **Quality Gate status** — pass/fail against configured thresholds.

Snow-White ships with a `basic-coverage` gate out of the box.
Custom gates can be configured via the UI or API.

---

## Checklist

- [ ] `x-api-name` and `x-service-name` added to the spec `info` block
- [ ] `OTEL_SERVICE_NAME` matches `x-service-name`
- [ ] OTEL Java agent attached (or manual span enrichment in place)
- [ ] `spring-web-autoconfiguration` dependency added (Spring Boot only)
- [ ] Specification published to the API index
- [ ] CLI config file created and coverage calculation runs successfully

---

## Example Application

The [`example-application`](https://github.com/bbortt/snow-white/tree/main/example-application) demonstrates a complete integration:

- A minimal Spring Boot service (`GET /ping`, `POST /pong`)
- An annotated OpenAPI spec
- Maven build with the code generator and OTEL agent
- A `dev/` Docker Compose environment with the full stack

**Prerequisites:** Java 25, Node.js 22, Docker or Podman (with Compose).

```shell
# 1. Build
./mvnw -Pnode package

# 2. Start the stack
docker compose -f dev/docker-compose.yaml up -d

# 3. Set InfluxDB token in dev/.env, then restart

# 4. Generate a trace
curl -ijv http://localhost:8080/ping?message=hello

# 5. Calculate coverage
node toolkit/cli/target/cli/index.js calculate --configFile dev/snow-white.json
```

See [`DEVELOPMENT.md`](https://github.com/bbortt/snow-white/blob/main/DEVELOPMENT.md) for the full development environment description.
