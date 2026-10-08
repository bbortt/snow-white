# OpenAPI as the supported indexed specification format

<!-- markdownlint-disable MD036 -->

**Title**
OpenAPI as the supported indexed specification format

**Lens**: SYS

**Status**: active

**Description**
Snow-White indexes and analyzes OpenAPI (v3.x) specifications today; it does not yet support other
specification formats such as AsyncAPI.
The criteria and quality-gate model are format-agnostic by design, so a future format is
additive, not a redesign — but only OpenAPI is indexed and analyzed now.

**Rationale**
OpenAPI is the dominant specification format for the synchronous HTTP APIs Snow-White targets
first; committing the criteria model to be format-agnostic from the start avoids having to redesign
it when a second format (e.g. AsyncAPI for event-driven APIs) is added later.

**Verification Description**
A review of the indexing and criteria-evaluation code confirms no OpenAPI-specific assumption
leaks into the criteria model itself, and that a specification in an unsupported format is
rejected with a clear error rather than silently mis-indexed.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — the spec side of the correlation

**Related**

- [SYS-001](SYS-001-periodic-spec-sync.md) — what is synchronized
- [SYS-006](SYS-006-criteria-based-evaluation.md) — the format-agnostic criteria model
