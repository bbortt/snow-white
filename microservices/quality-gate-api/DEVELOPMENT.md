# Developer Instructions

## Building the Application

To build the **Quality-Gate API** application:

### 1. Build the Maven artifact:

```shell
./mvnw -Pprod -pl :quality-gate-api -am install
```

### 2. Package it into an Image

```shell
./mvnw -Pnative -Dimage.tag=latest -DskipTests -pl :quality-gate-api spring-boot:build-image
```

The resulting image is: `ghcr.io/bbortt/snow-white/quality-gate-api:latest`.

## Running Application Tests

Before running tests, ensure:

- Docker is installed and running.
- The `quality-gate-api` image has been built (see [Building the Application](#building-the-application) above).

### Step 1 - Start Docker Compose

The application tests depend on a running PostgreSQL instance.
In **one terminal**, run:

```shell
docker compose -f microservices/quality-gate-api/src/apptest/resources/docker-compose-apptest.yaml up
```

### Step 2 - Run the Quality-Gate API tests

In **another terminal**, run:

```shell
./mvnw \
  -Dimage.tag=latest \
  -Papptest \
  -pl :quality-gate-api \
  -Ddocker.network=github_actions \
  verify
```
