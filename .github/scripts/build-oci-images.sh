#!/usr/bin/env sh

image_tag="$1"

scripts_dir="$(dirname "$(realpath "$0")")"
root_dir="$scripts_dir/../.."

microservices="api-gateway api-sync-job openapi-coverage-service"

for service in $microservices; do
  echo "Building $service..."
  docker build \
    -f "$root_dir/microservices/$service/Dockerfile" \
    -t "snow-white/$service:$image_tag" \
    "$root_dir/microservices/$service"
done
