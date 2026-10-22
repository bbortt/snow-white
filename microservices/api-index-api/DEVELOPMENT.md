# Developer Instructions

## Building the Application

To build the **API Index API** application:

### 1. Build the Maven artifact:

```shell
./mvnw -Pprod -pl :api-index-api -am install
```

### 2. Package it into an Image

```shell
./mvnw -Pnative -Dimage.tag=latest -DskipTests -pl :api-index-api spring-boot:build-image
```

The resulting image is: `ghcr.io/bbortt/snow-white/api-index-api:latest`.

## Running Application Tests

Before running tests, ensure:

- Docker is installed and running.
- The `api-index-api` image has been built (see [Building the Application](#building-the-application) above).

### Step 1 - Create an external Network

Application Tests need a bridged network to connect to.

```shell
docker network create github_actions
```

### Step 2 - Start Docker Compose

The application tests depend on a running Kafka and PostgreSQL instance.
In **one terminal**, run:

```shell
docker compose -f microservices/api-index-api/src/apptest/resources/docker-compose-apptest.yaml up
```

### Step 3 - Run the Quality-Gate API tests

In **another terminal**, run:

```shell
./mvnw \
  -Dimage.tag=latest \
  -Papptest \
  -pl :api-index-api \
  -Ddocker.network=github_actions \
  verify
```
