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

The calculation's own instant is one of those inputs, not ambient state.
It is pinned when the calculation is triggered — the report's `createdAt`, published as `initiatedAt`
— and carried explicitly to whatever computes against it (`SW-043`), so nothing inside a calculation
asks a system clock what time it is.
A rule that depends on time is therefore still deterministic: the same specification, telemetry,
request and pinned instant answer the same way however much later they are replayed.

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
- [SW-043](SW-043-expiry-judged-against-the-triggering-instant.md) — the pinned calculation instant
  that keeps a time-dependent rule inside this invariant
- [SYS-011](SYS-011-bounded-time-resolution.md) — the other time input a calculation already consumes
  explicitly rather than ambiently

## Changes

- **2026-10-04** — Names the calculation instant as an input, for `SW-043` (`STR-023`).
  Nothing here is weakened: waiver expiry is the first rule in the system whose answer could have
  depended on the time of asking, and the fix was to pin the instant rather than to carve out an
  exception.
  The instant was already a parameter in practice — the telemetry window is bounded by a timestamp
  handed down the pipeline — so this records a property the calculation had and never stated.
