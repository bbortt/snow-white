#!/usr/bin/env sh

#
# Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
# Licensed under the Polyform Small Business License 1.0.0
# See LICENSE file for full details.
#

image_tag="$1"
registry="${2:-ghcr.io}"
additional_args="$3"

scripts_dir="$(dirname "$(realpath "$0")")"
root_dir="$scripts_dir/../.."

microservices="api-gateway api-sync-job openapi-coverage-stream"

for service in $microservices; do
  echo "Building $service..."
  docker build \
    -f "$root_dir/microservices/$service/Dockerfile" \
    -t "$registry/bbortt/snow-white/$service:$image_tag" \
    --build-arg BUILD_DATE="$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
    --build-arg PROJECT_VERSION="$image_tag" \
    $additional_args \
    "$root_dir/microservices/$service"
done
