# Fully unattended operation once configured

<!-- markdownlint-disable MD036 -->

**Title**
Fully unattended operation once configured

**Lens**: NF

**Status**: active

**Description**
Once its configuration (external spec sources, telemetry backend, quality gates) is in place,
Snow-White requires no manual interaction to keep synchronizing specs, correlating telemetry, and
serving analyses — it runs as an unattended CI/CD and platform component.

**Rationale**
A tool that needs a human to click through a step for each analysis could not sit inside an
automated pipeline, which is Snow-White's primary intended placement (`RQ-5`, `RQ-6.2`).

**Verification Description**
A review of the deployment confirms no scheduled task, sync cycle, or triggered analysis requires
a manual step after initial configuration.

## Relations

**Related**

- [SYS-001](SYS-001-periodic-spec-sync.md) — one of the unattended cycles
- [SYS-010](SYS-010-analysis-triggering.md) — triggering itself stays automatable
