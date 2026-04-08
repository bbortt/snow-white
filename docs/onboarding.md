---
title: Onboarding
nav_order: 3
description: 'Connect your service to Snow-White and start measuring OpenAPI coverage from your test suite'
---

# Onboarding

{: .no_toc }

This guide walks you through integrating your service with Snow-White so that it can measure OpenAPI coverage from your test suite.

{: .note }

> **Prerequisite:** A Snow-White instance must already be running and reachable.
> If you need to set one up first, see [Installation](installation).

## Table of contents

{: .no_toc .text-delta }

1. TOC
   {:toc}

---

## How It Works

Snow-White correlates two things:

1. **Your OpenAPI specification** - describes what endpoints exist.
2. **OpenTelemetry (OTEL) traces** - records which endpoints were actually called at runtime.

The link between them is a set of identifiers that must appear in **both** places:

| Identifier   | In the OpenAPI spec   | In the OTEL span         |
| ------------ | --------------------- | ------------------------ |
| Service name | `info.x-service-name` | `service.name` attribute |
| API name     | `info.x-api-name`     | `api.name` attribute     |
| API version  | `info.version`        | `api.version` attribute  |

The steps below show you how to set these up.

---

## Step 1 - Annotate your OpenAPI specification

Add `x-api-name` and `x-service-name` to the `info` block of your specification:

```yaml
openapi: 3.1.0
info:
  title: My Service API
  version: 1.0.0
  x-api-name: my-api # unique name for this API within the service
  x-service-name: my-service # matches the OTEL service.name of your application
```

Both values must be stable identifiers (lowercase, hyphen-separated is recommended) - they are used as lookup keys by Snow-White.

See [`example-application/specs/ping-pong.yml`](https://github.com/bbortt/snow-white/blob/main/example-application/specs/ping-pong.yml) for a complete example.

---

## Step 2 - Instrument your application

Snow-White needs the OTEL spans emitted by your service to carry the same three identifiers: `service.name`, `api.name`, and `api.version`.

### Option A: Spring Boot (recommended)

If your service uses Spring Boot, the simplest path is to use the two toolkit components provided by Snow-White.

**1. Add the `spring-web-autoconfiguration` dependency**

```xml
<dependency>
  <groupId>io.github.bbortt.snow-white.toolkit</groupId>
  <artifactId>spring-web-autoconfiguration</artifactId>
  <version>${snow-white.version}</version>
</dependency>
```

This automatically enriches every HTTP span with `api.name` and `api.version` based on the OpenAPI spec metadata. The `service.name` is picked up from the standard `OTEL_SERVICE_NAME` environment variable (or from `spring.application.name` as a fallback).

**2. Use the `snow-white-spring-server` code generator (optional but recommended)**

Instead of writing controller interfaces by hand, generate them from your OpenAPI spec. This ensures your implementation stays in sync with the spec and automatically wires the annotation metadata:

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

**3. Configure the OTEL Java agent**

Attach the OpenTelemetry Java agent to your application and point it at your OTEL collector:

```shell
java \
  -javaagent:/path/to/opentelemetry-javaagent.jar \
  -jar app.jar
```

```shell
# Required environment variables
OTEL_SERVICE_NAME=my-service
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_EXPORTER_OTLP_PROTOCOL=grpc
```

### Option B: Manual OTEL enrichment

If you are not using Spring Boot, attach the three attributes to your HTTP spans manually using the OTEL SDK of your choice:

```
service.name  = <your service name>   # set via OTEL_SERVICE_NAME or SDK resource
api.name      = <value of x-api-name in your spec>
api.version   = <value of info.version in your spec>
```

Refer to the [Snow-White semantic convention](https://github.com/bbortt/snow-white/blob/main/semantic-convention/openapi.md) for the full attribute specification.

---

## Step 3 - Publish your specification

Snow-White must have access to your OpenAPI specification before it can correlate traces against it.

### Manual (quick start)

You can register a specification directly via the API index endpoint:

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

> Snow-White stores a reference to the spec URL, not the spec itself.
> The URL must remain accessible at all times.

### Automatic (production)

For production environments, enable the bundled synchronization job in your Helm values so that specs are picked up automatically from your Artifactory repository:

```yaml
snowWhite:
  apiSyncJob:
    enabled: true
    artifactory:
      baseUrl: 'http://artifactory:8082/artifactory'
      repository: 'api-specs-local'
```

See [Installation - API Indexation](installation#api-indexation) for full configuration options.

---

## Step 4 - Generate traces

Run your test suite (or make requests against your service) with the OTEL agent attached. Each HTTP call produces a span that Snow-White can process.

For a quick smoke test during local development:

```shell
curl http://localhost:8080/your-endpoint
```

Traces flow from your application to the OTEL collector, where they are filtered and forwarded to Snow-White.

---

## Step 5 - Calculate coverage

Use the Snow-White CLI to trigger a coverage calculation. Create a configuration file:

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

Then run the CLI (from the repository root after building):

```shell
node toolkit/cli/target/cli/index.js calculate --configFile snow-white.json
```

The CLI exits with code `0` if the quality gate passes, or a non-zero code if it fails - making it suitable for CI pipelines.

See [`toolkit/cli/README.md`](https://github.com/bbortt/snow-white/blob/main/toolkit/cli/README.md) for all exit codes and options.

---

## Step 6 - Review results

Results can be reviewed in the Snow-White UI or consumed from the API. The UI shows:

- **Coverage** - which endpoints were hit and which were not.
- **Quality Gate status** - pass/fail against your configured thresholds.

Snow-White ships with a `basic-coverage` quality gate out of the box. Custom quality gates can be configured in the UI or via the API.

---

## Checklist

- [ ] `x-api-name` and `x-service-name` added to the OpenAPI spec `info` block
- [ ] `OTEL_SERVICE_NAME` matches `x-service-name`
- [ ] OTEL Java agent attached (or manual span enrichment in place)
- [ ] `spring-web-autoconfiguration` dependency added (Spring Boot only)
- [ ] Specification published to the API index
- [ ] CLI config file created and coverage calculation runs successfully

---

## Example Application

The [`example-application`](https://github.com/bbortt/snow-white/tree/main/example-application) in this repository demonstrates a complete integration:

- A minimal Spring Boot service with two endpoints (`GET /ping`, `POST /pong`)
- An annotated OpenAPI spec ([`specs/ping-pong.yml`](https://github.com/bbortt/snow-white/blob/main/example-application/specs/ping-pong.yml))
- Maven build configuration with the code generator and OTEL agent
- A `dev/` environment with Docker Compose to run the full stack locally, including an OTEL collector and the Snow-White services

**Prerequisites:** Java 25, Node.js 22, and Docker or Podman (with Compose) must be installed.

**1. Build the project**

```shell
./mvnw -Pnode package
```

This compiles all modules, generates sources from the OpenAPI spec, and builds the CLI.

**2. Start the Docker environment**

```shell
docker compose -f dev/docker-compose.yaml up -d
```

This starts InfluxDB, Kafka, the OTEL Collector, PostgreSQL, and the Snow-White services.

**3. Configure the InfluxDB access token**

Snow-White stores raw trace data in InfluxDB, which requires an access token.

1. Open the InfluxDB UI at [http://localhost:8086](http://localhost:8086) and log in with `snow-white` / `snow-white`.
2. Create a token with **read and write** access to the `raw-data` bucket.
3. Copy the token into `dev/.env`:

   ```ini
   INFLUXDB_TOKEN=<your-token>
   ```

4. Restart the environment to apply the change:

   ```shell
   docker compose -f dev/docker-compose.yaml down && docker compose -f dev/docker-compose.yaml up -d
   ```

**4. Generate traces**

```shell
curl -ijv http://localhost:8080/ping?message=hello
```

**5. Calculate coverage**

```shell
node toolkit/cli/target/cli/index.js calculate --configFile dev/snow-white.json
```

For a full description of the development environment, see [`DEVELOPMENT.md`](https://github.com/bbortt/snow-white/blob/main/DEVELOPMENT.md).
