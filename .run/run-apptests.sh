#!/usr/bin/env bash

#
# Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
# Licensed under the Polyform Small Business License 1.0.0
# See LICENSE file for full details.
#

# Runs the black-box application tests of every microservice, one after another, the way the
# `app-tests` and `native-tests` jobs of `.github/workflows/build.yml` run each matrix entry.
#
# It only *runs* them: the image under test has to exist locally already, and the modules' test
# dependencies (`citrus-junit-jupiter-extension` included) have to be installed into the local
# Maven repository - CI's "Rebuild JAR" and "Build Image" steps.
#
# Usage: .run/run-apptests.sh [IMAGE_TAG] [REGISTRY] [MAVEN_ARGS...]

set -uo pipefail

if [[ -z "${1:-}" ]]; then
  echo "Must provide image tag as first variable: .run/run-apptests.sh [IMAGE_TAG] [REGISTRY] [MAVEN_ARGS...]" 1>&2
  exit 1
fi

image_tag="$1"
registry="${2:-ghcr.io}"
additional_args=("${@:3}")

network=github_actions

root_dir="$(dirname "$(dirname "$(realpath "$0")")")"
cd "$root_dir" || exit 1

if ! docker network inspect "$network" > /dev/null 2>&1; then
  docker network create "$network" > /dev/null || exit 1
fi

failed_services=()

for compose_file in microservices/*/src/apptest/resources/docker-compose-apptest.yaml; do
  service="${compose_file#microservices/}"
  service="${service%%/*}"
  image="${registry}/bbortt/snow-white/${service}:${image_tag}"

  echo
  echo "==> ${service}"

  if ! docker image inspect "$image" > /dev/null 2>&1; then
    echo "!!! Image ${image} does not exist locally - build it first." 1>&2
    failed_services+=("$service")
    continue
  fi

  # kafka.ui is a dev-convenience dashboard, never needed by the tests themselves - skip it to keep resource usage low.
  services=$(COMPOSE_NETWORK_NAME="$network" docker compose -f "$compose_file" config --services | grep -v '^kafka\.ui$')

  # shellcheck disable=SC2086 # one compose service name per word
  if COMPOSE_NETWORK_NAME="$network" docker compose -f "$compose_file" up -d --no-deps --wait --wait-timeout 120 $services; then
    ./mvnw -B verify \
      -b smart \
      -Papptest \
      -Ddocker.network="$network" \
      -Dcontainer.registry="$registry" \
      -Dimage.tag="$image_tag" \
      -pl ":${service}" \
      -am \
      ${additional_args[@]+"${additional_args[@]}"} ||
      failed_services+=("$service")
  else
    failed_services+=("$service")
  fi

  # Every stack publishes the same host ports (9000, 5432, 9092, 4317, ...), so the next service's
  # stack cannot start while this one is still up. CI never sees this: each matrix entry gets a VM.
  COMPOSE_NETWORK_NAME="$network" docker compose -f "$compose_file" down --volumes --remove-orphans
done

for microservice in api-index-api quality-gate-api report-coordinator-api; do
  traces_dir="microservices/${microservice}/target/otel-collector"

  # Absent when the service was skipped, failed before its collector wrote anything, or this
  # script already renamed it on an earlier run.
  if [[ -f "${traces_dir}/traces.json" ]]; then
    mv "${traces_dir}/traces.json" "${traces_dir}/traces-${microservice}.json"
  fi
done

echo
if [[ ${#failed_services[@]} -gt 0 ]]; then
  echo "!!! Application tests failed: ${failed_services[*]}" 1>&2
  exit 1
fi

echo "==> All application tests passed"
