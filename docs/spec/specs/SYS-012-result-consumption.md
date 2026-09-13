# Programmatic and visual consumption of analysis and quality-gate results

<!-- markdownlint-disable MD036 -->

**Title**
Programmatic and visual consumption of analysis and quality-gate results

**Lens**: SYS

**Status**: active

**Description**
Beyond the pass/fail gate result, Snow-White makes the full analysis result retrievable
programmatically (for external systems and CI/CD pipelines) and viewable through the web UI's
reporting and visualization, so a result is consumable both by machines and by a person reviewing
it.

**Rationale**
A CI pipeline needs the machine-consumable form to gate a build; a developer investigating why a
gate failed needs a visual, human-readable form of the same result.
Serving only one would leave the other audience unserved.

**Verification Description**
A test retrieves a completed analysis result through the programmatic interface and asserts its
content matches what the web UI renders for the same analysis.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — makes the correlation's outcome
  usable by its audiences

**Related**

- [SYS-009](SYS-009-machine-consumable-gate-result.md) — the gate-level subset of this
  consumption
