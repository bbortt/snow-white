# Developer Setup Guide

Welcome, and thank you for considering to contribute to **Snow-White**!
This guide walks you through setting up your development environment and building the services.
If you run into any issues, please open an issue or contact the maintainers.

## Quick Start

### 1. Prerequisites

- Java 25 installed
- Node.js 22 installed
- Docker or Podman (with Compose) installed

### 2. Launch the Development Environment

Start all required services using Docker/Podman Compose:

```shell
docker compose -f dev/docker-compose.yaml up -d
```

This includes InfluxDB, Kafka, OTEL Collector, PostgreSQL, and supporting UI tools.
For more on which services are running and their ports, see [Mapped Ports](./docs/architecture.md#mapped-ports).

### 3. Configure InfluxDB Access

You'll need a **Read/Write token** for the raw-data bucket in InfluxDB.

1. Visit the InfluxDB UI (port 8086)
2. The login is `snow-white:snow-white`
3. Create a token with both `read` and `write` into the `raw-data` bucket
4. Add the token to your `dev/.env` file:

```ini
INFLUXDB_TOKEN=[YOUR_TOKEN_GOES_HERE]
```

![InfluxDB Token](dev/influxdb-token.png)

Restart the environment for changes to take effect:

```shell
docker compose -f dev/docker-compose.yaml down && docker compose -f dev/docker-compose.yaml up -d
```

### 4. Generate some Tracing Data

You can use the provided example application to generate some tracing data.

```shell
curl -ijv http://localhost:8080/ping?message=pong
```

### 5. Run the Coverage Calculation

Use the following query to run the coverage calculation against the generated data.

```shell
node toolkit/cli/target/cli/index.js calculate --configFile dev/snow-white.json
```

## Running Tests and Code Quality

Run all unit/integration tests and aggregate coverage:

```shell
./mvnw verify -T 1C
```

To run a [SonarQube](https://www.sonarsource.com) analysis:

1. Start Sonar:

   ```shell
   docker compose -f dev/sonar.yaml up -d
   ```

2. Create a new Sonar project (`snow-white`) and token.

   The initial login to <http://localhost:9000> can be done with `admin:admin`.
   The password must be changed at first login.

   Enter into SonarQube and add a new project called `snow-white`.
   Choose manual setup with a local build environment.
   This will lead you up to the token generation.
   Create a token with a name of your choice, but select "No expiration date".

3. Run the analysis:

   ```shell
   ./mvnw jacoco:report-aggregate sonar:sonar -Dsonar.login=${SONAR_TOKEN}
   ```

### Application Tests

Application tests run tests against the fully built and running application.
To run application tests within GitHub Actions, add the `include:apptests` label in your pull request.

## Building and Running Services

Use the following steps for rapid local development:

1. Build all modules:

   ```shell
   ./mvnw package -b smart
   ```

2. Start the Docker environment (if not already running):

   ```shell
   docker compose -f dev/docker-compose.yaml up -d
   ```

3. Stop the microservice you want to develop and run it manually:

   ```shell
   ./mvnw spring-boot:run -pl :<microservice-name>
   ```

### JDK Builds

These microservices run in a traditional JVM image:

- `api-gateway`
- `api-sync-job`
- `openapi-coverage-stream`

Build an image using:

```shell
./mvnw -am -pl :<maven-module> -b smart install

docker build \
    -f "microservices/<maven-module>/Dockerfile" \
    -t "ghcr.io/bbortt/snow-white/<maven-module>:latest" \
    --build-arg BUILD_DATE="$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
    --build-arg PROJECT_VERSION="latest" \
    "microservices/<maven-module>"
```

Alternatively, install all packages and microservices first, then simply run the following script:

```shell
./mvnw -b smart -Pprod install
.github/scripts/build-oci-images.sh latest
```

### Native Builds

These microservices support native image builds:

- `api-index-api`
- `otel-event-filter-stream`
- `quality-gate-api`
- `report-coordinator-api`

Build an image using:

```shell
./mvnw -am -pl :<maven-module> -b smart install
./mvnw -DskipTests -Pnative -pl :<maven-module> spring-boot:build-image
```

Build all native microservices:

```shell
./mvnw -b smart -Pprod install
./mvnw -DskipTests -Pnative -b smart -pl :api-index-api,:otel-event-filter-stream,:report-coordinator-api,:quality-gate-api spring-boot:build-image
```

For development, override `-Dimage.tag=latest` to build a "latest" image for usage with [Docker/Podman Compose](#quick-start).
Podman is supported using the `podman` profile as well.

## Maven Proxy Setup

If you're behind a corporate proxy, use the following snippet in `.mvn/settings.xml`:

```xml
<settings>
  <mirrors>
    <mirror>
      <id>central-mirror</id>
      <name>...</name>
      <url>...</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
    <mirror>
      <id>confluent-mirror</id>
      <name>...</name>
      <url>...</url>
      <mirrorOf>confluent</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

Make sure to add this to `.mvn/settings.xml` (this file is gitignored).
