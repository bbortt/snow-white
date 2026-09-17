# Four predefined gates ship with a fixed criteria set and threshold each

<!-- markdownlint-disable MD036 -->

**Title**
Four predefined gates ship with a fixed criteria set and threshold each

**Lens**: SW

**Status**: active

**Description**
`quality-gate-api` ships exactly four predefined gates, each with a fixed criteria set and
minimum coverage threshold:

| Name             | Threshold | Criteria                                                                                                                                                                                        |
| ---------------- | --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `basic-coverage` | 80%       | HTTP method, operation-success, positive-response-code, required-parameter coverage; no-undocumented-positive-response-codes                                                                    |
| `full-feature`   | 100%      | HTTP method, operation-success, response-code, parameter, content-type, required-error-fields coverage; no-undocumented-response-codes (7 of 14 — the other 7 are each implied by one of these) |
| `minimal`        | 80%       | Path coverage only                                                                                                                                                                              |
| `dry-run`        | 100%      | None — enforces no rules, for reports/tooling only                                                                                                                                              |

These are the values `DefaultOpenApiQualityGates` seeds; a caller sees them exactly this way
through the same `GET`/list contract as any other gate — nothing in the API surface marks them as
special beyond `isPredefined: true`.
No gate lists a criterion alongside one it already implies (`pages/_pages/quality-gate-criteria.md`
§ Criteria Relationships) — a parent's own coverage check subsumes its children's, so listing both
adds no additional guarantee, only a longer, misleading criteria list.

**Rationale**
Each predefined gate targets a distinct use case a team should be able to reach for by name
without composing one from criteria themselves: a pragmatic baseline (`basic-coverage`), the
strictest possible bar (`full-feature`), a minimal reachability check (`minimal`), and a
no-op gate for tooling or reporting that never blocks a pipeline (`dry-run`).
Pinning the exact composition here — rather than leaving it only in
`DefaultOpenApiQualityGates`'s source — is what lets a future change to any of the four be
recognized as a deliberate revision of this spec, not an accidental drift.

**Verification Description**
A test fetches all four predefined gates by name and asserts, for each, the exact criteria set
and threshold listed above — matching `DefaultOpenApiQualityGates`'s current seeding logic.

## Relations

**Related**

- [ARCH-003](ARCH-003-idempotent-ordered-startup-seeding.md) — how and when these are
  seeded
- [SW-009](SW-009-quality-gate-crud-contract.md) — the read contract these gates are
  served through

## Changes

- **2026-09-17** — Set active: anchored against `quality-gate-api`'s existing implementation
  (`STR-009`).
- **2026-09-17** — Revised `basic-coverage` and `full-feature`'s criteria lists, dropping every
  criterion a listed parent already implies (path coverage under HTTP method coverage;
  positive/error response-code coverage under response-code coverage; required/optional parameter
  coverage under parameter coverage; no-undocumented-positive/error-response-codes under
  no-undocumented-response-codes) — `full-feature` drops from 14 criteria to 7, `basic-coverage`
  from 6 to 5.
  The prior composition listed both sides of these pairs, which added no additional
  guarantee over the parent alone and only inflated the reported criteria count
  ([issue #2010](https://github.com/bbortt/snow-white/issues/2010), `STR-010`).
