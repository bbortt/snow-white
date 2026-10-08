# Sync cadence is owned by the scheduler, not by the sync job

<!-- markdownlint-disable MD036 -->

**Title**
Sync cadence is owned by the scheduler, not by the sync job

**Lens**: ARCH

**Status**: active

**Description**
`api-sync-job` is a one-shot process, not a resident service with a timer.
It runs a single sync pass on startup and then exits; it holds no schedule, and the module declares
no in-process scheduling.
The periodicity [SYS-001](SYS-001-periodic-spec-sync.md) calls for is supplied by whatever schedules
the process — the shipped Helm chart renders a Kubernetes `CronJob` whose `schedule` is an operator
value, with the run's own deadline, retry-on-failure and history limits owned by the same manifest.

**Rationale**
The cadence is an operating decision, not a product one: how often a spec repository is worth
polling depends on how often that repository changes, and an operator who has to redeploy to change
it will simply not change it.
Delegating it also hands the hard parts of periodic work to a component built for them.
Overlap, missed runs, a maximum run duration, and how many failed runs to keep are all already
answered by a `CronJob`, and would each need re-implementing — with their own bugs — inside a
resident service.
A one-shot process additionally frees its memory and its connections between runs, which a resident
scheduler holding a thread pool for a job that runs a few times a day would not.
The cost accepted in exchange is that nothing inside Snow-White can state when the last sync ran, or
trigger one on demand; that belongs to the scheduler.

**Verification Description**
A structural test asserts the module's production code declares no in-process scheduling — no
scheduling annotation and no scheduling enablement — so a timer cannot be reintroduced without
failing the build.
A test asserts the startup hook runs the sync pass exactly once per process start and returns, so
the process exits when the pass completes.
A chart test asserts the rendered workload is a `CronJob` and that its schedule comes from an
operator-supplied value.

## Relations

**Related**

- [SYS-001](SYS-001-periodic-spec-sync.md) — the periodic capability whose cadence this delegates
- [NF-002](NF-002-unattended-operation.md) — the unattended operation the scheduler delivers
- [NF-003](NF-003-scalability-without-redesign.md) — the footprint a one-shot process keeps out of
  the running system

## Changes

- **2026-09-29** — Set active: implementation of STR-019 began.
