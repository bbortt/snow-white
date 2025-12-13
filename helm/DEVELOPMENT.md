# Helm Instructions

## Installing Helm Repositories

In order to fetch third-party Helm charts, repositories must be setup:

```shell
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add influxdata https://helm.influxdata.com/
```

## Local Deployment

If you planned on locally installing `snow-white`, you'll have to build all native services first:

```shell
helm upgrade --install \
  test-release \
  helm/charts/snow-white \
  --set snowWhite.ingress.host=localhost \
  --set snowWhite.ingress.tls=false \
  --set snowWhite.apiGateway.image.tag=latest \
  --set snowWhite.qualityGateApi.image.tag=latest \
  --set snowWhite.reportCoordinatorApi.image.tag=latest
```
