# Result consumption

<!-- markdownlint-disable MD036 -->

**Title**
Result consumption

**Status**: done

**Business Value**
A gate result or an analysis result only creates value once someone or something can actually act
on it — fail a build, or show a developer what is missing.

**Problem / Context**
The audiences for a result differ: a CI pipeline needs a programmatic, machine-consumable form; a
developer investigating a failure needs a visual, human-readable form.
Serving only one leaves the other audience without a path to act.

**Solution Approach**
Snow-White makes the full analysis result, and the quality-gate pass/fail outcome, retrievable
programmatically for external systems and CI/CD pipelines, and viewable through the web UI's
reporting and visualization — the same underlying result, two consumption paths.

**Acceptance Criteria**

- A result retrieved programmatically matches what the web UI renders for the same analysis.
- The CLI's `calculate` command's exit code reflects the gate outcome, suitable for a CI pipeline
  step to branch on directly.

**Out of scope**

- Alerting or notification delivery (a caller polls or queries; Snow-White does not push results
  out).

## Relations

**Realizes**

- [SYS-012](../specs/SYS-012-result-consumption.md) — the two consumption paths

**Related**

- [SYS-009](../specs/SYS-009-machine-consumable-gate-result.md) — the gate-level subset of
  this consumption
- [NF-005](../specs/NF-005-clear-failure-feedback.md) — the failure-feedback half of this
  consumption
