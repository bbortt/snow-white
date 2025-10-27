# Developer Instructions

## Building the Application

To build the **OTEL Event Filter** application:

### 1. Build the Maven artifact:

```shell
./mvnw -Pprod -pl :otel-event-filter-stream -am install
```

### 2. Package it into a Docker image:

```shell
./mvnw -Dimage.tag=latest -Pnative -pl :otel-event-filter-stream spring-boot:build-image
```

## Running Application Tests

Before running tests, ensure:

- Docker is installed and running.
- The API Gateway Docker image is built (see [Building the Application](#building-the-application) above).

### Step 1 - Start Docker Compose

The application tests depend on a running Kafka and Redis instance.
In **one terminal**, run:

```shell
docker compose -f microservices/otel-event-filter-stream/src/apptest/resources/docker-compose-apptest.yaml up
```

### Step 2 - Run the API Gateway tests

In **another terminal**, run:

```shell
./mvnw \
  -Dimage.tag=latest \
  -Papptest \
  -pl :otel-event-filter-stream \
  -Ddocker.network=github_actions \
  verify
```
