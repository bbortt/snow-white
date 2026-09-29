# An unreachable index defers the identity to a later cycle instead of failing the run

<!-- markdownlint-disable MD036 -->

**Title**
An unreachable index defers the identity to a later cycle instead of failing the run

**Lens**: SW

**Status**: active

**Description**
Both calls `api-sync-job` makes into `api-index-api` — the existence check and the ingestion — are
retried on a server error or a connection failure, with an exponentially growing delay between
attempts.
When the retries are exhausted the call does not propagate: the existence check resolves as
"not indexed" and the ingestion resolves without raising, so the cycle continues through its
remaining specifications and the process still exits successfully.
A specification whose ingestion was exhausted this way is not counted as published — it is counted
under the reason it actually reached, so the cycle's summary states how many identities were
deferred rather than reporting a publish that did not happen.
No specification is dropped by this: an identity that was not actually indexed is still absent at
the next cycle's existence check, so the next cycle publishes it.
The existence check resolving to "not indexed" is the deliberate direction — an unknown answer
biases towards attempting the publish, which the index's own immutability invariant
([CON-007](CON-007-stable-api-reference-is-immutable-once-indexed.md)) makes safe, rather than
towards skipping an API that may never have been indexed at all.

**Rationale**
This is [CON-002](CON-002-tolerate-dependency-outages.md) for the spec-index path, the way
[SW-008](SW-008-kafka-as-async-calculation-driver.md) is for `openapi-coverage-stream`'s telemetry
and Kafka paths.
`api-sync-job` is the one component that can afford to absorb an outage completely, because it is
periodic: the work it fails to do now is re-derived from the external source on the next run, so
there is nothing to persist, queue or compensate.
Failing the run instead would achieve nothing beyond a red job in the operator's history, and —
under a platform that retries a failed job — would re-run the whole catalog against a dependency
that is still down.
Biasing an unknown existence answer towards "absent" matters because the two mistakes are not
symmetric: wrongly attempting a publish costs one rejected request, while wrongly skipping leaves an
API unindexed and its telemetry uncorrelatable until someone notices.
Absorbing the outage silently is not the same as hiding it.
The per-reason tally is the only thing an unattended run leaves behind
([NF-005](NF-005-clear-failure-feedback.md)), so counting a deferred identity as published would
make the one artefact an operator reads assert the opposite of what happened, and a repeated outage
would look like a repeatedly successful sync.

**Verification Description**
A test makes the index answer the existence check with a server error for the configured number of
attempts and asserts the call is retried and then resolves as "not indexed" rather than raising.
A second test does the same for the ingestion call and asserts the cycle completes rather than
propagating the failure, and that the specification is absent from the cycle's published count.
Both fail if the exception reaches the caller; the second also fails if an exhausted ingestion is
tallied as a publish.

## Relations

**Related**

- [CON-002](CON-002-tolerate-dependency-outages.md) — the invariant this is the spec-index-path
  instance of
- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the sibling instance of the same
  invariant, for `openapi-coverage-stream`
- [CON-007](CON-007-stable-api-reference-is-immutable-once-indexed.md) — the invariant that makes a
  wrongly attempted publish harmless
- [NF-002](NF-002-unattended-operation.md) — the unattended operation an absorbed outage preserves
- [NF-005](NF-005-clear-failure-feedback.md) — the feedback the per-reason tally is, and which
  counting a deferral as a publish would falsify
- [SYS-001](SYS-001-periodic-spec-sync.md) — the periodicity that makes deferral a complete recovery

## Changes

- **2026-09-29** — Set active: implementation of STR-019 began.
