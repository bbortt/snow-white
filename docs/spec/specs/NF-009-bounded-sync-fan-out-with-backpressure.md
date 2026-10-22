# Sync fan-out is bounded by a configurable worker pool behind a bounded queue

<!-- markdownlint-disable MD036 -->

**Title**
Sync fan-out is bounded by a configurable worker pool behind a bounded queue

**Lens**: NF

**Status**: active

**Description**
A sync cycle downloads and parses the specifications it found at a bounded concurrency, regardless of
how many the source holds.
At most a configured number of specifications are in flight at once (three by default), and the
handover between listing and processing is a queue of a configured capacity (thirty by default).
When that queue is full the listing side waits rather than growing it, so the cycle's memory
footprint does not scale with the size of the repository.
Both bounds are operator settings.
A cycle over a source larger than either bound still processes every specification it listed —
bounding the fan-out delays work, it never discards it.

**Rationale**
The number of specifications in a spec repository is outside Snow-White's control and grows over the
deployment's life, so anything derived from it — a thread per specification, an unbounded queue of
pending downloads — is a footprint no one chose.
The pressure lands on the shared Artifactory instance as much as on the job: an unbounded fan-out
turns a routine sync into a burst of concurrent downloads against a system other teams are using,
which is the kind of behaviour that gets a job's credentials revoked.
The bounds are settable rather than fixed because the right values depend on the repository's size
and on what the Artifactory instance tolerates, both of which are site facts
([NF-007](NF-007-tempo-search-limit-is-operator-configurable.md) makes the same call for the Tempo
search limit).
Blocking the listing side when the queue is full is chosen over discarding or expanding because a
skipped specification would silently fail to index, and the delay costs nothing in a job with no
latency requirement.

**Verification Description**
A test runs a cycle with more specifications than both the configured worker count and the queue
capacity, recording how many are in flight at any moment.
It asserts the observed peak never exceeds the configured worker count, and that every specification
was nonetheless processed exactly once.
Lowering the queue capacity below the specification count does not change the outcome.

## Relations

**Related**

- [NF-003](NF-003-scalability-without-redesign.md) — the scalability property a bounded footprint
  preserves
- [NF-007](NF-007-tempo-search-limit-is-operator-configurable.md) — the sibling decision to make a
  fetch bound an operator setting
- [NF-006](NF-006-bounded-telemetry-fetch-footprint.md) — the same bounded-footprint reasoning on
  the telemetry side
- [SYS-001](SYS-001-periodic-spec-sync.md) — the cycle whose fan-out this bounds

## Changes

- **2026-09-29** — Set active: implementation of STR-019 began.
