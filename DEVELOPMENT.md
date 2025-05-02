# Developer Setup Guide

## Local Environment Setup

To start the required services locally using [Docker](https://www.docker.com/), run:

```shell
docker compose -f dev/docker-compose.yaml up -d
```

For details on the microservices and their respective ports, see the [Microservices and Ports](#microservices-and-ports)
section.

### Setting Up InfluxDB

Once the services are running, you need to extract an InfluxDB token and store it in the [`dev/.env`](dev/.env) file.
The token must have **Read and Write** access to the `raw-data` bucket.

```ini
INFLUXDB_TOKEN=[YOUR_TOKEN_GOES_HERE]
```

![InfluxDB Token](dev/influxdb-token.png)

After saving the token, restart the Docker Compose environment for changes to take effect.

```shell
docker compose -f dev/docker-compose.yaml down && docker compose -f dev/docker-compose.yaml up -d
```

## Microservices and Ports

The application consists of several microservices running on different ports.
For rapid development, follow these steps:

1. Run the following command to package the application:

   ```shell
   ./mvnw package
   ```

   Optionally add the `-T1C` argument to run a parallel build using one thread per CPU core.

2. Start all microservices and dependencies using Docker Compose:

   ```shell
   docker compose -f dev/docker-compose.yaml up -d
   ```

3. Stop the specific microservice you want to develop and start it manually using Spring Boot.

4. Optionally, you can run multiple services natively if needed.

### Mapped Ports

| Service                                                                      | Spring Boot (`dev` profile) | Docker Compose (`docker-compose.yaml`) |
| ---------------------------------------------------------------------------- | --------------------------- | -------------------------------------- |
| [`example-application`](./example-application)                               | `8080`                      | `8080`                                 |
| [`api-gateway`](./microservices/api-gateway)                                 | `9080`                      | `80`                                   |
| [`kafka-event-filter`](./microservices/kafka-event-filter)                   | -                           | -                                      |
| [`openapi-coverage-service`](./microservices/openapi-coverage-service)       | -                           | -                                      |
| [`quality-gate-api`](./microservices/quality-gate-api)                       | `8081`                      | `8081`                                 |
| [`report-coordination-service`](./microservices/report-coordination-service) | `8084`                      | `8084`                                 |

The UI Development Server (of `api-gateway`) is running on port 9001.

### Additional Services

The following services are only mapped in [`dev/docker-compose.yaml`](dev/docker-compose.yaml):

| Service                      | Ports                                                    |
| ---------------------------- | -------------------------------------------------------- |
| OTEL Collector               | `1888`, `8888`, `8889`, `13133`, `4317`, `4318`, `55679` |
| Kafka                        | `9092`, `9094`                                           |
| Kafka UI                     | `8090`                                                   |
| Redis                        | `6379`                                                   |
| Redis Insight                | `5540`                                                   |
| InfluxDB                     | `8086`                                                   |
| Service Interface Repository | `3000`                                                   |

This guide ensures a structured and efficient development setup. Happy coding!

## Sonar Analysis

You can start a local [SonarQube](https://www.sonarsource.com/) service using Docker Compose:

```shell
docker compose -f dev/sonar.yaml up -d
```

The initial login on http://localhost:9000 can be done with `admin:admin`.
The password must be changed at first login.

Enter into SonarQube and add a new project called `snow-white`.
Choose manual setup with a local build environment.
This will lead you up to the token generation.
Create a token with a name of your choice, but select "No expiration date".

Afterward, you're able to analyze the project using your token:

```shell
./mvnw verify sonar:sonar -Dsonar.login=${SONAR_TOKEN}
```

## Maven Proxy Setup

The following proxies are required in order to build `snow-white` behind corporate proxies:

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

You should add them to [`.mvn/settings.xml`](./.mvn/settings.xml) which is being ignored by `git`.
