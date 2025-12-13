# Report Coordinator APIs

The **Report Coordinator API** is a core backend component of the **Snow White** system.
It coordinates API coverage and quality gate reports by triggering calculations, collecting results, and storing them for later querying.
This service is **exposed via the API Gateway** and serves as the backend for the UI reporting interface.

## Purpose

The service acts as an orchestrator for report-related workflows:

- **Triggers** coverage and quality gate calculations.
- **Listens** to result topics (e.g. OpenAPI coverage results via Kafka).
- **Persists** results into a PostgreSQL database.
- **Serves** data to the frontend for display and analysis.

## Configuration

### Required Configuration

These environment variables **must** be configured for the service to work properly:

<!-- markdownlint-disable markdownlint-sentences-per-line -->

| Property                                                               | Description                                                                                                | Example Value                                            |
| ---------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| `SNOW_WHITE_REPORT_COORDINATOR_API_CALCULATION-REQUEST-TOPIC`          | Kafka topic to which calculation requests are sent.                                                        | `snow-white-calculation-request`                         |
| `SNOW_WHITE_REPORT_COORDINATOR_API_OPENAPI-CALCULATION-RESPONSE_TOPIC` | Kafka topic from which calculation responses are consumed.                                                 | `snow-white-openapi-calculation-response`                |
| `SNOW_WHITE_REPORT_COORDINATOR_API_PUBLIC_API_GATEWAY_URL`             | Public URL of the [`api-gateway`](../api-gateway). Used to construct external links and route information. | `http://localhost`                                       |
| `SNOW_WHITE_REPORT_COORDINATOR_API_QUALITY-GATE-API-URL`               | Internal URL to the `quality-gate-api` for data collection.                                                | `http://quality-gate-api:8080`                           |
| `SPRING_DATASOURCE_URL`                                                | JDBC URL for the PostgreSQL instance.                                                                      | `jdbc:postgresql://postgres:5432/report-coordinator-api` |
| `SPRING_DATASOURCE_USERNAME`                                           | Runtime database username (read/write).                                                                    | `report_coord_app`                                       |
| `SPRING_DATASOURCE_PASSWORD`                                           | Runtime database password (of `${SPRING_DATASOURCE_PASSWORD}`).                                            | `strongpassword2`                                        |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS`                                       | Kafka bootstrap servers used for messaging.                                                                | `kafka:9094`                                             |

<!-- markdownlint-enable markdownlint-sentences-per-line -->

### Optional Configuration

| Property                                        | Description                                                                           | Default Value                   |
| ----------------------------------------------- | ------------------------------------------------------------------------------------- | ------------------------------- |
| `SNOW_WHITE_REPORT_COORDINATOR_API_INIT-TOPICS` | Whether to auto-create Kafka topics if they don’t exist.                              | `false`                         |
| `SPRING_FLYWAY_USER`                            | Database username used for applying database migrations.                              | `${SPRING_DATASOURCE_USERNAME}` |
| `SPRING_FLYWAY_PASSWORD`                        | Database password (of `${SPRING_FLYWAY_USER}`) used for applying database migrations. | `${SPRING_DATASOURCE_PASSWORD}` |

> ⚠️ In production, it is **recommended** to restrict the runtime database user to basic CRUD access.
> The Flyway user should only be used during migrations.

## Deployment Notes

- This service is being natively compiled for production.
- It is available externally via the [`api-gateway`](../api-gateway).
