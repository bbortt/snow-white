# Report status is persisted as a stable numeric code, and an unknown code reads as `NOT_STARTED`

<!-- markdownlint-disable MD036 -->

**Title**
Report status is persisted as a stable numeric code, and an unknown code reads as `NOT_STARTED`

**Lens**: ARCH

**Status**: active

**Description**
Both a report and an API test store their status as an explicit numeric code carried by the status
enum, not as the enum's name and not as its ordinal.
The code is assigned per constant and is part of the persisted contract: reordering, renaming, or
inserting constants must not change an existing constant's code.
Reading is total — a stored code matching no constant decodes to `NOT_STARTED` rather than raising.
Newly created reports and API tests both start at the `IN_PROGRESS` code.

A finding's status (`ARCH-012`) is persisted the same way: an explicit per-constant code, stable
across reordering and renaming, read tolerantly.
Its landing spot for an unrecognised code is `NOT_APPLICABLE`, not `NOT_STARTED` — a finding has no
lifecycle to be early in, and `NOT_APPLICABLE` is the one status that sits outside both sides of
the coverage fraction (`SW-030`), so an undecodable row cannot silently move a ratio in either
direction.
Codes are not shared across the two enums; each is stable only within its own column.
On the read API a finding's status is serialised by name, so the numeric code stays an internal
storage detail and never becomes a published contract.

**Rationale**
An ordinal binds the database to source order, so inserting a constant silently rewrites the
meaning of every stored row; a name binds it to spelling, so a rename does the same and costs more
storage per row for a value written on every status change.
An explicit code decouples both: the enum can be reordered and its constants renamed freely,
because the only thing the database ever sees is the number.
Decoding tolerantly rather than throwing keeps one unrecognised row from making a report
unreadable, and `NOT_STARTED` is the deliberate landing spot — it is one of the two statuses the
housekeeping sweep acts on
([SW-018](SW-018-stale-reports-time-out-not-deleted.md)), so a corrupt or
future-versioned row is eventually timed out and made visible instead of lingering as a silently
wrong verdict.

**Verification Description**
A test asserts that each status constant decodes from and encodes to its documented numeric code,
that a code matching no constant decodes to `NOT_STARTED`, and that a freshly built report and API
test both carry the `IN_PROGRESS` code.
`ReportStatusUnitTest` pins the per-constant codes and the unknown-code fallback, and
`QualityGateReportUnitTest` covers the round trip through a report's persisted column.
An equivalent test pins the finding-status codes and asserts an unrecognised code decodes to
`NOT_APPLICABLE`; a serialisation test asserts the read API emits the status name, not the code.

## Relations

**Related**

- [SW-015](SW-015-report-status-aggregates-with-sticky-terminal.md) — the status
  derivation whose result this encoding stores
- [SW-018](SW-018-stale-reports-time-out-not-deleted.md) — the sweep that selects on
  these codes and catches the tolerant-decode fallback
- [NF-003](NF-003-scalability-without-redesign.md) — the schema stability this encoding
  preserves across releases
- [ARCH-012](ARCH-012-findings-on-the-event-coverage-as-cache.md) — the finding rows this
  encoding now also governs, and where storage cost per row actually bites
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — why `NOT_APPLICABLE` is the safe
  landing spot for an undecodable finding status

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
- **2026-09-22** — Extended from report and API-test status to finding status (`STR-017`), with
  `NOT_APPLICABLE` as that enum's tolerant-read landing spot and names — not codes — on the read
  API.
  Findings are the highest-cardinality rows the system writes, which is what makes the code
  rather than the name worth paying for here; the tolerant read is also what keeps `#2009` adding
  a `WAIVED` constant cheap.
