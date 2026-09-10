# Developer Instructions

## Building the Application

To build the **API Gateway** application:

### 1. Build the Maven artifact

```shell
./mvnw -Pprod -pl :api-gateway -am install
```

### 2. Package it into an Image

```shell
docker build \
  -t ghcr.io/bbortt/snow-white/api-gateway:latest \
  -f microservices/api-gateway/Dockerfile \
  --build-arg BUILD_DATE="$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
  --build-arg PROJECT_VERSION="latest" \
  microservices/api-gateway/
```

## Running Application Tests

Before running tests, ensure:

- Docker is installed and running.
- The API Gateway Docker image is built (see [Building the Application](#building-the-application) above).

### Step 1 - Create Network

**Open a terminal** and run:

```shell
docker network create github_actions
```

### Step 2 - Start Docker Compose

The application tests depends on a running Wiremock instance.
In **the same terminal**, run:

```shell
docker compose -f microservices/api-gateway/src/apptest/resources/docker-compose-apptest.yaml up
```

### Step 3 - Run the API Gateway tests

In **another terminal**, run:

```shell
./mvnw \
  -Dimage.tag=latest \
  -Papptest \
  -pl :api-gateway \
  -Ddocker.network=github_actions \
  verify
```
