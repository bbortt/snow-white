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

## Relations

**Related**

- [SW-015](SW-015-report-status-aggregates-with-sticky-terminal.md) — the status
  derivation whose result this encoding stores
- [SW-018](SW-018-stale-reports-time-out-not-deleted.md) — the sweep that selects on
  these codes and catches the tolerant-decode fallback
- [NF-003](NF-003-scalability-without-redesign.md) — the schema stability this encoding
  preserves across releases

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
