# Kubernetes restarts a coverage stream whose topology has died

<!-- markdownlint-disable MD036 -->

**Title**
Kubernetes restarts a coverage stream whose topology has died

**Status**: active

**Business Value**
[STR-024](STR-024-the-coverage-stream-reports-its-own-health.md) made the coverage stream's
topology state readable from outside the process, but nothing reads it.
A stream in `ERROR` still serves a `DOWN` verdict to an endpoint no one probes, so the pod stays
up, the partitions stay assigned to it, and calculation requests keep piling up on the topic.
Wiring the deployment's probes to that verdict is what turns the diagnosis into a recovery: the
pod is replaced automatically, its partitions are reassigned, and the backlog drains without an
operator being paged.

**Problem / Context**
`helm/charts/snow-white/templates/openapi-coverage-stream.yaml` declares no container port and no
probe.
[STR-024](STR-024-the-coverage-stream-reports-its-own-health.md) listed both as out of scope and
named the open question they raise: the startup-budget the existing templates' thresholds imply
for a pod that joins a consumer group.

That question has a concrete answer, and it is why this is not a copy-paste of an existing
template.
All four deployments that already probe an actuator port — `api-gateway`, `api-index-api`,
`quality-gate-api`, and `report-coordinator-api` — share one threshold block, commented "native
apps start fast, so allow minimal delay", that allows roughly twenty seconds before the kubelet
kills the container.
Three of those four run as native images, where that holds.
`openapi-coverage-stream` runs on the JVM and additionally has to reach a broker and finish a
group join before its health surface answers `UP`.
Copying the twenty-second budget onto it would produce a restart loop on exactly the slow starts
the probe exists to survive.

The verdict's own shape is already settled and needs no revisiting:
[SW-045](../specs/SW-045-stream-state-decides-the-health-verdict.md) counts `REBALANCING` as
healthy on both probe groups, so a rebalance cannot trip either probe.

**Solution Approach**
Declare the management port as a named `actuator` container port on 8090, and add startup,
readiness, and liveness probes against the health surface
([ARCH-019](../specs/ARCH-019-probes-sized-for-a-consumer-group-join.md)).
Leave the main application port undeclared — it serves nothing
([ARCH-018](../specs/ARCH-018-management-surface-without-a-business-one.md)).

Size the startup probe in minutes rather than seconds so a JVM start plus a group join fits inside
it, and point it at the aggregate `/actuator/health`, which stays `DOWN` until the topology is
genuinely up.
Point readiness and liveness at the `readiness` and `liveness` groups, and poll readiness no more
aggressively than liveness, since this service has no inbound traffic whose routing readiness
could protect
([SW-008](../specs/SW-008-kafka-as-async-calculation-driver.md)).

Keep the thresholds in the template rather than in values, as every other deployment in the chart
does, and extend the chart's vitest suite to assert the ports, the probe targets, and the startup
budget.

**Acceptance Criteria**

- The rendered deployment declares an `actuator` container port on 8090 and no `http` port.
- Startup, readiness, and liveness probes target `/actuator/health`,
  `/actuator/health/readiness`, and `/actuator/health/liveness` on the named `actuator` port.
- The startup probe's budget — period times failure threshold — is at least two minutes.
- Readiness is polled no more often than liveness.
- A pod whose topology reaches a terminal state fails its liveness probe and is restarted; a
  rebalancing pod is not.

**Out of scope**

- Exposing any probe threshold through `values.yaml` — the chart hardcodes probes for every other
  deployment, and a per-cluster override is a separate question.
- A `Service` for the management port; the kubelet probes the pod directly, and
  [ARCH-018](../specs/ARCH-018-management-surface-without-a-business-one.md) keeps that surface
  unrouted.
- The same treatment for `otel-event-filter-stream`, which still has no health surface at all and
  needs [STR-024](STR-024-the-coverage-stream-reports-its-own-health.md)'s increment before it can
  have probes.
- `terminationGracePeriodSeconds` and any other shutdown tuning for a draining stream.
- Custom domain metrics, scrape annotations, or any monitoring surface beyond the health endpoint.

## Relations

**Realizes**

- [ARCH-019](../specs/ARCH-019-probes-sized-for-a-consumer-group-join.md) — the probe and
  startup-budget decision this story delivers

**Related**

- [STR-024](STR-024-the-coverage-stream-reports-its-own-health.md) — the increment that added the
  health surface these probes target, and that deferred them here
- [ARCH-018](../specs/ARCH-018-management-surface-without-a-business-one.md) — the management-only
  surface the probes reach
- [SW-045](../specs/SW-045-stream-state-decides-the-health-verdict.md) — the rebalance-tolerant
  verdict the probe groups carry
- [SW-008](../specs/SW-008-kafka-as-async-calculation-driver.md) — the absence of inbound traffic
  that shapes the readiness threshold
- [CON-002](../specs/CON-002-tolerate-dependency-outages.md) — the invariant that a
  rebalance-triggered restart would violate
- [NF-001](../specs/NF-001-operational-observability.md) — the observability quality this increment
  completes for this service
