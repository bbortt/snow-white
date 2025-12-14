# Installation

## Adding the Helm Repository

[Helm](https://helm.sh) must be installed to use the Snow-White chart.
Please refer to Helm's [documentation](https://helm.sh/docs) to get started.

Once Helm has been set up correctly, add the repo as follows:

    helm repo add snow-white https://bbortt.github.io/snow-white

If you had already added this repo earlier, run `helm repo update` to retrieve the latest versions of the packages.
You can then run `helm search repo snow-white` to see the charts.

To install the snow-white chart:

    helm install my-snow-white snow-white/snow-white --set snowWhite.ingress.host=[PUBLIC_HOST]

> ℹ️ Replace `[PUBLIC_HOST]` by the domain snow-white will be reachable on.

To uninstall the chart:

    helm uninstall my-snow-white

Note that persistent volume claims are not automatically cleaned up:

    kubectl delete pvc --all
