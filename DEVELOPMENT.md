# Developer Instructions

## Local Environment Setup

## Microservices and Ports

Depending on where you run the microservices, different ports are being mapped.
Basically, for fast development you can execute `./mvnw package` once, then invoke the [`dev/docker-compose.yaml`](dev/docker-compose.yaml).
It will start up all microservices and required dependencies.

Next, go ahead, stop the one microservice you'd like to develop and start it up using Spring directly.

Of course, you could theoretically start multiple services natively.

Here's the mapped ports:

| Service                                                                      | Port with Spring + `dev` profile | Port in `docker-compose.yaml` |
| ---------------------------------------------------------------------------- | -------------------------------- | ----------------------------- |
| [`example-application`](./example-application)                               | `8080`                           | `8080`                        |
| [`api-gateway`](./microservices/api-gateway)                                 | `9080`                           | `80`                          |
| [`kafka-event-filter`](./microservices/kafka-event-filter)                   | `8081`                           | -                             |
| [`openapi-coverage-service`](./microservices/openapi-coverage-service)       | `8083`                           | `8083`                        |
| [`quality-gate-api`](./microservices/quality-gate-api)                       | `8082`                           | `8082`                        |
| [`report-coordination-service`](./microservices/report-coordination-service) | `8084`                           | `8084`                        |

Other services that are being mapped only using [`dev/docker-compose.yaml`](dev/docker-compose.yaml):

| Service                      | Port                                               |
| ---------------------------- | -------------------------------------------------- |
| OTEL Collector               | `1888`,`8888`,`8889`,`13133`,`4317`,`4318`,`55679` |
| Kafka                        | `9092`,`9094`                                      |
| Kafka UI                     | `8090`                                             |
| Redis                        | `6379`                                             |
| Redis Insight                | `5540`                                             |
| InfluxDB                     | `8086`                                             |
| Service Interface Repository | `3000`                                             |
