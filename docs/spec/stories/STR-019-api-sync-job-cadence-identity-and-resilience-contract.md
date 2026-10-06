# Documented cadence, identity, failure tolerance and fan-out contract for `api-sync-job`

<!-- markdownlint-disable MD036 -->

**Title**
Documented cadence, identity, failure tolerance and fan-out contract for `api-sync-job`

**Status**: active

**Business Value**
`api-sync-job` is the component that makes [SYS-001](../specs/SYS-001-periodic-spec-sync.md) real,
and it is the last microservice with no retrace at all besides `otel-event-filter-stream`, which
[STR-008](../stories/STR-008-openapi-coverage-stream-backend-and-messaging-architecture.md) put out of scope.
A source read found **no** code in the module anchored to any spec in the corpus — its entire
behaviour lives only in the code.
What that hides is not incidental: the job decides which identities are worth publishing, tolerates a
bad file and an unreachable index without failing, reads the correlation key out of the
specification document, and bounds its own fan-out against a shared Artifactory instance.
None of those is visible from `SYS-001`, which says only that sync happens periodically.
Pinning them gives the operator-facing surface (the schedule, the parsing mode, the two concurrency
bounds, the three JSON paths) a stated intent to check against, and gives the next change to this
module something other than the current code to reason from.
Half of that surface is not reachable from the chart today — only the schedule and the JSON paths
are values, so the two bounds and the parsing mode can be set only through `additionalEnvs`, by an
operator who already knows the property names.
Specifying a setting the chart does not expose is specifying it for nobody, so this story also
carries the four settings through to `values.yaml`.

**Problem / Context**
`microservices/api-sync-job` lists candidate files in a configured Artifactory repository, downloads
and parses each as OpenAPI, extracts an identity, and publishes the ones the index does not already
hold.
A source read of `SyncJob`, `ApiSyncProcessor`, `ArtifactoryApiCatalogService`,
`OpenApiValidationService`, `ApiIndexCachingService` and `ApiIndexApiClient`, against the existing
unit, integration and Helm suites, found the module carries zero trace markers, and that the specs
above it (`SYS-001`, `SYS-003`, `SYS-013`, `CON-002`, `NF-002`) are unanchored anywhere in the
repository.

Several decisions stood out as consequential and not derivable from the code's shape or the
component's name:

- The existence check that makes the cycle idempotent is asked with prereleases **excluded**, so an
  identity held only by a prerelease is treated as absent and the stable specification takes the
  identity over on the first cycle after publication.
  The job always submits as stable.
- A file that will not download, will not parse, or lacks a mandatory field is skipped and counted
  under its own reason, and the rest of the cycle proceeds — unless the operator selects a strict
  parsing mode, under which the first such file aborts the cycle.
  Graceful is the default.
- Both calls into `api-index-api` retry with exponential backoff and then resolve without raising:
  the existence check as "not indexed", the ingestion silently.
  The run always succeeds; the work is re-derived next cycle.
- A candidate file is parsed without following its `$ref` pointers, which is why a specification
  whose identity fields sit behind a `$ref` never indexes.
  The parse cost, not the identity extraction, is the dominant cost of a cycle.
- The identity triple is read from the specification document at three operator-settable JSON paths,
  with the OTel service name carried as an `info` vendor extension — the same extraction the CLI
  applies to a prerelease upload.
- The cycle processes at a bounded concurrency behind a bounded queue, both operator-settable, so the
  footprint does not scale with the repository.
- The job holds no schedule: it runs one pass and exits, and the shipped Helm chart supplies the
  cadence as a Kubernetes `CronJob`.

Three of these are also **under-verified** today.
Nothing indexes a prerelease and then observes the stable specification taking the identity over —
the prerelease-excluding half of the existence check is asserted only as an argument passed to a
mock, which would still pass if the index ignored the flag.
Nothing observes the concurrency bound; the processor's tests assert every supplier runs, not that no
more than the configured number run at once.
Nothing prevents an in-process timer being added to the module.

One of them is also **wrong** today.
`ApiIndexApiClient.recoverIngestApiWithHttpInfo` returns `200 OK` when the retries are exhausted, so
`ApiIndexCachingService` sees a successful publish, `SyncJob` counts the specification as
`PUBLISHED`, and the single log line an unattended run leaves behind reports a publish that never
reached the index.
The deferral is right; the number is not.

**Solution Approach**
Add seven specs — three `SW` for the publish decision, the skip-or-abort behaviour and the absorbed
index outage; two `ARCH` for the delegated cadence and the in-document identity; two `NF` for the
bounded fan-out and the reference resolution — and anchor each to the code that decides it and the
tests that exercise it.
Add the three missing verifications named above: an integration test that indexes a prerelease and
asserts the stable specification supersedes it, a processor test that records peak in-flight
concurrency against the configured bound, and a structural rule that fails the build if the module
declares in-process scheduling.

Two behaviour changes come with the specs, because writing either one down truthfully would have
meant writing down something worth fixing instead.
The ingestion recovery resolves as a failed publish rather than a successful one, so an exhausted
retry is counted under the reason it reached instead of inflating the published count.
And `$ref` resolution moves from a hardcoded `false` to an operator setting that keeps `false` as
its default, so a deployment whose specifications assemble from fragments has a way in.
Both settings, plus the two concurrency bounds and the parsing mode, become `values.yaml` entries
on the chart's `apiSyncJob` section.

**Acceptance Criteria**

- A spec states that the cycle skips an identity the index already holds as a stable entry, that an
  identity held only by a prerelease is treated as absent, and that the job always submits as a
  stable release — so a stable specification supersedes a prerelease under the same identity.
- A spec states that a file which cannot be downloaded, parsed, or completed with all mandatory
  fields is skipped and counted under its own reason without failing the cycle, and that a strict
  parsing mode inverts this so the first such file aborts the cycle.
- A spec states that both calls into `api-index-api` are retried with growing delay and then resolve
  without propagating, that the existence check resolves as "not indexed" specifically, that a
  deferred ingestion is not counted as published, and that the deferred identity is published by a
  later cycle rather than lost.
- A spec states that the identity triple is read from the specification document at three
  operator-settable JSON paths, names the defaults, and states that an unresolvable path yields no
  value rather than an error.
- A spec states that a cycle's in-flight work is bounded by a configurable worker count behind a
  bounded queue, that the listing side waits rather than the queue growing, and that no listed
  specification is discarded by either bound.
- A spec states that the job holds no schedule — one pass per process start, then exit — and that the
  cadence is supplied by whatever schedules the process.
- A spec states that a candidate file is parsed without following its `$ref` pointers by default,
  that an operator may turn resolution on, and that a specification whose identity sits behind a
  `$ref` is skipped rather than failing the cycle while resolution is off.
- An ingestion call that exhausts its retries leaves its specification out of the cycle's published
  count, and a test fails if it is counted as published.
- The chart exposes the parsing mode, both concurrency bounds and the reference-resolution setting
  as `apiSyncJob` values, each rendering its property's environment variable only when set, and a
  chart test asserts both the set and the unset rendering.
- Every one of the seven is anchored to the code that decides it and to a test that would fail if
  the decision were reversed, including the three verifications that do not exist yet.

**Out of scope**

- Anchoring the system-level and stakeholder-level specs this module realizes (`SYS-001`, `SYS-003`,
  `STK-001`).
  They stay related but unclaimed, consistent with the `api-gateway`, `quality-gate-api`,
  `report-coordinator-api` and `api-index-api` retraces
  ([STR-009](../stories/STR-009-quality-gate-configuration-and-criteria-management-api.md),
  [STR-015](../stories/STR-015-api-gateway-ingress-routing-security-and-spa-fallback.md),
  [STR-016](../stories/STR-016-api-index-api-ingestion-and-lookup-contract.md)); closing that gap is its own
  story.
- `ApiSyncJobPropertiesValidator`'s required-configuration fail-fast check, for the same reason
  `STR-016` left the equivalent out: the pattern repeats in every microservice and is worth one
  generic spec, not a per-module one.
- The `ApiLoadStatus` value set as a published vocabulary.
  The per-reason tally is specified as behaviour above; the enum itself is an internal counter, not a
  contract any consumer reads.
- The candidate-file pattern (`*.json`, `*.yml`, `*.yaml` under the configured repository).
  It states which files are looked at, not a decision about what happens to them.
- Artifactory outage tolerance.
  Unlike the index calls, the Artifactory client carries no retry treatment, so `CON-002` is not
  claimed for that leg; stating it here would overstate what holds.
- Retrying or otherwise recovering an exhausted ingestion within the same cycle.
  The tally now tells the truth about it, but the deferral itself stays exactly as specified — the
  next cycle republishes, and nothing is queued or compensated in between.
- Surfacing the per-reason tally anywhere but the run's own log line.
  A machine-readable run summary is a real gap, and a separate one.

## Relations

**Realizes**

- [SW-033](../specs/SW-033-sync-skips-stable-supersedes-prerelease.md) — the publish
  decision and prerelease supersession
- [SW-034](../specs/SW-034-unreadable-spec-skipped-unless-strict.md) — the skip-and-count
  default and the strict-mode inversion
- [SW-035](../specs/SW-035-index-outage-defers-to-next-cycle.md) — the absorbed index outage
- [ARCH-014](../specs/ARCH-014-sync-cadence-owned-by-the-scheduler.md) — the cadence
  delegated to the scheduler
- [ARCH-015](../specs/ARCH-015-identity-declared-in-the-specification.md) — the identity
  read from the document
- [NF-009](../specs/NF-009-bounded-sync-fan-out-with-backpressure.md) — the bounded fan-out
- [NF-010](../specs/NF-010-reference-resolution-is-off-by-default.md) — the reference
  resolution made an operator setting

**Related**

- [STR-001](../stories/STR-001-api-specification-ingestion.md) — the original ingestion story this module is the
  pull half of
- [STR-016](../stories/STR-016-api-index-api-ingestion-and-lookup-contract.md) — the retrace of the service on
  the other side of every call this job makes
- [SYS-001](../specs/SYS-001-periodic-spec-sync.md) — the capability this module delivers
- [SYS-013](../specs/SYS-013-prerelease-specification-ingestion.md) — the prerelease path whose
  entries a stable sync supersedes
- [CON-002](../specs/CON-002-tolerate-dependency-outages.md) — the invariant the absorbed index
  outage is an instance of
