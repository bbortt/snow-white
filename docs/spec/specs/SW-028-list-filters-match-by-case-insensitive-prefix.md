# Service-name and API-name list filters match by case-insensitive prefix

<!-- markdownlint-disable MD036 -->

**Title**
Service-name and API-name list filters match by case-insensitive prefix

**Lens**: SW

**Status**: active

**Description**
The `serviceName` and `apiName` query parameters on `api-index-api`'s ingested-APIs listing filter
by case-insensitive **prefix** — a value matches only entries whose corresponding field starts with
it, not entries that merely contain it or match it exactly.
Both filters combine (a request naming both narrows by both).

**Rationale**
A prefix match supports the incremental-typing lookup a UI or CLI autocomplete needs (narrowing a
list as a caller types the start of a name), which a substring match would either not support well
or would over-match for, and which an exact match would defeat entirely.
Prefix was chosen over substring specifically so a short, common fragment (for instance a single
letter) does not return every entry containing it anywhere in the name.

**Verification Description**
A test indexes entries with overlapping and distinct service/API names, then queries the list with
a `serviceName` and an `apiName` value that is a true prefix of some entries and a substring-but-
not-prefix of others (in a different case than stored).
Only the entries whose corresponding field starts with the given value, case-insensitively, are
returned.

## Relations

**Related**

- [SYS-002](SYS-002-indexed-spec-lookup.md) — the addressing scheme this listing queries against

## Changes

- **2026-09-19** — Set active: implementation of `STR-016` began.
</content>
