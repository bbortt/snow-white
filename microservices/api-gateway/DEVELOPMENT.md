# Developer Instructions

## Building the Application

To build the **API Gateway** application:

### 1. Build the Maven artifact:

```shell
./mvnw -Dimage.tag=latest  -Pprod -pl :api-gateway -am package
docker build -t ghcr.io/bbortt/snow-white/api-gateway:latest -f microservices/api-gateway/Dockerfile microservices/api-gateway/
```

### 2. Package it into a Docker image:

```shell
docker build \
  -t ghcr.io/bbortt/snow-white/api-gateway:latest \
  -f microservices/api-gateway/Dockerfile \
  microservices/api-gateway/
```

## Running Application Tests

Before running tests, ensure:

- Docker is installed and running.
- The API Gateway Docker image is built (see [Building the Application](#building-the-application) above).

### Step 1 - Create a Docker network

```shell
docker network create api-gateway-network
```

### Step 2 - Start WireMock

In **one terminal**, run:

```shell
docker run --rm \
  -p 9000:8080 \
  --name wiremock \
  --network github_actions \
  wiremock/wiremock:3x-alpine
```

### Step 3 - Run the API Gateway tests

In **another terminal**, run:

```shell
./mvnw \
  -Dimage.tag=latest \
  -Dwiremock.baseURL=http://host.docker.internal:9000 \
  -Papptest \
  -pl :api-gateway \
  -Ddocker.network=github_actions \
  verify
```
