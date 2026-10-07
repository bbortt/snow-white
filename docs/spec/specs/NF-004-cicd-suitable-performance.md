# Processing time suitable for CI/CD feedback loops

<!-- markdownlint-disable MD036 -->

**Title**
Processing time suitable for CI/CD feedback loops

**Lens**: NF

**Status**: active

**Description**
Spec synchronization and telemetry-driven analysis complete within a time frame a CI/CD pipeline
can absorb as a build step, not as a background job a pipeline has to poll for indefinitely.

**Rationale**
Snow-White's primary placement is a pipeline gate (`RQ-5`, `RQ-6.2`); a pipeline step that routinely
took minutes to hours would be abandoned by teams regardless of the correctness of its result.

**Verification Description**
A benchmark runs a representative analysis (typical API size, typical lookback window) and asserts
completion within the documented time budget for a CI step.

## Relations

**Related**

- [SYS-011](SYS-011-bounded-time-resolution.md) — the hard bound this budget sits inside
