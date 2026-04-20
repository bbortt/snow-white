---
title: 'CLI Reference'
permalink: /cli/
toc: true
toc_sticky: true
---

The Snow-White CLI (`snow-white`) is the integration point between your CI pipeline and the Snow-White quality gate engine.
It ships as a self-contained binary for Linux, macOS, and Windows — no Node.js runtime required.

---

## Installation

### Download from GitHub Releases

Pre-built binaries are available on the [GitHub Releases page](https://github.com/bbortt/snow-white/releases).

Download the binary for your platform, make it executable, and place it on your `PATH`:

```shell
# Linux (x64)
curl -Lo snow-white https://github.com/bbortt/snow-white/releases/download/v1.0.0-alpha.16/snow-white-linux-x64
chmod +x snow-white
sudo mv snow-white /usr/local/bin/

# macOS (Apple Silicon)
curl -Lo snow-white https://github.com/bbortt/snow-white/releases/download/v1.0.0-alpha.16/snow-white-macos-arm64
chmod +x snow-white
sudo mv snow-white /usr/local/bin/
```

Available binaries per release:

| File                         | Platform    |
| ---------------------------- | ----------- |
| `snow-white-linux-arm64`     | Linux ARM64 |
| `snow-white-linux-x64`       | Linux x64   |
| `snow-white-macos-arm64`     | macOS ARM64 |
| `snow-white-macos-x64`       | macOS x64   |
| `snow-white-windows-x64.exe` | Windows x64 |

---

### OCI Image

The CLI is also distributed as an OCI image on the GitHub Container Registry:

```text
ghcr.io/bbortt/snow-white/toolkit/cli:1.0.0-alpha.16
```

The image is built `FROM scratch` — it contains only the binaries and cannot be run directly.
Use it as a build-time source in one of the two ways below.

#### Copy into your own image

Use `COPY --from` to embed the CLI binary into a custom Docker image:

```dockerfile
FROM ghcr.io/bbortt/snow-white/toolkit/cli:1.0.0-alpha.16 AS snow-white-cli

FROM ubuntu:24.04
COPY --from=snow-white-cli /snow-white-linux-x64 /usr/local/bin/snow-white
RUN chmod +x /usr/local/bin/snow-white
```

Choose the binary matching your target platform (`snow-white-linux-x64`, `snow-white-linux-arm64`, etc.).

#### Extract the binary to the local file system

To pull a binary out of the image without building a new image, use `docker create` and `docker cp`:

```shell
# Extract the binary directly using BuildKit (no container needed)
docker buildx build --output type=local,dest=out - <<'EOF'
FROM ghcr.io/bbortt/snow-white/toolkit/cli:1.0.0-alpha.16 as source
FROM scratch
COPY --from=source /snow-white-linux-x64 /snow-white
EOF

chmod +x out/snow-white
mv out/snow-white ./snow-white
```

---

## Commands

### `info`

Prints platform and runtime information.
Useful for verifying the binary works in your environment.

```shell
snow-white info
```

---

### `calculate`

Triggers a Quality-Gate calculation against a running Snow-White instance.
The CLI polls for the result and exits non-zero if the gate fails.

```shell
snow-white calculate [options]
```

**Options:**

| Option                           | Description                                                                      |
| -------------------------------- | -------------------------------------------------------------------------------- |
| `--config-file <path>`           | Path to a YAML or JSON config file (can contain all other options)               |
| `--url <baseUrl>`                | Base URL of the Snow-White instance (overrides config file)                      |
| `--quality-gate <name>`          | Quality-Gate configuration name                                                  |
| `--service-name <name>`          | Name of the service                                                              |
| `--api-name <name>`              | Name of the API                                                                  |
| `--api-version <version>`        | API version                                                                      |
| `--api-specs <pattern>`          | Glob pattern selecting which OpenAPI spec files to read identifiers from         |
| `--api-name-path <jsonPath>`     | JSON path to the API name field in the spec (default: `info.title`)              |
| `--api-version-path <jsonPath>`  | JSON path to the API version field in the spec (default: `info.version`)         |
| `--service-name-path <jsonPath>` | JSON path to the service name field in the spec (default: `info.x-service-name`) |
| `--lookback-window <window>`     | Time window for the calculation, e.g. `1h`, `24h`, `7d`                          |
| `--filter <key=value>`           | Attribute filter for telemetry data (repeatable)                                 |
| `--async`                        | Fire-and-forget: submit the calculation without polling for the result           |

**Config file example (`snow-white.json`):**

```json
{
  "url": "http://<snow-white-host>",
  "qualityGate": "basic-coverage",
  "apiInformation": [
    {
      "serviceName": "my-service",
      "apiName": "my-api",
      "apiVersion": "1.0.0"
    }
  ]
}
```

**Usage examples:**

```shell
# Using a config file
snow-white calculate --config-file snow-white.json

# Fully inline
snow-white calculate \
  --url http://snow-white:8080 \
  --quality-gate basic-coverage \
  --service-name my-service \
  --api-name my-api \
  --api-version 1.0.0

# Derive identifiers from spec files
snow-white calculate \
  --config-file snow-white.json \
  --api-specs "services/**/openapi.yaml"
```

**Exit codes:**

| Exit Code | Meaning                                  |
| --------- | ---------------------------------------- |
| `0`       | Quality gate passed                      |
| non-zero  | Quality gate failed or an error occurred |

---

### `upload-prereleases`

Uploads one or more OpenAPI specifications from the local file system as prereleases.
Intended to be called at the start of a pipeline, before integration tests run, so Snow-White can correlate traces with the spec under review.
Uploaded prereleases are temporary and are cleaned up asynchronously.

```shell
snow-white upload-prereleases [options]
```

**Options:**

| Option                           | Description                                                                           |
| -------------------------------- | ------------------------------------------------------------------------------------- |
| `--api-specs <pattern>`          | Glob pattern selecting which spec files to upload (e.g. `"services/**/openapi.yaml"`) |
| `--url <baseUrl>`                | Base URL of the Snow-White instance (overrides config file)                           |
| `--config-file <path>`           | Path to a config file (used to resolve `--url` if not provided directly)              |
| `--api-name-path <jsonPath>`     | JSON path to the API name field in the spec                                           |
| `--api-version-path <jsonPath>`  | JSON path to the API version field in the spec                                        |
| `--service-name-path <jsonPath>` | JSON path to the service name field in the spec (maps to `info.x-service-name`)       |
| `--ignore-existing`              | Skip specs that are already indexed                                                   |

**Usage example:**

```shell
snow-white upload-prereleases \
  --url http://snow-white:8080 \
  --api-specs "services/**/openapi.yaml"
```

---

## CI Pipeline Integration

### GitHub Actions

```yaml
- name: Upload API specs as prereleases
  run: |
    snow-white upload-prereleases \
      --config-file snow-white.json \
      --api-specs "services/**/openapi.yaml"

# ... run your integration tests here ...

- name: Calculate API coverage
  run: snow-white calculate --config-file snow-white.json
```

Using the OCI image to install the binary in a pipeline:

```yaml
- name: Install snow-white CLI
  run: |
    docker buildx build --output type=local,dest=out - <<'EOF'
    FROM ghcr.io/bbortt/snow-white/toolkit/cli:1.0.0-alpha.16 as source
    FROM scratch
    COPY --from=source /snow-white-linux-x64 /snow-white
    EOF

    chmod +x out/snow-white
    mv out/snow-white /usr/local/bin/snow-white
```

For the full workflow context see [Pipeline Workflows](/workflows/).
