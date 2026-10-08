# An unreadable specification is skipped and counted, and aborts the cycle only in strict mode

<!-- markdownlint-disable MD036 -->

**Title**
An unreadable specification is skipped and counted, and aborts the cycle only in strict mode

**Lens**: SW

**Status**: active

**Description**
A file in the configured source that cannot be turned into an indexable specification — it will not
download, it will not parse as OpenAPI, it is missing one of service name, API name or version, it
has no source URL, or it has no API type — does not fail the sync cycle.
It is skipped, and counted under the reason it was skipped, while every other specification in the
same cycle still publishes.
The operator can invert this per deployment with a strict parsing mode, under which the first such
file raises and the cycle aborts.
Graceful skipping is the default; strict is opt-in.

**Rationale**
A spec repository is shared and not curated for Snow-White: a draft, a partial file, or an
unrelated JSON document can appear in it at any time, put there by someone with no idea a sync job
reads the repository.
If one such file aborted the cycle, a single stray document would stop every other API in the
catalog from being indexed — an unattended job
([NF-002](NF-002-unattended-operation.md)) would go quietly useless, and the failure would surface
far away, as missing coverage rather than as a bad file.
Counting each skip under its own reason is what keeps "skipped" from meaning "silently dropped":
the cycle's own summary says how many files it could not index and why
([NF-005](NF-005-clear-failure-feedback.md)).
Strict mode exists because the opposite is right for a curated, Snow-White-owned repository, where
an unreadable file is a publication bug that should stop the line rather than be tallied.

**Verification Description**
A test runs a cycle over a source holding one valid specification and one file that fails at each
of the stages above, with the default mode, and asserts the valid one publishes while each
unreadable file is counted under its own distinct reason rather than raising.
The same source under strict mode raises instead, and the cycle does not complete.

## Relations

**Related**

- [NF-002](NF-002-unattended-operation.md) — the unattended operation a fail-fast default would
  undermine
- [NF-005](NF-005-clear-failure-feedback.md) — the feedback the per-reason tally provides
- [SYS-003](SYS-003-openapi-as-indexed-format.md) — the format a candidate file is parsed as
- [SYS-001](SYS-001-periodic-spec-sync.md) — the cycle a skip must not abort

## Changes

- **2026-09-29** — Set active: implementation of STR-019 began.
