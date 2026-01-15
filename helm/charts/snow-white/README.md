# snow-white

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=flat-square)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square)
![AppVersion: v1.0.0-ci.0](https://img.shields.io/badge/AppVersion-v1.0.0--ci.0-informational?style=flat-square)

A Helm chart for deploying [`snow-white`](https://github.com/bbortt/snow-white).

## Values

### Scheduling and Affinity

| Key                       | Type   | Default  | Description                                                                                         |
| ------------------------- | ------ | -------- | --------------------------------------------------------------------------------------------------- |
| affinity                  | object | `{}`     | Custom affinity rules. Overrides all presets.                                                       |
| nodeAffinityPreset.key    | string | `""`     | Node label key to match. Ignored if `affinity` is set. E.g.: `key: "kubernetes.io/e2e-az-name"`.    |
| nodeAffinityPreset.type   | string | `""`     | Node affinity preset configuration. Ignored if `affinity` is set. Allowed values: `soft` or `hard`, |
| nodeAffinityPreset.values | list   | `[]`     | Node label values to match. Ignored if `affinity` is set. E.g.: `values: [ 'e2e-az1', 'e2e-az2' ]`. |
| podAffinityPreset         | string | `""`     | Pod affinity preset. Ignored if `affinity` is set. Allowed values: `soft`, `hard`.                  |
| podAntiAffinityPreset     | string | `"soft"` | Pod anti-affinity preset. Ignored if `affinity` is set. Allowed values: `soft`, `hard`.             |

### Infrastructure

| Key          | Type   | Default                                 | Description                                 |
| ------------ | ------ | --------------------------------------- | ------------------------------------------- |
| commonLabels | object | `app.kubernetes.io/part-of: snow-white` | Labels applied to infrastructure resources. |

### Corporate Settings

| Key                      | Type   | Default     | Description                               |
| ------------------------ | ------ | ----------- | ----------------------------------------- |
| global.imagePullSecrets  | list   | `[]`        | List of image pull secrets.               |
| snowWhite.image.registry | string | `"ghcr.io"` | Image registry for Snow-White components. |

### Image Parameters

| Key              | Type   | Default          | Description        |
| ---------------- | ------ | ---------------- | ------------------ |
| image.pullPolicy | string | `"IfNotPresent"` | Image pull policy. |

### Infrastructure (InfluxDB)

| Key              | Type | Default | Description                                       |
| ---------------- | ---- | ------- | ------------------------------------------------- |
| influxdb.enabled | bool | `true`  | Deploy InfluxDB StatefulSet alongside Snow-White. |

### Infrastructure (Kafka)

| Key                  | Type   | Default                    | Description                                                        |
| -------------------- | ------ | -------------------------- | ------------------------------------------------------------------ |
| kafka.clusterId      | string | `"NmE4MjRhYjI1MjkwNGI5ZG"` | Kafka cluster ID. Run "kafka-storage random-uuid" to generate one. |
| kafka.enabled        | bool   | `true`                     | Deploy Kafka StatefulSet alongside Snow-White.                     |
| kafka.image.name     | string | `"confluentinc/cp-kafka"`  | Image name.                                                        |
| kafka.image.registry | string | `"docker.io"`              | Image registry.                                                    |
| kafka.image.tag      | string | `"8.1.1"`                  | Image tag.                                                         |
| kafka.storageClass   | string | `"hostpath"`               | Storage class for Kafka persistent volumes.                        |
| kafka.storageSize    | string | `"10Gi"`                   | Size of the storage for Kafka.                                     |

### OpenTelemetry Collector

| Key                                    | Type   | Default                                                                                                          | Description                                                                                                  |
| -------------------------------------- | ------ | ---------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| otelCollector.exposeThroughIngress     | bool   | `true`                                                                                                           | Whether to expose the OTeL collector through the (public) ingress.                                           |
| otelCollector.image.registry           | string | `"docker.io"`                                                                                                    | Image registry.                                                                                              |
| otelCollector.image.tag                | string | `"0.143.1"`                                                                                                      | Image tag.                                                                                                   |
| otelCollector.influxdb.bucket          | string | `"raw-data"`                                                                                                     | InfluxDB bucket.                                                                                             |
| otelCollector.influxdb.endpoint        | string | The chart will connect to the provisioned InfluxDB StatefulSet automatically.                                    | InfluxDB endpoint.                                                                                           |
| otelCollector.influxdb.org             | string | `"snow-white"`                                                                                                   | InfluxDB organization.                                                                                       |
| otelCollector.influxdb.token           | string | The chart will connect to the provisioned InfluxDB StatefulSet automatically.                                    | InfluxDB token.                                                                                              |
| otelCollector.kafka.inboundTopic       | string | `"snow-white_inbound"`                                                                                           | The "inbound" Kafka topic for unfiltered events.                                                             |
| otelCollector.kafka.outbountTopic      | string | `"snow-white_outbound"`                                                                                          | The "outbound" Kafka filtered events being persisted to InfluxDB.                                            |
| otelCollector.pipeline.resourceCleanup | object | `{"attributes":[{"action":"delete","pattern":"^process\\..+"},{"action":"delete","pattern":"^telemetry\\..+"}]}` | Attributes to clean up from span resources. Reduces required storage space, but limits filter functionality! |

### Infrastructure (PostgreSQL)

| Key                                        | Type   | Default                                | Description                                                             |
| ------------------------------------------ | ------ | -------------------------------------- | ----------------------------------------------------------------------- |
| postgresql.enabled                         | bool   | `true`                                 | Deploy PostgreSQL StatefulSet alongside Snow-White.                     |
| postgresql.primary.initdb.scriptsConfigMap | string | `"snow-white-postgresql-init-scripts"` | Name of the ConfigMap containing initialization scripts for PostgreSQL. |

### Snow-White API Gateway

| Key                                 | Type   | Default       | Description                                              |
| ----------------------------------- | ------ | ------------- | -------------------------------------------------------- |
| snowWhite.apiGateway.additionalEnvs | list   | `[]`          | Additional environment variables forwarded to container. |
| snowWhite.apiGateway.image.tag      | string | `""`          | Image tag.                                               |
| snowWhite.apiGateway.service.port   | int    | `80`          | Exposed port for the API Gateway service.                |
| snowWhite.apiGateway.service.type   | string | `"ClusterIP"` | Type of Kubernetes service.                              |

### Snow-White Report Coordinator API

| Key                                           | Type   | Default | Description                                              |
| --------------------------------------------- | ------ | ------- | -------------------------------------------------------- |
| snowWhite.apiIndexApi.additionalEnvs          | list   | `[]`    | Additional environment variables forwarded to container. |
| snowWhite.apiIndexApi.image.tag               | string | `""`    | Image tag.                                               |
| snowWhite.reportCoordinatorApi.additionalEnvs | list   | `[]`    | Additional environment variables forwarded to container. |
| snowWhite.reportCoordinatorApi.image.tag      | string | `""`    | Image tag.                                               |

### Snow-White Ingress

| Key                         | Type   | Default   | Description                                                       |
| --------------------------- | ------ | --------- | ----------------------------------------------------------------- |
| snowWhite.ingress.className | string | `"nginx"` | Ingress class name.                                               |
| snowWhite.ingress.enabled   | bool   | `true`    | Enable ingress.                                                   |
| snowWhite.ingress.host      | string | `""`      | .Ingress hostname. Must be specified when `ingress.enabled=true`. |
| snowWhite.ingress.tls       | bool   | `true`    | Enable TLS.                                                       |

### Snow-White Kafka

| Key                                             | Type   | Default                                                                    | Description                        |
| ----------------------------------------------- | ------ | -------------------------------------------------------------------------- | ---------------------------------- |
| snowWhite.kafka.brokers                         | list   | The chart will connect to the provisioned Kafka StatefulSet automatically. | Kafka bootstrap server list.       |
| snowWhite.kafka.calculationRequestTopic         | string | `"snow-white-calculation-request"`                                         | Calculation request topic.         |
| snowWhite.kafka.initTopics                      | string | `"true"`                                                                   | Initialize Kafka topics.           |
| snowWhite.kafka.openapiCalculationResponseTopic | string | `"snow-white-openapi-calculation-response"`                                | OpenAPI calculation response topic |

### Availability

| Key                        | Type   | Default            | Description                                                                              |
| -------------------------- | ------ | ------------------ | ---------------------------------------------------------------------------------------- |
| snowWhite.mode             | string | `"high-available"` | Deployment mode of Snow-White. Allowed values: `minimal`, `high-available`, `autoscale`. |
| snowWhite.rollout.maxSurge | string | `"25%"`            | Max pods above desired during rolling update.                                            |

### Snow-White OpenAPI Coverage Stream

| Key                                               | Type   | Default                                                                       | Description                                              |
| ------------------------------------------------- | ------ | ----------------------------------------------------------------------------- | -------------------------------------------------------- |
| snowWhite.openapiCoverageStream.additionalEnvs    | list   | `[]`                                                                          | Additional environment variables forwarded to container. |
| snowWhite.openapiCoverageStream.image.tag         | string | `""`                                                                          | Image tag.                                               |
| snowWhite.openapiCoverageStream.influxdb.bucket   | string | `"raw-data"`                                                                  | InfluxDB bucket.                                         |
| snowWhite.openapiCoverageStream.influxdb.endpoint | string | The chart will connect to the provisioned InfluxDB StatefulSet automatically. | InfluxDB endpoint.                                       |
| snowWhite.openapiCoverageStream.influxdb.org      | string | `"snow-white"`                                                                | InfluxDB organization.                                   |
| snowWhite.openapiCoverageStream.influxdb.token    | string | The chart will connect to the provisioned InfluxDB StatefulSet automatically. | InfluxDB token.                                          |
| snowWhite.openapiCoverageStream.replicas          | int    | `1`                                                                           | Number of replicas to deploy.                            |

### Snow-White OTel Event Filter Stream

| Key                                            | Type   | Default | Description                                              |
| ---------------------------------------------- | ------ | ------- | -------------------------------------------------------- |
| snowWhite.otelEventFilterStream.additionalEnvs | list   | `[]`    | Additional environment variables forwarded to container. |
| snowWhite.otelEventFilterStream.image.tag      | string | `""`    | Image tag.                                               |
| snowWhite.otelEventFilterStream.replicas       | int    | `1`     | Number of replicas to deploy.                            |

### Snow-White Quality Gate API

| Key                                     | Type   | Default | Description                                              |
| --------------------------------------- | ------ | ------- | -------------------------------------------------------- |
| snowWhite.qualityGateApi.additionalEnvs | list   | `[]`    | Additional environment variables forwarded to container. |
| snowWhite.qualityGateApi.image.tag      | string | `""`    | Image tag.                                               |

### Advanced Configuration

| Key                            | Type | Default | Description                          |
| ------------------------------ | ---- | ------- | ------------------------------------ |
| snowWhite.revisionHistoryLimit | int  | `3`     | Number of old ReplicaSets to retain. |

### Other Values

| Key                                                            | Type   | Default                                    | Description |
| -------------------------------------------------------------- | ------ | ------------------------------------------ | ----------- |
| influxdb.config.http.auth-enabled                              | bool   | `true`                                     |             |
| influxdb.setDefaultUser.enabled                                | bool   | `true`                                     |             |
| postgresql.architecture                                        | string | `"standalone"`                             |             |
| postgresql.primary.extraEnvVars[0].name                        | string | `"API_INDEX_DATASOURCE_PASSWORD"`          |             |
| postgresql.primary.extraEnvVars[0].valueFrom.secretKeyRef.key  | string | `"api-index-password"`                     |             |
| postgresql.primary.extraEnvVars[0].valueFrom.secretKeyRef.name | string | `"snow-white-postgresql-credentials"`      |             |
| postgresql.primary.extraEnvVars[1].name                        | string | `"API_INDEX_FLYWAY_PASSWORD"`              |             |
| postgresql.primary.extraEnvVars[1].valueFrom.secretKeyRef.key  | string | `"api-index-flyway-password"`              |             |
| postgresql.primary.extraEnvVars[1].valueFrom.secretKeyRef.name | string | `"snow-white-postgresql-credentials"`      |             |
| postgresql.primary.extraEnvVars[2].name                        | string | `"REPORT_COORDINATOR_DATASOURCE_PASSWORD"` |             |
| postgresql.primary.extraEnvVars[2].valueFrom.secretKeyRef.key  | string | `"report-coord-password"`                  |             |
| postgresql.primary.extraEnvVars[2].valueFrom.secretKeyRef.name | string | `"snow-white-postgresql-credentials"`      |             |
| postgresql.primary.extraEnvVars[3].name                        | string | `"REPORT_COORDINATOR_FLYWAY_PASSWORD"`     |             |
| postgresql.primary.extraEnvVars[3].valueFrom.secretKeyRef.key  | string | `"report-coord-flyway-password"`           |             |
| postgresql.primary.extraEnvVars[3].valueFrom.secretKeyRef.name | string | `"snow-white-postgresql-credentials"`      |             |
| postgresql.primary.extraEnvVars[4].name                        | string | `"QUALITY_GATE_DATASOURCE_PASSWORD"`       |             |
| postgresql.primary.extraEnvVars[4].valueFrom.secretKeyRef.key  | string | `"quality-gate-password"`                  |             |
| postgresql.primary.extraEnvVars[4].valueFrom.secretKeyRef.name | string | `"snow-white-postgresql-credentials"`      |             |
| postgresql.primary.extraEnvVars[5].name                        | string | `"QUALITY_GATE_FLYWAY_PASSWORD"`           |             |
| postgresql.primary.extraEnvVars[5].valueFrom.secretKeyRef.key  | string | `"quality-gate-flyway-password"`           |             |
| postgresql.primary.extraEnvVars[5].valueFrom.secretKeyRef.name | string | `"snow-white-postgresql-credentials"`      |             |
