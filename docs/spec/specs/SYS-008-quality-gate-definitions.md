# Configurable quality-gate definitions that group criteria, with predefined and user-defined thresholds

<!-- markdownlint-disable MD036 -->

**Title**
Configurable quality-gate definitions that group criteria, with predefined and user-defined
thresholds

**Lens**: SYS

**Status**: active

**Description**
A quality gate is a named, configurable selection of criteria (and their thresholds) that analysis
results are checked against.
Snow-White ships predefined quality-gate configurations and lets users define their own custom
thresholds on top of the same criteria set.

**Rationale**
Teams differ in how strict their coverage bar should be and which criteria matter to them; a fixed,
single gate would either be too strict for some teams or too lax for others.
Naming a gate separately from the criteria set lets a team select or tune a gate without
redefining what each criterion means.

**Verification Description**
A test defines a custom quality gate with a threshold looser than a predefined gate's, evaluates
the same analysis result against both, and asserts the two gates disagree on pass/fail where
expected.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — turns per-criterion results into
  an actionable pass/fail

**Related**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the criteria a gate groups
- [SYS-009](SYS-009-machine-consumable-gate-result.md) — the result of evaluating a gate
