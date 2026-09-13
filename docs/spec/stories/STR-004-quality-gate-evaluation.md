# Quality gate evaluation

<!-- markdownlint-disable MD036 -->

**Title**
Quality gate evaluation

**Status**: done

**Business Value**
Per-criterion fulfilled/unfulfilled results still leave "should this build pass or fail" to
manual judgment; a named, configurable gate turns that judgment into a repeatable, automatable
decision.

**Problem / Context**
Teams differ in which criteria matter to them and how strict their bar should be.
A single, hard-coded pass/fail rule would either be too strict for some teams or too lax for
others, and could not be referenced by name from a pipeline.

**Solution Approach**
A quality gate is a named, configurable selection of criteria and thresholds.
Snow-White ships predefined gate configurations and lets users define custom ones on the same
criteria set, evaluating a given analysis result against a selected gate to produce a
machine-consumable pass/fail result.
Predefined gates are immutable: a user extends the criteria set by defining an additional gate,
never by changing or deleting a predefined one.

**Acceptance Criteria**

- A custom gate with a looser threshold than a predefined gate can disagree with it on the same
  analysis result.
- Evaluating a gate produces a result a caller can branch on programmatically (not only a
  human-readable report).
- An attempt to modify or delete a predefined gate is rejected with a defined error.
  The predefined gate is left unchanged.

**Out of scope**

- Criteria definition itself (criteria are fixed and specification-derived — see
  `STR-003-criteria-based-analysis.md`); a gate only selects and thresholds them.

## Relations

**Realizes**

- [SYS-008](../specs/SYS-008-quality-gate-definitions.md) — the gate definition mechanism
- [SYS-009](../specs/SYS-009-machine-consumable-gate-result.md) — the evaluation result
- [CON-003](../specs/CON-003-predefined-quality-gates-are-immutable.md) — predefined gates
  cannot be changed or deleted

**Related**

- [SYS-006](../specs/SYS-006-criteria-based-evaluation.md) — the criteria a gate groups
