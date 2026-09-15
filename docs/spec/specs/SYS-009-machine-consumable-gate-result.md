# Machine-consumable quality-gate evaluation result

<!-- markdownlint-disable MD036 -->

**Title**
Machine-consumable quality-gate evaluation result

**Lens**: SYS

**Status**: active

**Description**
Evaluating analysis results against a selected quality gate produces a result a machine can act on
directly — pass or fail, per-criterion detail — rather than only a human-readable report.
A CI pipeline step consumes this to decide whether to fail the build.

**Rationale**
A quality gate whose result only a human could interpret could not be wired into an automated
pipeline; the gate's entire purpose (per `RQ-6.2`/CI-CD usage) depends on a caller being able to
branch on the result programmatically.

**Verification Description**
The CLI's `calculate` command is run against a failing gate and asserts a non-zero exit code; run
against a passing gate, it asserts a zero exit code.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — the actionable outcome of the
  correlation

**Related**

- [SYS-008](SYS-008-quality-gate-definitions.md) — what is being evaluated
- [SYS-012](SYS-012-result-consumption.md) — the broader set of ways a result is consumed
