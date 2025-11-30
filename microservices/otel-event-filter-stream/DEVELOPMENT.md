# Developer Instructions

## Building the Application

To build the **OTEL Event Filter** application:

### 1. Build the Maven artifact:

```shell
./mvnw -Pprod -pl :otel-event-filter-stream -am install
```

### 2. Package it into an Image

```shell
./mvnw -Pnative -Dimage.tag=latest -DskipTests -pl :otel-event-filter-stream spring-boot:build-image
```

The resulting image is: `ghcr.io/bbortt/snow-white/otel-event-filter-stream:latest`.

## Running Application Tests

Before running tests, ensure:

- Docker is installed and running.
- The `otel-event-filter-stream` image has been built (see [Building the Application](#building-the-application) above).

### Step 1 - Create an external Network

Application Tests need a bridged network to connect to.

```shell
docker network create github_actions
```

### Step 2 - Start Docker Compose

The application tests depend on a running Kafka and Redis instance.
In **one terminal**, run:

```shell
docker compose -f microservices/otel-event-filter-stream/src/apptest/resources/docker-compose-apptest.yaml up
```

### Step 3 - Run the OTEL Event Filter tests

In **another terminal**, run:

```shell
./mvnw \
  -Dimage.tag=latest \
  -Papptest \
  -pl :otel-event-filter-stream \
  -Ddocker.network=github_actions \
  verify
```
