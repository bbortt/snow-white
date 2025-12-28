# Helm Instructions

## Installing Helm Repositories

In order to fetch third-party Helm charts, repositories must be setup:

```shell
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add influxdata https://helm.influxdata.com/
```

## Generate Documentation

Snow-White makes use of [`helm-docs`](https://github.com/norwoodj/helm-docs) for automatic markdown documentation of the Helm chart.
Go fetch the latest binary from their GitHub page.
Then generate a new `README.md` after editing `values.yaml`:

```shell
helm-docs
```

## Local Installation

If you planned on locally installing `snow-white`, you'll have to [build all native services](../DEVELOPMENT.md#native-builds) first.
After successfully doing so, get the Helm chart up and running:

```shell
helm upgrade --install \
  test-release \
  helm/charts/snow-white \
  --set snowWhite.mode=minimal \
  --set snowWhite.ingress.host=localhost \
  --set snowWhite.ingress.tls=false \
  --set snowWhite.apiIndex.image.tag=latest \
  --set snowWhite.apiGateway.image.tag=latest \
  --set snowWhite.openapiCoverageStream.image.tag=latest \
  --set snowWhite.qualityGateApi.image.tag=latest \
  --set snowWhite.reportCoordinatorApi.image.tag=latest
```
