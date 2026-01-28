# Developer Instructions

## Building the Job

To build the **API Job**:

### 1. Build the Maven artifact:

```shell
./mvnw -Pprod -pl :api-sync-job -am install
```

### 2. Package it into an Image

```shell
docker build \
  -t ghcr.io/bbortt/snow-white/api-sync-job:latest \
  -f microservices/api-sync-job/Dockerfile \
  --build-arg BUILD_DATE="$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
  --build-arg PROJECT_VERSION="latest" \
  microservices/api-sync-job/
```

## Running Application Tests

Before running tests, ensure:

- Docker is installed and running.
- The `api-sync-job` image has been built (see [Building the Application](#building-the-job) above).

### Step 1 - Create an external Network

Application Tests need a bridged network to connect to.

```shell
docker network create github_actions
```

### Step 2 - Start Docker Compose

The application tests depends on a running JFrog Artifactory & Wiremock instance.
In **the same terminal**, run:

```shell
docker compose -f microservices/api-sync-job/src/apptest/resources/docker-compose-apptest.yaml up
```
