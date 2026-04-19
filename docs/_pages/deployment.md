---
title: 'Deployment'
permalink: /deployment/
toc: true
toc_sticky: true
---

Snow-White is distributed as a [Helm](https://helm.sh) chart and can be deployed to any Kubernetes cluster.

## Adding the Helm Repository

[Helm](https://helm.sh) must be installed to use the Snow-White chart.
Please refer to Helm's [documentation](https://helm.sh/docs) to get started.

Once Helm has been set up correctly, add the repo:

```shell
helm repo add snow-white https://bbortt.github.io/snow-white
helm repo update
helm search repo snow-white
```

**Install:**

```shell
helm install my-snow-white snow-white/snow-white \
  --set snowWhite.ingress.host=[PUBLIC_HOST]
```

> Replace `[PUBLIC_HOST]` with the domain Snow-White will be reachable on.

**Uninstall:**

```shell
helm uninstall my-snow-white
```

> Persistent volume claims are **not** automatically cleaned up:
>
> ```shell
> kubectl delete pvc --all
> ```

---

## API Indexation

By default, Snow-White does not deploy an API synchronization job.

For a quick start, register specifications manually via `curl`.
The [`api-index-api` OpenAPI spec](https://github.com/bbortt/snow-white/blob/main/microservices/api-index-api/src/main/resources/openapi/v1-api-index-api.yml) describes how to register a specification.

For production environments, enable the bundled `CronJob`:

```yaml
snowWhite.apiSyncJob.enabled=true
```

Currently JFrog Artifactory is the only supported API source.
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

---

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

---

## Replacing Bundled Infrastructure

### Disable PostgreSQL

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

For security, use DML-only credentials at runtime and separate DDL credentials for Flyway via `SPRING_FLYWAY_USER` / `SPRING_FLYWAY_PASSWORD`.

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

> Disabling InfluxDB entirely is **not safely supported** in the current release.

---

## Custom Truststores

Truststores must be in JKS format, stored in a Kubernetes Secret:

```yaml
jssecacerts:
  secretName: 'my-secret'
  key: 'truststore.jks'
```

The JRE picks up the truststore automatically from `$JAVA_HOME/lib/security/jssecacerts`.
