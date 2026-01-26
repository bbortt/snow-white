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
  --set influxdb2.persistence.size=5Gi \
  --set kafka.persistence.size=5Gi \
  --set image.pullPolicy=IfNotPresent \
  --set snowWhite.mode=minimal \
  --set snowWhite.ingress.host=localhost \
  --set snowWhite.ingress.tls=false \
  --set snowWhite.apiIndexApi.image.tag=latest \
  --set snowWhite.apiGateway.image.tag=latest \
  --set snowWhite.openapiCoverageStream.image.tag=latest \
  --set snowWhite.otelEventFilterStream.image.tag=latest \
  --set snowWhite.qualityGateApi.image.tag=latest \
  --set snowWhite.reportCoordinatorApi.image.tag=latest
```

## Kubescore

The GitHub Actions pipeline runs [`kube-score`](https://github.com/zegl/kube-score) against the Helm chart to verify Kubernetes best practices.

To perform the same static template analysis locally, you can run `kube-score` using Docker (or a comparable container runtime).

### Local Execution

```shell
helm template kubescore helm/charts/snow-white/ -f ".github/kubescore_values.yml" | \
    docker run --rm -i zegl/kube-score:latest score \
      --ignore-container-cpu-limit \
      --ignore-test pod-networkpolicy \
      -
```

Replace `<path-to-your-snow-white-repo>` with the absolute path to your local Snow-White repository.
