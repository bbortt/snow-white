# A sync cycle counts its specifications by load status, so an index outage does not read as an empty cycle

<!-- markdownlint-disable MD036 -->

**Title**
A sync cycle counts its specifications by load status, so an index outage does not read as an empty
cycle

**Lens**: SW

**Status**: planned

**Description**
At the end of each sync cycle, `api-sync-job` increments `snow.white.sync.specifications` once per
specification, dimensioned by `snow.white.load.status` — the `ApiLoadStatus` the specification
reached.
The values are the enum's ten constants, unchanged and uncollapsed — `PUBLISHED`,
`PUBLISH_DEFERRED`, `LOADED`, `DOWNLOAD_FAILED`, `PARSE_FAILED`, `LOAD_FAILED`,
`MANDATORY_INFORMATION_MISSING`, `NO_SOURCE`, `NO_API_TYPE`, and `UNLOADED`.

The counter is fed from the `Map<ApiLoadStatus, Long>` that `ApiSyncProcessor.process` already
returns and `SyncJob.syncCatalog` already logs.
No count is computed for the metric, and the workers do not touch the registry.

A cycle that aborts — which only a strict parsing mode causes — emits the counts accumulated before
the abort, so a partial cycle is not a silent one.

`api-sync-job` gains the dependency needed to register a meter; it carries neither
`spring-boot-starter-actuator` nor the OpenTelemetry Spring Boot starter today.

**Rationale**
The distinction this metric carries is already made, deliberately, in the code — and currently
reaches nothing but a log line.
`SyncJob.publishLoadedApi` separates `PUBLISH_DEFERRED` from `LOADED` precisely so that, in its own
words, a cycle that published nothing because the index was down does not read like a cycle that
had nothing to publish.
That is an alert threshold expressed as a comment.
`PUBLISH_DEFERRED` rising is the index-outage signal, and it is invisible today.

Emitting once per cycle from the returned map, rather than incrementing inside the workers, keeps
the registry off the concurrent path.
`ApiSyncProcessor` already tallies into a `ConcurrentHashMap` of `AtomicLong` under a bounded
worker pool ([NF-009](NF-009-bounded-sync-fan-out-with-backpressure.md)); adding a second
concurrent write per specification would duplicate that tallying for no additional information.

The enum is not collapsed into success and failure because the ten constants are not
interchangeable to an operator.
`DOWNLOAD_FAILED` points at the source repository, `PARSE_FAILED` at the specification's author,
`MANDATORY_INFORMATION_MISSING` at its metadata, and `PUBLISH_DEFERRED` at Snow-White's own index —
four different people to talk to.
A collapsed failure count would say only that somebody should be talked to.

The cost of the new dependency is accepted rather than worked around.
The alternative — leaving this module uninstrumented because it is the awkward one — would omit the
cheapest high-value metric in the system, since the aggregate already exists and only needs
emitting.

**Verification Description**
A unit test drives `SyncJob` with a processor returning a known status map and asserts the counter
is incremented by the mapped amount for each status, against a `SimpleMeterRegistry`.
A test asserts a cycle whose supplier raises still emits the counts accumulated before the abort.
A test asserts the counter's tag keys are exactly the load status, per
[CON-017](CON-017-metric-dimensions-are-bounded.md).
A review confirms no registry interaction occurs inside `ApiSyncProcessor`'s workers.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality this cycle counter
  serves

**Related**

- [SW-035](SW-035-index-outage-defers-to-next-cycle.md) — the deferral this counter finally makes
  visible
- [ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md) — the decision
  that a batch count is instrumented in code, because the collector would count cycles
- [CON-017](CON-017-metric-dimensions-are-bounded.md) — the invariant this counter's
  single dimension satisfies
- [NF-009](NF-009-bounded-sync-fan-out-with-backpressure.md) — the bounded fan-out whose tally
  this counter reads
- [SW-034](SW-034-unreadable-spec-skipped-unless-strict.md) — the skip-versus-abort behaviour that
  decides whether a cycle's counts are complete
- [CON-002](CON-002-tolerate-dependency-outages.md) — the outage tolerance that turns an index
  failure into a deferral rather than an error
