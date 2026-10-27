# Developer Instructions

The MCP API is a façade over three existing REST surfaces: it derives nothing, and every tool
answers with the same value the corresponding endpoint serves.
It needs those three services
reachable, and refuses to start without all three base URLs:

| Property                                | Served by                | Host port in `dev/docker-compose.yaml` |
| --------------------------------------- | ------------------------ | -------------------------------------- |
| `snow.white.mcp.api.api-index.base-url` | `api-index-api`          | `8085`                                 |
| `snow.white.mcp.api.criteria.base-url`  | `quality-gate-api`       | `8081`                                 |
| `snow.white.mcp.api.report.base-url`    | `report-coordinator-api` | `8084`                                 |

## Building the Application

```shell
./mvnw -Pprod -pl :mcp-api -am install
```

The `-am` is not optional: this module compiles against `commons`, whose traceables are generated
into the root module.

The build produces `target/mcp-api-<version>-executable.jar`, which is what both run modes below
launch.

## Running it

One artifact serves two transports, and never both at once.
Which one it is comes from
configuration.

### Over standard streams, for a local agent client

This is the mode an MCP client starts itself.
Nothing but protocol frames may reach stdout, so the
`stdio` profile moves logging to stderr, drops the banner and starts no web server.

```shell
java -jar microservices/mcp-api/target/mcp-api-*-executable.jar \
  --spring.profiles.active=stdio \
  --snow.white.mcp.api.api-index.base-url=http://localhost:8085 \
  --snow.white.mcp.api.criteria.base-url=http://localhost:8081 \
  --snow.white.mcp.api.report.base-url=http://localhost:8084
```

Registered with a client that reads the usual `mcpServers` configuration, the same thing looks
like:

```json
{
  "mcpServers": {
    "snow-white": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-api-executable.jar",
        "--spring.profiles.active=stdio",
        "--snow.white.mcp.api.api-index.base-url=http://localhost:8085",
        "--snow.white.mcp.api.criteria.base-url=http://localhost:8081",
        "--snow.white.mcp.api.report.base-url=http://localhost:8084"
      ]
    }
  }
}
```

A stdio process has no actuator surface, because it has no web server to carry one.
That is
accepted: it fails by exiting, in front of the one client that started it.

### Over streamable HTTP, for a hosted Snow-White

The shipped default.
Drop the profile and the server listens on `/mcp`:

```shell
java -jar microservices/mcp-api/target/mcp-api-*-executable.jar \
  --snow.white.mcp.api.api-index.base-url=http://localhost:8085 \
  --snow.white.mcp.api.criteria.base-url=http://localhost:8081 \
  --snow.white.mcp.api.report.base-url=http://localhost:8084
```

Leaving `spring.ai.mcp.server.stdio=true` set in this mode is refused at startup rather than
tolerated — the server would otherwise answer its health probe while serving no tools at all.

## Running the tests

```shell
./mvnw -pl :mcp-api -am -Dsurefire.failIfNoSpecifiedTests=false verify
```

`verify`, not `test`: the integration suite includes one test that launches the repackaged jar as
its own process, which only exists after the `package` phase.

## What this module deliberately does not have

No image, chart or compose entry yet.
The deployment increment settles those, along with which
probes target the HTTP mode's management port; until then the stdio mode is the one that runs,
and it runs from the jar.
