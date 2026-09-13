# Tolerate temporary unavailability of external dependencies without data loss

<!-- markdownlint-disable MD036 -->

**Title**
Tolerate temporary unavailability of external dependencies without data loss

**Lens**: CON

**Status**: active

**Description**
A temporary outage of an external dependency (the telemetry backend, the spec repository, Kafka)
must not lose an in-flight request or a piece of already-ingested data.
The request either completes once the dependency recovers or resolves to a defined error (see
[SYS-011](SYS-011-bounded-time-resolution.md)) — it never silently drops data.
This invariant does not yet cover each service's own PostgreSQL persistence (`api-index-api`,
`quality-gate-api`, `report-coordinator-api`): an outage there relies on default Spring Data/JPA
behavior only, with no dedicated retry treatment and no test verifying this invariant holds for it.

**Rationale**
External dependencies (InfluxDB, Grafana Tempo, an Artifactory instance, Kafka itself) are outside
Snow-White's control and will have outages.
A system that lost data whenever one blipped would be unusable as a CI gate, where a false failure
has the same cost as a missed real one.
PostgreSQL is excluded above as a statement of current fact, not a design decision: unlike the
listed dependencies, it has received no equivalent resilience work, so claiming the invariant for
it here would overstate what is actually verified.

**Verification Description**
A test induces a temporary outage of the telemetry backend mid-request.
It asserts the request either retries to completion or resolves to a defined error, with no
ingested data lost in either case.
The check is rejected if data is silently dropped.

## Relations

**Related**

- [SYS-011](SYS-011-bounded-time-resolution.md) — the terminal-state guarantee this
  invariant relies on
