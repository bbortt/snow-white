# Identical analysis results for identical specification and telemetry input

<!-- markdownlint-disable MD036 -->

**Title**
Identical analysis results for identical specification and telemetry input

**Lens**: CON

**Status**: active

**Description**
Given the same API specification and the same telemetry data, Snow-White always produces the same
analysis result.
No source of non-determinism (ordering, timing, sampling) may change the outcome for identical
input.

**Rationale**
A quality gate that could pass or fail differently on a rerun of the exact same input would be
untrustworthy as a CI gate — teams need to trust that a failure reflects the input, not a coin
flip.

**Verification Description**
A test runs the same analysis twice against frozen specification and telemetry fixtures and asserts
byte-for-byte identical results; the check is rejected (fails the test) if the two runs diverge.

## Relations

**Related**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the computation this invariant governs
- [SYS-007](SYS-007-on-demand-recomputation.md) — recomputation must still honor this
