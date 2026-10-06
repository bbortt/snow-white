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
- [SW-039](SW-039-report-publishes-its-pinned-threshold.md) — the published threshold that makes
  the per-criterion detail here act-upon-able rather than merely present

## Changes

- **2026-10-03** — Records `SW-039` (`STR-022`) as what makes the "per-criterion detail" this
  capability promises actually machine-actionable.
  The detail was being served without the bar it is judged against, so the one consumer built on it
  — `calculate --agentic` — reported every included criterion as a failure, including on a gate
  that passed.
  The pass/fail outcome and the exit-code contract this spec verifies are unchanged; the defect was
  always in the detail, never in the verdict.
