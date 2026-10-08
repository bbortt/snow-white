#!/usr/bin/env bash

#
# Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
# Licensed under the Polyform Small Business License 1.0.0
# See LICENSE file for full details.
#

set -euo pipefail

workspace="$(pwd)"

log() {
  printf '\n==> %s\n' "$1"
}

# Named volumes are created root-owned when their mount point does not exist in
# the image. Without this, Maven, pnpm and bun cannot write their caches.
log 'Taking ownership of the cache volumes'
for cache_dir in "${HOME}/.m2" "${HOME}/.local/share/pnpm" "${HOME}/.bun"; do
  sudo mkdir -p "${cache_dir}"
  sudo chown -R "$(id -u):$(id -g)" "${cache_dir}"
done

# The workspace is bind-mounted from the host, so its owner is the host user.
git config --global --add safe.directory "${workspace}"
git config --global --add safe.directory "${workspace}/opentelemetry-proto"

# `otel-event-filter-stream` compiles the OpenTelemetry protobuf definitions and
# does not build without them. The `backstage` submodule is not part of the
# Maven reactor and stays uninitialized - it is large, and nothing needs it.
log 'Initializing the opentelemetry-proto submodule'
git submodule update --init --depth 1 opentelemetry-proto

log 'Installing pnpm'
if command -v corepack >/dev/null 2>&1; then
  # `corepack install` reads the `packageManager` pin from package.json.
  corepack enable
  corepack install
else
  pnpm_version="$(node -p "require('./package.json').packageManager.split('@')[1]")"
  npm install --global "pnpm@${pnpm_version}"
fi

log 'Installing Node dependencies'
pnpm install --frozen-lockfile

# The `helm` module renders the chart in its tests, which needs the chart
# dependencies present. Non-fatal: a failure here costs `./mvnw verify` on that
# one module, not the container.
log 'Fetching Helm chart dependencies'
if ! (
  cd "${workspace}/helm/charts/snow-white"
  helm repo add --force-update bitnami https://charts.bitnami.com/bitnami
  helm repo add --force-update influxdata https://helm.influxdata.com
  helm repo add --force-update kafbat-ui https://kafbat.github.io/helm-charts
  helm dependency build
); then
  printf '\n!!! Helm chart dependencies could not be fetched. Run `helm dependency build` in helm/charts/snow-white before building that module.\n'
fi

log 'Ready'
cat <<'EOF'
  ./mvnw verify -T 1C                                 build and test everything
  docker compose -f dev/docker-compose.yaml up -d     start the dev environment

See DEVELOPMENT.md for the rest.
EOF
