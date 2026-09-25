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
Run them with

```shell
./mvnw -pl :example-spring-boot -am -P apptest verify
```
