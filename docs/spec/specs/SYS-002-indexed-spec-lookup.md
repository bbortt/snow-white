# Indexed lookup of API specifications by service name, API name, and API version

<!-- markdownlint-disable MD036 -->

**Title**
Indexed lookup of API specifications by service name, API name, and API version

**Lens**: SYS

**Status**: active

**Description**
Every synchronized API specification is indexed under the triple (service name, API name, API
version), so a specific specification can be looked up directly rather than scanned for.
This triple, not the spec's own internal identifiers, is Snow-White's addressing scheme for a
spec.

**Rationale**
Coverage analysis and quality-gate triggering both need to resolve "this exact API, this exact
version" to a specification quickly and unambiguously; a stable, explicit key is simpler and more
predictable than inferring identity from spec content.

**Verification Description**
An integration test indexes two specifications differing only in version, then queries by the
triple and asserts each query resolves to the correct one.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — specs must be resolvable before
  they can be correlated with telemetry

**Related**

- [SYS-001](SYS-001-periodic-spec-sync.md) — what populates the index
- [SYS-010](SYS-010-analysis-triggering.md) — triggering an analysis uses this same triple
