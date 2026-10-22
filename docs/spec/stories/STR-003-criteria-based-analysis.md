# Criteria-based coverage analysis

<!-- markdownlint-disable MD036 -->

**Title**
Criteria-based coverage analysis

**Status**: done

**Business Value**
Turns a correlated specification and telemetry set into a concrete verdict — which parts of the
API are covered, which are not — instead of leaving the raw correlation for a human to interpret.

**Problem / Context**
"This span matches this operation" is not, by itself, an answer to "is this API adequately
tested." Answering that requires a named, comparable set of checks (path coverage, response-code
coverage, parameter coverage, and others — see `pages/_pages/quality-gate-criteria.md`) applied
consistently across runs.

**Solution Approach**
Snow-White evaluates the correlated specification and telemetry against a fixed, predefined
criteria set, reporting each criterion as fulfilled or unfulfilled and associating the result with
the specific API version analyzed.
Results are not permanently cached: a result can be recomputed on request as new telemetry or a
new specification version arrives.

**Acceptance Criteria**

- An untested required element (e.g. a parameter never exercised) is reported unfulfilled while
  covered elements are reported fulfilled, in the same run.
- Recomputing an analysis after new telemetry arrives reflects that telemetry in the new result.
- Results for one API version never blend with results for a different version of the same API.

**Out of scope**

- Continuous background recomputation (recomputation is on request, not automatic).
- Criteria not derivable from the specification itself (see `pages/_pages/quality-gate-criteria.md`
  for the current set).

## Relations

**Realizes**

- [SYS-006](../specs/SYS-006-criteria-based-evaluation.md) — the evaluation itself
- [SYS-007](../specs/SYS-007-on-demand-recomputation.md) — recomputation on request

**Related**

- [CON-001](../specs/CON-001-deterministic-analysis-results.md) — the determinism this
  evaluation must honor
