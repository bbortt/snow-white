# Installation

## Adding the Helm Repository

[Helm](https://helm.sh) must be installed to use the Snow-White chart.
Please refer to Helm's [documentation](https://helm.sh/docs) to get started.

Once Helm has been set up correctly, add the repo as follows:

```shell
helm repo add snow-white https://bbortt.github.io/snow-white
```

If you had already added this repo earlier, run `helm repo update` to retrieve the latest versions of the packages.
You can then run `helm search repo snow-white` to see the charts.

To install the snow-white chart:

```shell
helm install my-snow-white snow-white/snow-white --set snowWhite.ingress.host=[PUBLIC_HOST]
```

> ℹ️ Replace `[PUBLIC_HOST]` by the domain snow-white will be reachable on.

To uninstall the chart:

```shell
helm uninstall my-snow-white
```

⚠️ Note that persistent volume claims are not automatically cleaned up:

```shell
kubectl delete pvc --all
```

## Infrastructure Components

By default, the Snow-White Helm chart is self-contained and automatically deploys the following infrastructure components:

- InfluxDB
- Kafka
- OTel Collector
- PostgreSQL

**Exception:** MinIO is only deployed when using Backstage as your backing API index.

## Replacing Infrastructure with Existing Infrastructure

You may already operate parts of the required infrastructure (for example PostgreSQL, Kafka, or other shared services) outside of the Snow-White Helm chart.
Snow-White allows you to disable the bundled infrastructure components and connect the services to your own existing installations instead.

The following sections describe how to selectively disable infrastructure components provided by the chart and how to configure Snow-White services to use externally managed alternatives.

### Disable PostgreSQL

If you already have a PostgreSQL instance available, you can disable the PostgreSQL deployment that is bundled with the Snow-White chart by setting `postgresql.enabled=false`.

When PostgreSQL is disabled, the following microservices must be configured explicitly to connect to your external database:

- `api-index-api`
- `quality-gate-api`
- `report-coordinator-api`

Each of these services requires the standard Spring datasource environment variables to be provided.
If any required variable is missing, the Helm installation will fail.

The example below shows a `values.yaml` configuration with PostgreSQL disabled.
Database credentials are assumed to be stored in a Kubernetes secret named `my-postgresql-credentials`.

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

#### Database Permissions and Flyway

For security reasons, it is recommended to:

- use database users with Data Manipulation Language (DML) permissions only at runtime, and
- use separate users with Data Definition Language (DDL) permissions for Flyway schema migrations.

To follow this recommendation, additionally configure the following environment variables for each service:

- `SPRING_FLYWAY_USER`
- `SPRING_FLYWAY_PASSWORD`

This allows Snow-White to apply schema migrations using a privileged user while keeping the runtime database access restricted.

### Disable InfluxDB

Disabling InfluxDB is currently **not safely supported**.
At the moment, there are no safeguards or validations for missing configuration values, which may lead to runtime errors.
Until this is properly implemented, disabling InfluxDB is **not recommended**.

#### Using Static Passwords and Tokens

When installing Snow-White using a GitOps operator such as Argo CD, special care must be taken when handling InfluxDB credentials.

Typically, a Git repository containing deployment manifests has no knowledge of the target runtime namespace.
As a result, InfluxDB passwords and tokens may be re-generated on each deployment.
In most cases, this behavior is undesirable, as it can break connectivity for dependent components.

To avoid this, you can provide **static credentials** via a Kubernetes `Secret`.

##### Creating the Secret

Create a secret containing the keys `admin-token` and `admin-password`, for example:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: custom-influxdb-credentials
data:
  admin-password: YWRtaW4tcGFzc3dvcmQ=
  admin-token: YWRtaW4tdG9rZW4=
```

> Note: The values must be Base64-encoded.

##### Referencing the Secret

Next, reference the secret in your `custom_values.yaml`.
Both InfluxDB and all connected microservices will use the credentials defined in this secret.

```yaml
influxdb2:
  adminUser:
    existingSecret: custom-influxdb-credentials
```
