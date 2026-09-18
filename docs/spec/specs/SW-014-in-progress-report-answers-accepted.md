# A report that is still running answers `202` with its partial body, not `200` and not an error

<!-- markdownlint-disable MD036 -->

**Title**
A report that is still running answers `202` with its partial body, not `200` and not an error

**Lens**: SW

**Status**: active

**Description**
Retrieving a quality-gate report by its calculation id has three outcomes, and the same three
apply to both the JSON and the JUnit XML representation:

- **`404`** — no report with that calculation id exists, with a `{code, message}` body naming the
  id.
- **`202`** — the report exists but its status is still `IN_PROGRESS`; the body is the report as
  it currently stands, in JSON, **including for the JUnit XML endpoint**.
- **`200`** — the report has reached any other status; the body is the full report in the
  requested representation, the JUnit XML variant additionally carrying a
  `Content-Disposition: attachment` header with a fixed file name.

Listing reports is a separate, always-`200` surface: it is paginated, filterable by service name,
API name, and API version, and carries the pagination totals in headers.

**Rationale**
A calculation is asynchronous, so a caller polls.
Answering `200` while the report is incomplete would make "finished" indistinguishable from "still
running" without inspecting the body, and answering an error would conflate "not ready yet" with
"something went wrong" — a distinction a CI pipeline must branch on to decide between waiting and
failing the build.
`202` separates the two on the status line alone, while still returning the partial report so a
caller can show progress without a second request.
The JUnit endpoint deliberately breaks its own content type for the in-progress case: emitting a
half-populated JUnit document would be read by a build server as a genuine, passing test run.

**Verification Description**
A test requests a report by an id that does not exist and asserts `404`; requests a report whose
status is `IN_PROGRESS` and asserts `202` with the report body on both the JSON and the JUnit
endpoint; and requests a completed report and asserts `200`, plus the `Content-Disposition`
attachment header and XML content type on the JUnit endpoint.
`ReportResourceUnitTest` and `ReportResourceIT` cover the status mapping, and
`ReportCoordinatorApiAppTest`'s full-lifecycle case observes the `202` → `200` transition against
a running service.

## Relations

**Related**

- [SYS-012](SYS-012-result-consumption.md) — the consumption capability these endpoints serve
- [SYS-011](SYS-011-bounded-time-resolution.md) — the guarantee that the `202` state is
  eventually left
- [SW-013](SW-013-calculation-trigger-is-all-or-nothing.md) — the trigger whose
  `Location` header leads here
- [SW-015](SW-015-report-status-aggregates-with-sticky-terminal.md) — the status this
  contract branches on
- [SW-017](SW-017-junit-export-mirrors-the-gate-verdict.md) — the JUnit document
  returned once the report is complete

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
