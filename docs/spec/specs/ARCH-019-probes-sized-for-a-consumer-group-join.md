# A stream deployment probes the actuator port with a startup budget that covers a consumer-group join

<!-- markdownlint-disable MD036 -->

**Title**
A stream deployment probes the actuator port with a startup budget that covers a consumer-group
join

**Lens**: ARCH

**Status**: active

**Description**
The `openapi-coverage-stream` deployment declares the management port (8090) as a named
`actuator` container port and carries startup, readiness, and liveness probes against the health
surface [ARCH-018](ARCH-018-management-surface-without-a-business-one.md) confines to it.
No `http` port is declared: the main application port serves nothing, so there is no port to name.

The startup probe is budgeted for a JVM process that has to build a topology and join a consumer
group before it can answer, rather than for a native image that answers almost immediately.
It targets the aggregate `/actuator/health`, and its budget is minutes rather than the roughly
twenty seconds the four native-image deployments allow.

Readiness and liveness target the `readiness` and `liveness` probe groups, which
[SW-045](SW-045-stream-state-decides-the-health-verdict.md) gives the same rebalance-tolerant
verdict.
Readiness is checked no more aggressively than liveness, because this service has no inbound
traffic to withhold.

The probes are fixed in the template rather than exposed through values, matching every other
deployment in the chart.
Nothing declares a `Service` for the management port: the kubelet reaches the pod directly, and
adding one would widen the surface [ARCH-018](ARCH-018-management-surface-without-a-business-one.md)
deliberately keeps narrow.

**Rationale**
The thresholds the existing templates use cannot be copied onto this service.
All four of them carry `initialDelaySeconds: 1`, `periodSeconds: 1`, and `failureThreshold: 19`
under the comment "native apps start fast, so allow minimal delay" — a budget of roughly twenty
seconds before the kubelet kills the container.
That budget is true of a native image.
`openapi-coverage-stream` is one of the two deployments in the chart that runs on the JVM, and it
is the only one that must also reach a broker and complete a group join before its health surface
answers `UP`.
A JVM start, a Spring context, a topology build, and a join cannot be relied on to finish inside
twenty seconds on a loaded node, and a startup probe that expires mid-join restarts the pod —
which triggers a further rebalance in its siblings and never converges.
Sizing the budget in minutes makes a slow start slow rather than fatal.

The startup probe targets the aggregate health endpoint rather than a probe group because the
question it asks is different: not "is this instance healthy" but "has it got far enough to be
asked".
`CREATED` and the absent-instance case are `DOWN` by
[SW-045](SW-045-stream-state-decides-the-health-verdict.md), so the aggregate stays down until the
topology is genuinely up, and the first `REBALANCING` satisfies it.

Readiness uses the slower of the two cadences the chart's templates use, inverting the
"stop routing quickly if unhealthy" reasoning the HTTP services apply.
That reasoning assumes a caller whose requests must stop arriving.
This service has none ([SW-008](SW-008-kafka-as-async-calculation-driver.md)) — readiness here
gates only rollout progression, so a tighter threshold would risk stalling a rolling update
without protecting a single request.

Liveness restarts promptly once the verdict turns `DOWN`, because by
[SW-045](SW-045-stream-state-decides-the-health-verdict.md) every `DOWN` state other than a
not-yet-started one is terminal.
A rebalancing instance never reaches that verdict, so the restart that
[CON-002](CON-002-tolerate-dependency-outages.md) forbids cannot be triggered by an ordinary
broker blip.

**Verification Description**
Chart tests render the deployment and assert the `actuator` container port is declared on 8090,
that no `http` port is declared alongside it, and that all three probes target the health paths on
that named port.
A test asserts the startup budget — the product of its period and failure threshold — is at least
two minutes, so the join allowance is checked rather than left to a reviewer reading numbers.
A test asserts readiness is polled no more often than liveness.
No chart test can prove a real join finishes inside the budget; that remains a property of the
cluster, and the budget is deliberately generous rather than measured.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality these probes put to
  work

**Related**

- [ARCH-018](ARCH-018-management-surface-without-a-business-one.md) — the management surface these
  probes target, and the spec that defers the probes themselves to this one
- [SW-045](SW-045-stream-state-decides-the-health-verdict.md) — the verdict the probe groups carry
- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the absence of inbound traffic that makes
  an aggressive readiness threshold pointless here
- [CON-002](CON-002-tolerate-dependency-outages.md) — the invariant a rebalance-triggered restart
  would violate
