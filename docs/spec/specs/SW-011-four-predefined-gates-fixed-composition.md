# Four predefined gates ship with a fixed criteria set and threshold each

<!-- markdownlint-disable MD036 -->

**Title**
Four predefined gates ship with a fixed criteria set and threshold each

**Lens**: SW

**Status**: active

**Description**
`quality-gate-api` ships exactly four predefined gates, each with a fixed criteria set and
minimum coverage threshold:

| Name             | Threshold | Criteria                                                                                                                                                                                                                                                                            |
| ---------------- | --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `basic-coverage` | 80%       | Path, HTTP method, operation-success, positive-response-code, required-parameter coverage; no-undocumented-positive-response-codes                                                                                                                                                  |
| `full-feature`   | 100%      | HTTP method, operation-success, response-code, parameter, content-type, required-error-fields coverage; no-undocumented-response-codes (7 of 14 — 6 of the others are contained by one of these, and path coverage follows from HTTP method coverage at this gate's 100% threshold) |
| `minimal`        | 80%       | Path coverage only                                                                                                                                                                                                                                                                  |
| `dry-run`        | 100%      | None — enforces no rules, for reports/tooling only                                                                                                                                                                                                                                  |

These are the values `DefaultOpenApiQualityGates` seeds; a caller sees them exactly this way
through the same `GET`/list contract as any other gate — nothing in the API surface marks them as
special beyond `isPredefined: true`.
No gate lists a criterion alongside one that contains it, as `ARCH-016` declares containment — a
container's own coverage check subsumes the contained criterion's at every threshold, so listing
both adds no additional guarantee, only a longer, misleading criteria list.
Implication short of containment is not grounds for leaving a criterion out: it holds only at a
gate's own threshold, which is why `full-feature` can omit path coverage at 100% and
`basic-coverage` cannot at 80%.

The gates are also a ladder — `minimal` requires a subset of `basic-coverage`, which `full-feature`
covers in turn through containment and, for path coverage, through that 100% implication.

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
A second test asserts `minimal`'s criteria set is a non-empty subset of `basic-coverage`'s, which
is the one rung of the ladder expressible as a subset; the rung above rests on containment and on
the 100% implication, and is not asserted.

## Relations

**Related**

- [ARCH-003](ARCH-003-idempotent-ordered-startup-seeding.md) — how and when these are
  seeded
- [SW-009](SW-009-quality-gate-crud-contract.md) — the read contract these gates are
  served through
- [ARCH-016](ARCH-016-criteria-containment-declared-on-the-enum.md) — the declared containment that
  decides which of the pairs below is one

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
- **2026-10-07** — Withdrew one of the four pairs the entry above relies on, without changing any
  gate's composition: `ARCH-016` declares path coverage is not contained by HTTP method coverage,
  because the two judge different targets at different spec pointers.
  `full-feature` is unaffected — at its 100% threshold full method coverage does imply full path
  coverage, so dropping the entry still removes nothing it checked.
  `basic-coverage` is where the premise no longer reaches: below 100% the two ratios come apart,
  because a path counts once against path coverage and once per operation against method coverage.
  A specification with one path carrying twenty called operations and five single-operation paths
  never called reports 80% method coverage and 17% path coverage — passing this gate while five
  sixths of the API was never reached at all.
  Whether `basic-coverage` should list path coverage again is a composition decision this entry
  does not take; the table above is still what the seeder produces.
- **2026-10-07** — Took that decision: `basic-coverage` lists path coverage again, going from 5
  criteria back to 6.
  The deciding argument is not the one above but the ladder: `minimal` is path coverage alone, so
  leaving it out left a gate named "minimal" requiring something the pragmatic baseline above it
  never measured.
  Restoring it also puts the 17%-path case on the report instead of out of scope, though it does
  not by itself fail that case — `SW-016` passes a gate when the _share_ of criteria clearing the
  bar clears it too, and at six criteria and 80% that tolerates one failure, which path coverage
  would be.
  What the gate gains is that the blind spot is measured and visible, and that its one tolerated
  failure is now spent.
  Whether a single criterion at zero should be able to pass a gate at all is an `SW-016` question,
  untouched here.
  This is a breaking change for existing installations: `initPredefinedQualityGates` upserts
  predefined gates by name on every startup (`ARCH-003`), so an upgraded instance's
  `basic-coverage` gains the criterion without anyone asking for it.
  A pipeline that was green on exactly four of five criteria and has paths it never reaches now
  scores four of six, which is 67% against an 80% bar, and fails.
  `CON-003` forbids users mutating a predefined gate precisely so a pinned name keeps meaning one
  thing; changing one across a release owes them the same warning in return.
