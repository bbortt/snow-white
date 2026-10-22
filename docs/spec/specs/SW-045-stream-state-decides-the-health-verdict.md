# The Kafka Streams state decides the coverage stream's health verdict, with a rebalance counted as healthy

<!-- markdownlint-disable MD036 -->

**Title**
The Kafka Streams state decides the coverage stream's health verdict, with a rebalance counted as
healthy

**Lens**: SW

**Status**: active

**Description**
`openapi-coverage-stream` contributes a health indicator that reads the `KafkaStreams.State` of
the topology held by the `StreamsBuilderFactoryBean` and maps it to a verdict:

- `RUNNING` and `REBALANCING` are `UP`.
- `CREATED`, `PENDING_SHUTDOWN`, `PENDING_ERROR`, `ERROR`, and `NOT_RUNNING` are `DOWN`.

The mapping is exhaustive over `KafkaStreams.State`: every one of the seven constants is assigned
a verdict explicitly, so a state added by a future Kafka version fails the build rather than
silently defaulting to healthy.

The same mapping answers both liveness and readiness — there is deliberately no asymmetry between
the two groups.
`REBALANCING` counts as healthy in both.

The indicator contributes the current state by name to the health response, together with the
state of each stream thread, so the response distinguishes a topology that is wholly down from one
running with a dead thread.
Where no `KafkaStreams` instance exists yet — the factory bean present but not started — the
verdict is `DOWN` rather than an error, because an absent stream is a definite answer about
readiness and not a failure to determine one.

**Rationale**
A Kafka Streams process survives the death of its topology.
The JVM stays up and the process keeps running while `ERROR` or `NOT_RUNNING` means nothing is
being consumed, so process liveness alone cannot answer whether the service works — which is why
the verdict has to be derived from stream state rather than from the container being alive.

Counting `REBALANCING` as healthy follows from
[CON-002](CON-002-tolerate-dependency-outages.md).
Rebalancing is the ordinary response to a broker blip, a scale event, or a sibling pod restarting,
and it resolves on its own.
Reporting `DOWN` there would have Kubernetes restart the pod part-way through a coverage
calculation, which both discards in-flight work and provokes a further rebalance in the remaining
pods.
A health check that caused the data loss the constraint forbids would be worse than none.

Readiness carries the same verdict rather than the stricter `RUNNING`-only one because this
service has no inbound traffic to withhold
([SW-008](SW-008-kafka-as-async-calculation-driver.md)) — work arrives by Kafka record, and a
rebalancing consumer is already handled by Kafka's own partition assignment.
Readiness here therefore gates only rollout progression, and failing it during a routine rebalance
would stall a rolling update without protecting any caller.

`CREATED` is `DOWN` because a topology that has not started is not processing.
`PENDING_SHUTDOWN` and `PENDING_ERROR` are grouped with the terminal states deliberately: an
instance on its way down, or on its way into `ERROR`, is not coming back, so reporting it as alive
only delays the replacement.

**Verification Description**
Unit tests drive the indicator with a mocked `KafkaStreams` in each state and assert the resulting
verdict, including the no-instance case and the dead-thread detail.
An integration test starts the application against an embedded broker and asserts
`/actuator/health` reports `UP` with the state named in the response once the topology is running,
and that the liveness and readiness groups agree.
A test closes the stream and asserts the verdict turns `DOWN`.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality this behavior serves

**Related**

- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the topology whose state this reports,
  and the absence of inbound traffic that makes readiness and liveness agree
- [CON-002](CON-002-tolerate-dependency-outages.md) — the invariant that forces the
  rebalance-tolerant verdict
- [ARCH-018](ARCH-018-management-surface-without-a-business-one.md) — the management
  surface this verdict is served on
