# API Gateway

This service acts as the entry point to the **Snow White** application.
It is implemented using **Spring Cloud Gateway** and is the only component exposed publicly (outside the Kubernetes namespace).
The service **handles incoming HTTP requests** and routes them to internal services.
It also hosts the **UI** of the system.

## Purpose

The API Gateway handles:

- Routing requests to internal microservices.
- Acting as a reverse proxy and central access point.
- Hosting the static front-end assets of the Snow White UI.
- Centralized rate limiting, and future cross-cutting concerns.

## Connected Services

The gateway routes requests to the following internal microservices:

- [`quality-gate-api`](../quality-gate-api) – Handles quality gate evaluations and criteria management.
- [`report-coordinator-api`](../report-coordinator-api) – Coordinates data aggregation and reporting logic across the application.

## Required Configuration

These properties **must** be configured either via environment variables or configuration files to ensure the gateway
functions properly:

| Property                                            | Description                                                                                              | Example Value                        |
| --------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------------------------ |
| `SNOW_WHITE_API_GATEWAY_PUBLIC-URL`                 | The public URL where the gateway is exposed. This is the entry point into the Snow White system.         | `http://localhost`                   |
| `SNOW_WHITE_API_GATEWAY_QUALITY-GATE-API-URL`       | Internal URL to the `quality-gate-api` service. Used for routing requests internally within the cluster. | `http://quality-gate-api:8080`       |
| `SNOW_WHITE_API_GATEWAY_REPORT-COORDINATOR-API-URL` | Internal URL to the `report-coordinator-api`. Also used for internal routing.                            | `http://report-coordinator-api:8080` |

## Deployment Notes

- This service is typically exposed via an Ingress or LoadBalancer service in Kubernetes.
- It assumes DNS or ingress rules resolve the `PUBLIC-URL` to the correct gateway pod.
