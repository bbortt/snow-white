# OpenAPI Coverage Stream

The **OpenAPI Coverage Stream** is responsible for calculating the coverage of actual API usage against declared OpenAPI specifications.
It **analyzes telemetry data** to determine how much of each endpoint, parameter, and response has been exercised by real traffic.

## Purpose

This service is a core component of the **Snow White** application’s observability layer.
It:

- Queries telemetry data stored in InfluxDB.
- Compares runtime API usage against stored OpenAPI specs.
  - Stores and retrieves OpenAPI specs via [`api-index-api`](../api-index-api).
- Publishes coverage reports to Kafka, which are consumed by the [`report-coordinator-api`](../report-coordinator-api).

## Configuration

### Required Configuration

These environment variables **must** be configured for the service to work properly:

<!-- prettier-ignore -->
| Property                                                                | Description                                                | Example Value                             |
|-------------------------------------------------------------------------|------------------------------------------------------------|-------------------------------------------|
| `INFLUXDB_URL`                                                          | URL of the InfluxDB instance used to read telemetry data.  | <http://influxdb:8086>                    |
| `INFLUXDB_ORG`                                                          | Organization name used in InfluxDB.                        | `snow-white`                              |
| `INFLUXDB_BUCKET`                                                       | Name of the InfluxDB bucket containing raw telemetry data. | `raw-data`                                |
| `SNOW_WHITE_OPENAPI_COVERAGE_STREAM_API-INDEX_BASE-URL`                 | Base url to the `api-index-api` microservice.              | <http://localhost:8085>                   |
| `SNOW_WHITE_OPENAPI_COVERAGE_STREAM_CALCULATION-REQUEST-TOPIC`          | Kafka topic used to listen for calculation requests.       | `snow-white-calculation-request`          |
| `SNOW_WHITE_OPENAPI_COVERAGE_STREAM_OPENAPI-CALCULATION-RESPONSE-TOPIC` | Kafka topic used to publish coverage results.              | `snow-white-openapi-calculation-response` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS`                                        | Kafka bootstrap servers used for communication.            | `kafka:9094`                              |

### Optional Configuration

| Property                                         | Description                                              | Default Value |
| ------------------------------------------------ | -------------------------------------------------------- | ------------- |
| `SNOW_WHITE_OPENAPI_COVERAGE_STREAM_INIT-TOPICS` | Whether to auto-create Kafka topics if they don’t exist. | `false`       |

## Usage Notes

- The service reacts to Kafka messages — it does not expose an API.
- OpenAPI specifications must be loaded into `api-index-api` ahead of time.
- The service must have access to consistent telemetry data for accurate calculations.
