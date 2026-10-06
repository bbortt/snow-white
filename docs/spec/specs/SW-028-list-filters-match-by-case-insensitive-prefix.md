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
- **2026-09-29** — Verification brought in line with the description above.
  The three anchored unit tests assert the pattern string `ApiReferenceSpecification` hands
  `CriteriaBuilder.like` (`"my-service%"`) against mocks, which pins the shape of the query but
  never executes it; the two integration tests that do run against the index only indexed an entry
  matching by prefix and an unrelated one, so both passed unchanged under `%value%` semantics, and
  were even named `shouldMatch…ContainingFilter` — the reading this spec exists to reject.
  They now also index an entry holding the filter value as a substring but not a prefix
  (`legacy-prefix-ingesting-service` against filter `Prefix`), which is what the verification
  description asked for all along, and they carry the anchor they were missing.
  Confirmed by temporarily widening the pattern to `%value%`: exactly those two integration tests
  fail, and only those two.
  `clew` reported this spec `covered` throughout, because coverage counts anchors and cannot read
  what an assertion is worth.
