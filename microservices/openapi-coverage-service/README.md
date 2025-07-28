# OpenAPI Coverage Service

The **OpenAPI Coverage Service** is responsible for calculating the coverage of actual API usage against declared OpenAPI specifications.
It **analyzes telemetry data** to determine how much of each endpoint, parameter, and response has been exercised by real traffic.

## Purpose

This service is a core component of the **Snow White** application’s observability layer. It:

- Queries telemetry data stored in InfluxDB.
- Compares runtime API usage against stored OpenAPI specs.
  - Stores and retrieves OpenAPI specs via Redis.
- Publishes coverage reports to Kafka, which are consumed by the [`report-coordination-service`](../report-coordination-service).

## Configuration

### Required Configuration

These environment variables **must** be configured for the service to work properly:

| Property                                                                 | Description                                                | Example Value                             |
| ------------------------------------------------------------------------ | ---------------------------------------------------------- | ----------------------------------------- |
| `INFLUXDB_URL`                                                           | URL of the InfluxDB instance used to read telemetry data.  | `http://influxdb:8086`                    |
| `INFLUXDB_ORG`                                                           | Organization name used in InfluxDB.                        | `snow-white`                              |
| `INFLUXDB_BUCKET`                                                        | Name of the InfluxDB bucket containing raw telemetry data. | `raw-data`                                |
| `SPRING_DATA_REDIS_HOST`                                                 | Redis host used to retrieve OpenAPI specifications.        | `redis`                                   |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS`                                         | Kafka bootstrap servers used for communication.            | `kafka:9094`                              |
| `SNOW_WHITE_OPENAPI_COVERAGE_SERVICE_CALCULATION-REQUEST-TOPIC`          | Kafka topic used to listen for calculation requests.       | `snow-white-calculation-request`          |
| `SNOW_WHITE_OPENAPI_COVERAGE_SERVICE_OPENAPI-CALCULATION-RESPONSE-TOPIC` | Kafka topic used to publish coverage results.              | `snow-white-openapi-calculation-response` |

### Optional Configuration

| Property                                          | Description                                              | Default Value |
| ------------------------------------------------- | -------------------------------------------------------- | ------------- |
| `SNOW_WHITE_OPENAPI_COVERAGE_SERVICE_INIT-TOPICS` | Whether to auto-create Kafka topics if they don’t exist. | `false`       |

## Usage Notes

- The service reacts to Kafka messages — it does not expose an API.
- OpenAPI specifications must be loaded into Redis ahead of time.
- The service must have access to consistent telemetry data for accurate calculations.
