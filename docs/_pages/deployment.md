---
title: 'Deployment'
permalink: /deployment/
toc: true
toc_sticky: true
---

Snow-White is distributed as a [Helm](https://helm.sh) chart and can be deployed to any Kubernetes cluster.

## Installation

Snow-White is distributed as a [Helm](https://helm.sh) chart.
If you haven't used Helm before, refer to the [official documentation](https://helm.sh/docs) to get started.

Once Helm has been set up correctly, add the repository:

```shell
helm repo add snow-white https://bbortt.github.io/snow-white
helm repo update
helm search repo snow-white
```

Then, continue by installing the Chart:

```shell
helm install my-snow-white snow-white/snow-white \
  --set snowWhite.host=[PUBLIC_HOST]
```

> Replace `[PUBLIC_HOST]` with the domain Snow-White will be reachable on.

You must also enable one of the following ingress options:

| Option                                                                                                    | Value                              | Notes                                                                                                      |
| --------------------------------------------------------------------------------------------------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| [Kubernetes Gateway API](https://kubernetes.io/docs/concepts/services-networking/gateway) _(recommended)_ | `snowWhite.httproute.enabled=true` |                                                                                                            |
| [Kubernetes Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress)                     | `snowWhite.ingress.enabled=true`   | NGINX Ingress Controller is [retired](https://www.kubernetes.dev/blog/2025/11/12/ingress-nginx-retirement) |

### Uninstall

```shell
helm uninstall my-snow-white
```

> Persistent volume claims are **not** automatically cleaned up:
>
> ```shell
> kubectl delete pvc --all
> ```

## API Indexation

By default, Snow-White does not deploy an API synchronization job.

For a quick start, register specifications manually via `curl`.
The [`api-index-api` OpenAPI spec](https://github.com/bbortt/snow-white/blob/main/microservices/api-index-api/src/main/resources/openapi/v1-api-index-api.yml) describes how to register a specification.

For production environments, enable the bundled `CronJob`:

```yaml
snowWhite.apiSyncJob.enabled=true
```

Currently, [JFrog Artifactory](https://jfrog.com/artifactory) is the only supported API source.
Snow-White expects a [generic Artifactory repository](https://jfrog.com/help/r/jfrog-artifactory-documentation/generic-repositories) containing specs as plain text files.

```yaml
snowWhite.apiSyncJob.artifactory.baseUrl: 'http://localhost:8082/artifactory'
snowWhite.apiSyncJob.artifactory.repository: 'snow-white-generica-local'
```

> Snow-White stores **references** to spec URLs, not the specs themselves.
> Artifactory must remain available at all times.

### Artifactory Access Token

Inject the token via a Kubernetes `Secret`:

```yaml
snowWhite:
  apiSyncJob:
    additionalEnvs:
      - name: SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_ACCESS_TOKEN
        valueFrom:
          secretKeyRef:
            name: artifactory-secret
            key: artifactory-token
```

### Memory Management

The sync job ships with 1024Mi memory and 500m CPU and processes 3 specs in parallel.

**Increase memory** if you hit OOM with large specs:

```yaml
snowWhite:
  apiSyncJob:
    resources:
      memory:
        request: 2048Mi
        limit: 2048Mi
```

**Reduce parallelism** to lower peak memory at the cost of sync time:

```yaml
snowWhite:
  apiSyncJob:
    additionalEnvs:
      - name: SNOW_WHITE_API_SYNC_JOB_MAX_PARALLEL_SYNC_TASKS
        value: 1
```

## Ingesting OTeL Data

Snow-White deploys its own [OTel Collector](https://opentelemetry.io/docs/collector) by default.
That collector is used both for ingesting tracing data, as well as [exposing Snow-White's own metrics](#exporting-telemetry).

> Ingesting tracing data is a core concept of Snow-White (see ["How It Works"](/#how-it-works)).
> Snow-White cannot function without OTeL data.
>
> At the same time, **Snow-White is a telemetry sink, not a monitoring backend.**
> It only persists trace data — all other signal types (logs, metrics) are dropped.
> You only need to connect to the `/v1/traces` endpoint; sending other signals has no effect.
>
> The bundled OTel Collector and its storage are dedicated to Snow-White's own use.
> Because Snow-White also drops attributes and telemetry it does not need, the data it holds
> is unsuitable for general service monitoring.
>
> The recommended approach is to deploy your own OTel Collector in front of Snow-White
> and use it to fan out data: route a copy to your monitoring backend and a copy to Snow-White.
>
> On the other hand, if your infrastructure already includes an InfluxDB cluster,
> you might as well use that instead (see ["Disable InfluxDB"](#disable-influxdb))

### In-Cluster

To connect services running inside the same Kubernetes cluster, target the OTel Collector service directly via its FQDN:

```text
http://<release-name>-snow-white-otel-collector.<namespace>.svc.cluster.local:4317
```

For example, with the Helm release name `my-snow-white` deployed to the `observability` namespace:

```text
http://snow-white-otel-collector-my-snow-white.observability.svc.cluster.local:4317
```

### Outside the Cluster

The OTel Collector is also reachable through the public ingress by default, via the `/v1/traces` path.
For example, if Snow-White is available at `https://my.snow.white`, send traces to:

```text
https://my.snow.white/v1/traces
```

This is controlled by the `otelCollector.exposeThroughApiGateway` Helm value, which defaults to `true`.
Set it to `false` to restrict OTel ingestion to in-cluster access only:

```yaml
otelCollector:
  exposeThroughApiGateway: false
```

## Exporting Telemetry

Snow-White exposes its own OTEL telemetry.
Connect it to an external collector:

```yaml
otelCollector:
  connectToExternalOtelCollector:
    endpoint: 'my-endpoint:4317'
    exportLogs: true
    exportMetrics: true
    exportTraces: true
```

### Monitoring the OTel Collector

The bundled collector exposes Prometheus metrics on port `8888` at `/metrics`.
Enable scraping with:

```yaml
otelCollector:
  annotations:
    prometheus.io/scrape: 'true'
    prometheus.io/port: '8888'
    prometheus.io/path: '/metrics'
```

## Replacing Bundled Infrastructure

### Disable PostgreSQL

Add this to your `values.yaml`:

```yaml
postgresql:
  enabled: false
```

When disabled, configure datasource env vars for `api-index-api`, `quality-gate-api`, and `report-coordinator-api`.
Example with credentials from a Secret:

```yaml
postgresql:
  enabled: false

snowWhite:
  apiIndexApi:
    additionalEnvs:
      - name: SPRING_DATASOURCE_URL
        value: jdbc:postgresql://my.database:5432/api-index-api
      - name: SPRING_DATASOURCE_USERNAME
        value: api-index-api
      - name: SPRING_DATASOURCE_PASSWORD
        valueFrom:
          secretKeyRef:
            name: my-postgresql-credentials
            key: api-index-password
  qualityGateApi:
    additionalEnvs:
      - name: SPRING_DATASOURCE_URL
        value: jdbc:postgresql://my.database:5432/quality-gate-api
      - name: SPRING_DATASOURCE_USERNAME
        value: quality-gate-api
      - name: SPRING_DATASOURCE_PASSWORD
        valueFrom:
          secretKeyRef:
            name: my-postgresql-credentials
            key: quality-gate-api-password
  reportCoordinatorApi:
    additionalEnvs:
      - name: SPRING_DATASOURCE_URL
        value: jdbc:postgresql://my.database:5432/report-coordinator-api
      - name: SPRING_DATASOURCE_USERNAME
        value: report-coordinator-api
      - name: SPRING_DATASOURCE_PASSWORD
        valueFrom:
          secretKeyRef:
            name: my-postgresql-credentials
            key: report-coordinator-api-password
```

Snow-White won't accept a deployment that is missing any of the above environment variables.

For security, use DML-only credentials at runtime and separate DDL credentials for Flyway via `SPRING_FLYWAY_USER` / `SPRING_FLYWAY_PASSWORD`.

### Disable InfluxDB

Add this to your `values.yaml`:

```yaml
influxdb2:
  enabled: false
```

This disables the bundled InfluxDB StatefulSet, but the Snow-White microservices will still attempt to connect to an InfluxDB instance.
Unlike PostgreSQL, the Helm chart does not expose dedicated values for configuring an external InfluxDB connection.
You would need to supply the relevant environment variables manually via `additionalEnvs` — which requires reverse-engineering the expected configuration from the microservice sources.

### InfluxDB Static Credentials (GitOps)

When using Argo CD or similar GitOps operators, regenerated credentials will break connectivity on each sync.
Provide static credentials via a Secret:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: custom-influxdb-credentials
data:
  admin-password: YWRtaW4tcGFzc3dvcmQ=
  admin-token: YWRtaW4tdG9rZW4=
```

```yaml
influxdb2:
  adminUser:
    existingSecret: custom-influxdb-credentials
```

## Custom Truststores

Truststores must be in JKS format, stored in a Kubernetes Secret:

```yaml
jssecacerts:
  secretName: 'my-secret'
  key: 'truststore.jks'
```

The JRE picks up the truststore automatically from `$JAVA_HOME/lib/security/jssecacerts`.
