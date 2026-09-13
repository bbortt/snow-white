# Handles a growing number of APIs and telemetry events without architectural changes

<!-- markdownlint-disable MD036 -->

**Title**
Handles a growing number of APIs and telemetry events without architectural changes

**Lens**: NF

**Status**: active

**Description**
Snow-White's architecture — the event-driven microservices split, Kafka-mediated asynchronous
coverage calculation — is chosen so that a growing number of indexed APIs and a growing telemetry
volume can be handled by scaling existing components (e.g. more `openapi-coverage-stream`
instances), not by redesigning the system.

**Rationale**
Coverage evaluation over large datasets or long lookback windows can take considerable time; the
asynchronous split (`docs/spec/architecture.md`) exists specifically so this work scales
independently of the synchronous request path, rather than growth forcing an architecture change
later.

**Verification Description**
A load test increases indexed-API count and telemetry volume and confirms throughput scales by
adding stream-processor instances, with no change to the request/response contracts other
components depend on.

## Relations

**Related**

- [SYS-007](SYS-007-on-demand-recomputation.md) — one of the operations that must scale
