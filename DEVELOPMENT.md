# Developer Setup Guide

Welcome, and thank you for considering to contribute to **Snow-White**!
This guide walks you through setting up your development environment and building the services.
If you run into any issues, please open an issue or contact the maintainers.

## Development Container

`.devcontainer/` holds a [development container](https://containers.dev).
It is the recommended way to work on this repository - for humans, and for coding agents.

### Why a container

Building Snow-White executes a great deal of code you do not have reviewed: Maven plugins, pnpm and Bun lifecycle scripts, and every image the Testcontainers-based tests pull.
Run natively, all of it runs as you, with your SSH keys, your cloud credentials and your browser profile within reach.
A careless - or hostile - dependency needs no exploit to reach them, a `postinstall` script is enough.

The container does not make any of that code trustworthy.
It makes its blast radius the workspace and the caches that belong to it.

Be clear about where the boundary actually is.
This container runs `--privileged`, because it runs a Docker daemon of its own, and privileged plus a root-owned host daemon means host root.
The boundary that holds is therefore the virtual machine on macOS and Windows, and rootless Docker on Linux.
Both are set up below.

### What is inside

| Tool               | Version                                                             |
| ------------------ | ------------------------------------------------------------------- |
| Temurin JDK        | 25, matching CI                                                     |
| Node.js            | 24 (LTS)                                                            |
| pnpm               | the `packageManager` pin from `package.json`, installed by corepack |
| Bun                | latest                                                              |
| Docker and Compose | a daemon inside the container, not the host's socket                |
| Helm and kubectl   | latest                                                              |
| GitHub CLI         | latest                                                              |

Maven is deliberately absent - use `./mvnw`, as everywhere else.

Creating the container also initializes the `opentelemetry-proto` submodule, installs the Node dependencies and fetches the Helm chart dependencies, so that `./mvnw verify` works without further setup.
The Maven repository, the pnpm store and the Bun cache live on named volumes and survive a rebuild.

### Opening it

- **VS Code:** install the Dev Containers extension, then _Reopen in Container_.
- **IntelliJ IDEA:** _Remote Development_ → _Dev Containers_ → _New Dev Container_, pointed at `.devcontainer/devcontainer.json`.
- **No editor:** drive it from the CLI.

  ```shell
  npx @devcontainers/cli up --workspace-folder .
  npx @devcontainers/cli exec --workspace-folder . ./mvnw verify -T 1C
  ```

### Windows (WSL 2)

Use Docker Desktop with WSL integration enabled, or install Docker Engine inside the distribution itself.
Either way the daemon runs as root inside a virtual machine, so the container's default `vscode` user needs no adjustment.

Clone into the Linux filesystem (`~/snow-white`), never into `/mnt/c`: a Windows-side bind mount cannot represent Unix permissions and is slow enough to dominate the build.
Open the folder from inside WSL (`code .`), then reopen in the container.

The full reactor with Testcontainers wants memory.
Give the VM at least 8 GB in `%UserProfile%\.wslconfig`:

```ini
[wsl2]
memory=8GB
```

### macOS (Lima)

Docker Desktop and OrbStack need no extra steps.
For Lima, use the **rootful** template.
Lima's default `docker` template runs the daemon rootless, which maps your VM user to root inside the container and leaves the `vscode` user unable to write the workspace - the same trap as rootless Docker on Linux, described below.

Lima also mounts your home directory read-only by default, so make it writable:

```shell
brew install lima docker docker-compose
limactl start --set '.mounts[0].writable = true' template://docker-rootful
docker context create lima-docker-rootful --docker "host=unix://${HOME}/.lima/docker-rootful/sock/docker.sock"
docker context use lima-docker-rootful
```

### Linux

There is no virtual machine between the container and your kernel, so the isolation is only as good as the daemon you point it at.

With the distribution's default **rootful** Docker, the container works as shipped: Dev Containers rewrites the `vscode` user's UID to match yours, and the bind-mounted workspace stays writable.
A privileged container is then root on your machine, which is precisely what the container was meant to prevent.

Prefer **rootless** Docker (or Podman).
Your user then maps to root _inside_ the container, so the `vscode` user can no longer write the workspace, and a privileged container is confined to your unprivileged user.
Run as root instead - under rootless Docker, that root is you:

```jsonc
// .devcontainer/devcontainer.json, locally - do not commit
"remoteUser": "root",
"updateRemoteUserUID": false,
```

The cache volumes are mounted under `/home/vscode`, so point them at `/root` in the same edit, or accept that they are unused.

### Coding agents

Agents should use the container whenever one can be started, for the reason above turned up a notch: an agent runs commands nobody typed and installs dependencies nobody chose.

```shell
npx @devcontainers/cli up --workspace-folder .
npx @devcontainers/cli exec --workspace-folder . <command>
```

Where that is impossible - no container runtime, or a session already confined to its own sandbox - build on the host, and say so in the summary rather than leaving it implied.

### Troubleshooting

**`Cannot connect to the Docker daemon` inside the container.**
The Docker-in-Docker feature prefers the legacy iptables backend, which has no `nat` table on kernels built for nftables only.
`.devcontainer/post-start.sh` detects this and switches backends, so the first thing to check is its output in the _Dev Containers_ log.
If the daemon still refuses to start, the host is most likely blocking `--privileged`.

**`Permission denied` on the workspace, or `could not lock config file .git/config`.**
The container user does not own the bind mount - see [Linux](#linux) above.

**Helm chart dependencies missing.**
`post-create.sh` fetches them, but tolerates failure so that a network hiccup cannot break container creation.
Re-run it by hand:

```shell
helm dependency build helm/charts/snow-white
```

## Quick Start

The steps below describe a native setup.
Inside the development container, the prerequisites are already installed - start at step 2.

### 1. Prerequisites

- Java 25 installed
- Node.js 24 installed
- Docker or Podman (with Compose) installed

### 2. Launch the Development Environment

Start all required services using Docker/Podman Compose:

```shell
docker compose -f dev/docker-compose.yaml up -d
```

This includes InfluxDB, Kafka, OTel Collector, PostgreSQL, and supporting UI tools.
For more on which services are running and their ports, see [Mapped Ports](./pages/_pages/architecture.md#mapped-ports).

### 3. Configure InfluxDB Access

You'll need a **Read/Write token** for the raw-data bucket in InfluxDB.

1. Visit the InfluxDB UI (port 8086)
2. The login is `snow-white:snow-white`
3. Create a token with both `read` and `write` into the `raw-data` bucket
4. Add the token to your `dev/.env` file:

```ini
INFLUXDB_TOKEN=[YOUR_TOKEN_GOES_HERE]
```

![InfluxDB Token](dev/influxdb-token.png)

Restart the environment for changes to take effect:

```shell
docker compose -f dev/docker-compose.yaml down && docker compose -f dev/docker-compose.yaml up -d
```

### 4. Generate some Tracing Data

You can use the provided example application to generate some tracing data.

```shell
curl -ijv http://localhost:8080/ping?message=pong
```

### 5. Run the Coverage Calculation

Use the following query to run the coverage calculation against the generated data.

```shell
node toolkit/cli/target/cli/index.js calculate --configFile dev/snow-white.json
```

## Previewing the Docs Site

The documentation site lives in `pages/` and is built with [Jekyll](https://jekyllrb.com) using the [just-the-docs](https://just-the-docs.com) theme.

**Prerequisites:** Ruby (with Devkit) and Bundler - on Windows use [RubyInstaller](https://rubyinstaller.org) (`Ruby+Devkit` variant), then run `gem install bundler`.

```shell
cd pages
bundle install
bundle exec jekyll serve --livereload
```

The site is served at <http://localhost:4000/snow-white/> and reloads automatically on file changes.

## Running Tests and Code Quality

Run all unit/integration tests and aggregate coverage:

```shell
./mvnw verify -T 1C
```

To run a [SonarQube](https://www.sonarsource.com) analysis:

1. Start Sonar:

   ```shell
   docker compose -f dev/sonar.yaml up -d
   ```

2. Create a new Sonar project (`snow-white`) and token.

   The initial login to <http://localhost:9000> can be done with `admin:admin`.
   The password must be changed at first login.

   Enter into SonarQube and add a new project called `snow-white`.
   Choose manual setup with a local build environment.
   This will lead you up to the token generation.
   Create a token with a name of your choice, but select "No expiration date".

3. Run the analysis:

   ```shell
   ./mvnw jacoco:report-aggregate sonar:sonar -Dsonar.login=${SONAR_TOKEN}
   ```

### Application Tests

Application tests run tests against the fully built and running application.
To run application tests within GitHub Actions, add the `include:apptests` label in your pull request.

## Building and Running Services

Use the following steps for rapid local development:

1. Build all modules:

   ```shell
   ./mvnw package -b smart
   ```

2. Start the Docker environment (if not already running):

   ```shell
   docker compose -f dev/docker-compose.yaml up -d
   ```

3. Stop the microservice you want to develop and run it manually:

   ```shell
   ./mvnw spring-boot:run -pl :<microservice-name>
   ```

### JDK Builds

These microservices run in a traditional JVM image:

- `api-gateway`
- `api-sync-job`
- `openapi-coverage-stream`

Build an image using:

```shell
./mvnw -am -pl :<maven-module> -b smart install

docker build \
    -f "microservices/<maven-module>/Dockerfile" \
    -t "ghcr.io/bbortt/snow-white/<maven-module>:latest" \
    --build-arg BUILD_DATE="$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
    --build-arg PROJECT_VERSION="latest" \
    "microservices/<maven-module>"
```

Alternatively, install all packages and microservices first, then simply run the following script:

```shell
./mvnw -b smart -Pprod install
.github/scripts/build-oci-images.sh latest
```

### Native Builds

These microservices support native image builds:

- `api-index-api`
- `otel-event-filter-stream`
- `quality-gate-api`
- `report-coordinator-api`

Build an image using:

```shell
./mvnw -am -pl :<maven-module> -b smart install
./mvnw -DskipTests -Pnative -pl :<maven-module> spring-boot:build-image
```

Build all native microservices:

```shell
./mvnw -b smart -Pprod install
./mvnw -DskipTests -Pnative -b smart -pl :api-index-api,:otel-event-filter-stream,:report-coordinator-api,:quality-gate-api spring-boot:build-image
```

For development, override `-Dimage.tag=latest` to build a "latest" image for usage with [Docker/Podman Compose](#quick-start).
Podman is supported using the `podman` profile as well.

## Maven Proxy Setup

If you're behind a corporate proxy, use the following snippet in `.mvn/settings.xml`:

```xml
<settings>
  <mirrors>
    <mirror>
      <id>central-mirror</id>
      <name>...</name>
      <url>...</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
    <mirror>
      <id>confluent-mirror</id>
      <name>...</name>
      <url>...</url>
      <mirrorOf>confluent</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

Make sure to add this to `.mvn/settings.xml` (this file is gitignored).
