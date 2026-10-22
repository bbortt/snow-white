# The coverage stream reports whether its topology is actually processing

<!-- markdownlint-disable MD036 -->

**Title**
The coverage stream reports whether its topology is actually processing

**Status**: active

**Business Value**
An operator can today see that the `openapi-coverage-stream` pod is alive, but not whether its
Kafka Streams topology is consuming anything.
A dead or errored stream thread leaves the process running and the port quiet, so calculation
requests pile up on the topic with nothing to signal why.
This increment makes that failure visible from outside the process, which is the difference
between a stuck run diagnosed in seconds and one diagnosed by reading logs.

**Problem / Context**
`openapi-coverage-stream` is the only analysis service with no health surface at all.
`api-gateway`, `api-index-api`, `quality-gate-api`, and `report-coordinator-api` each pair
`spring-boot-starter-actuator` with a management port on 8090, and their Helm deployments carry
startup, readiness, and liveness probes against it.
The coverage stream has neither: no actuator dependency, no `ports:` entry, and no probe in
`helm/charts/snow-white/templates/openapi-coverage-stream.yaml`.

Two facts shape the solution.
The service has no web server, because it has no synchronous entry point of its own
([SW-008](../specs/SW-008-kafka-as-async-calculation-driver.md)) — so actuator added on its own
would expose no reachable surface whatsoever, JMX being off by default in Spring Boot 4.1.
And Spring Boot 4.1's `spring-boot-kafka` module ships no Kafka Streams health indicator, so the
state of the topology has to be read and mapped to a verdict deliberately.

The verdict's shape matters more here than on an HTTP service.
A Kafka outage must not lose an in-flight request
([CON-002](../specs/CON-002-tolerate-dependency-outages.md)), and a health check that reported down
during an ordinary rebalance would have Kubernetes restart the pod mid-calculation — turning a
blip into exactly the data loss that constraint forbids.

**Solution Approach**
Add `spring-boot-starter-actuator` to the module, and give it a management-only HTTP server on
port 8090 that serves the actuator surface without granting the service a business HTTP surface
([ARCH-018](../specs/ARCH-018-management-surface-without-a-business-one.md)).

Register a custom health indicator over the `StreamsBuilderFactoryBean` that `@EnableKafkaStreams`
already provides, mapping `KafkaStreams.State` to a verdict and contributing the state and
per-thread detail to the response
([SW-045](../specs/SW-045-stream-state-decides-the-health-verdict.md)).
Both `RUNNING` and `REBALANCING` count as healthy, and the liveness and readiness groups carry the
same verdict — a rebalancing instance has no caller to withhold itself from, so the stricter
readiness rule would stall a rollout without protecting anything.

This increment stops at the application: the endpoint is verified by tests against the management
port, and the Helm deployment that probes it follows separately.

**Acceptance Criteria**

- `GET /actuator/health` on the management port returns the stream's state, and reports `UP` when
  the topology is running.
- A stream driven into a terminal state (`ERROR`, `NOT_RUNNING`) makes the health endpoint report
  `DOWN`, on both the liveness and the readiness group.
- A rebalancing stream reports `UP` on both groups.
- The health response names the current `KafkaStreams.State`, so an operator reads the state
  without attaching a debugger.
- No business HTTP endpoint is served: the main application port answers nothing.

**Out of scope**

- The Helm deployment's `actuator` port and its startup, readiness, and liveness probes — the
  endpoint this increment adds is what makes them possible, and they land in a follow-up increment
  on top of this one, together with the startup-budget question the existing templates' thresholds
  raise for a pod that joins a consumer group.
- Custom domain metrics for the coverage calculation itself — [NF-001](../specs/NF-001-operational-observability.md)
  records that no custom metric exists yet, and this increment adds none.
- Routing this service's actuator surface through `api-gateway`; the stream sits behind no gateway
  route, so [CON-006](../specs/CON-006-actuator-endpoints-deny-by-default.md) is untouched.
- The same treatment for `otel-event-filter-stream`, which has the identical gap and should follow
  in its own increment.
- Any change to the topology, its error handling, or the response events it publishes.

## Relations

**Realizes**

- [SW-045](../specs/SW-045-stream-state-decides-the-health-verdict.md) — the health verdict
  this story delivers
- [ARCH-018](../specs/ARCH-018-management-surface-without-a-business-one.md) — the
  management-server decision that makes the verdict reachable

**Related**

- [SW-008](../specs/SW-008-kafka-as-async-calculation-driver.md) — the topology whose state is
  being reported, and the spec that records this service has no synchronous entry point
- [CON-002](../specs/CON-002-tolerate-dependency-outages.md) — the invariant that forces the
  rebalance-tolerant verdict
- [NF-001](../specs/NF-001-operational-observability.md) — the observability quality this increment
  advances
- [CON-006](../specs/CON-006-actuator-endpoints-deny-by-default.md) — the actuator exposure rule
  this increment deliberately does not touch
