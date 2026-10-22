# A stream service runs an HTTP server for its management surface only, never gaining a business entry point

<!-- markdownlint-disable MD036 -->

**Title**
A stream service runs an HTTP server for its management surface only, never gaining a business
entry point

**Lens**: ARCH

**Status**: active

**Description**
`openapi-coverage-stream` runs an embedded HTTP server bound to the management port (8090),
serving the actuator surface and nothing else.
The main server port is not used: the service exposes no business HTTP endpoint, and acquiring a
servlet container does not make it an HTTP-driven service.
It remains driven entirely by its Kafka Streams topology
([SW-008](SW-008-kafka-as-async-calculation-driver.md)).

Concretely, the management server is configured on its own port, and the exposed endpoint set is
limited to the health surface a deployment needs to probe.
The service is not registered as a gateway route, so its actuator surface is reachable only by
something with direct network access to the pod.

This holds for the stream services specifically.
It is the reason a web dependency appears in a module that serves no API, and it is a deliberate
narrowing rather than a step toward giving the stream an HTTP interface.

**Rationale**
A health verdict that nothing can read is not observability.
Kubernetes probes an HTTP endpoint, and the four services that already carry actuator
(`api-gateway`, `api-index-api`, `quality-gate-api`, `report-coordinator-api`) all serve it on a
management port on 8090 with startup, readiness, and liveness probes against it.
Without a web server this service has no actuator surface at all: `JmxAutoConfiguration` is gated
on `spring.jmx.enabled`, which Spring Boot 4.1 leaves off by default, so there is not even a JMX
fallback to read the verdict from.
Adding the starter without a server would therefore satisfy the letter of a health check while
leaving the gap [NF-001](NF-001-operational-observability.md) describes entirely untouched.

Confining the server to the management port keeps that from becoming an architectural drift.
[SW-008](SW-008-kafka-as-async-calculation-driver.md) records that this service has no synchronous
entry point, and that remains true of its business behavior: a calculation is still requested only
by a Kafka record, never by an HTTP call.
Separating the ports makes the distinction enforceable rather than conventional — there is no main
port serving anything, so a business endpoint cannot be added here by accident and inherit a route
that was opened for probes.

Keeping the exposed endpoint set to the health surface follows the same posture
[CON-006](CON-006-actuator-endpoints-deny-by-default.md) takes at the gateway: the operational
detail actuator can expose is legitimate for an operator at the pod, but there is no reason to
enable more of it than the deployment actually consumes.

**Verification Description**
An integration test asserts the actuator health endpoint answers on the management port.
A test asserts the service serves nothing on the main application port, so the absence of a
business surface is checked rather than assumed.
Whether a deployment's probes target that port is verified by the follow-up increment that adds
them, not here — this spec fixes only what the application exposes.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality this decision makes
  reachable

**Related**

- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the asynchronous-only entry point this
  decision preserves
- [SW-045](SW-045-stream-state-decides-the-health-verdict.md) — the verdict served on this
  surface
- [CON-006](CON-006-actuator-endpoints-deny-by-default.md) — the gateway-side actuator rule whose
  posture this mirrors at the pod
