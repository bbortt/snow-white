# Example Application

To create a Trace, run:

```shell
curl -ijv http://localhost:8080/ping?message=pong
```

## Naming the test that produced a Trace

This application is instrumented by the OpenTelemetry Java agent and configured with

```shell
OTEL_JAVA_EXPERIMENTAL_SPAN_ATTRIBUTES_COPY_FROM_BAGGAGE_INCLUDE=test.case.name
```

(see the [`Dockerfile`](./Dockerfile)), so a caller that sends a `test.case.name` baggage entry gets it copied onto the server span as an attribute:

```shell
curl -ijv -H 'baggage: test.case.name=org.example.PingPongIT.shouldPong' \
  http://localhost:8080/ping?message=pong
```

No application code takes part in that copy - it is agent configuration only.
Refer to the [`test.case.name` semantic convention](../../semantic-convention/test.md) for what the value means and how Snow-White reads it.

The application tests under [`src/apptest`](./src/apptest) assert exactly this, end to end: a request carrying the header produces an exported span with the attribute, and a request without it produces one without.
They drive the built image against a collector and a WireMock standing in for a telemetry backend, so building that image and starting that stack is part of running them.
From the repository root:

```shell
# once - the apptest stack joins an external network, the same one CI creates
docker network create github_actions

./mvnw -Pprod -DskipTests -pl :example-spring-boot -am clean install
docker build -t ghcr.io/bbortt/snow-white/example-spring-boot:local \
  examples/example-spring-boot

docker compose \
  -f examples/example-spring-boot/src/apptest/resources/docker-compose-apptest.yaml \
  up -d --wait

./mvnw -Papptest -Ddocker.network=github_actions -Dimage.tag=local \
  -pl :example-spring-boot verify
```

The tag is arbitrary as long as the image `docker build` produces is the one `image.tag` names; CI uses the commit SHA for both.
