# Periodic synchronization of API specifications from configured external sources

<!-- markdownlint-disable MD036 -->

**Title**
Periodic synchronization of API specifications from configured external sources

**Lens**: SYS

**Status**: active

**Description**
Snow-White periodically pulls API specifications from one or more configured external sources
(e.g. a JFrog Artifactory spec repository) rather than requiring specs to be pushed to it or
uploaded manually.
Synchronization runs on a schedule, not only on demand.

**Rationale**
API specifications change independently of when an analysis is triggered; a pull-based, scheduled
sync keeps the index current without coupling spec publication to any single pipeline run, and
supports the specification-first workflow where the spec is published ahead of the implementation.

**Verification Description**
An integration test publishes a specification to a stubbed external source, waits for a sync
cycle, and asserts the specification becomes available through the index.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — specs must be available before
  they can be correlated with telemetry

**Related**

- [SYS-002](SYS-002-indexed-spec-lookup.md) — what synchronization feeds
