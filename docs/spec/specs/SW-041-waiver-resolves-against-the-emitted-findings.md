# A waiver resolves against the findings the calculation emitted, never by matching paths a second time

<!-- markdownlint-disable MD036 -->

**Title**
A waiver resolves against the findings the calculation emitted, never by matching paths a second time

**Lens**: SW

**Status**: planned

**Description**
A waiver is resolved by selecting from the findings a criterion has already produced.
The calculator enumerates its target space and emits one finding per target (`SW-030`); the waiver
pass then selects the findings whose identity matches the waiver and rewrites their status.

Identity is the finding's own addressing, not the waiver's text:

- `path` and `method` match the finding's `httpPath` and `httpMethod`,
- `responseCode`, `parameterName` or `contentType`, when given, match the finding's discriminator of
  the same name,
- the criterion scope is `SW-042`'s.

No component re-reads the OpenAPI document to interpret a waiver, re-derives a spec pointer from
`path` and `method`, or applies path templating, parameter-token or wildcard rules of its own.
A waiver that does not match a finding by this selection does not match, and `CON-011` says what
happens then.

A matched finding becomes `WAIVED` and carries the waiver's reason and expiry.
`COVERED` findings are matched and rewritten like any other: a waiver removes a target from
judgement, and a target that happened to be covered anyway is still removed, so the published counts
describe the waiver that was applied rather than the one that turned out to matter.

**Rationale**
The matching this feature needs already exists and is already hard.
`SW-002`'s `default` wildcard, `SW-004`'s token-not-substring parameter matching and the path
templating under them are the interpretation rules of this system, and they are implemented once, in
the calculators.
A waiver that carried its own matcher would be a second implementation of the same rules, diverging
the first time one of them is fixed — and diverging silently, because the symptom is a waiver that
stops suppressing or starts suppressing too much.
Resolving against emitted findings means a waiver inherits every interpretation rule the criterion
uses, including the ones added after the waiver was written.

`SW-030` is what makes this possible rather than merely tidy.
Because a criterion emits a finding for every target it enumerated — including the ones it does not
judge — the finding set _is_ the target space, so a waiver has something complete to select from and
a miss is a real signal rather than a timing artifact.
Without the complete enumeration, "no finding matched" would be ambiguous between a wrong waiver and
a target that simply produced nothing.

Rewriting `COVERED` findings too is the less obvious half, and it is a determinism requirement.
If a waiver only applied to findings that were failing, the same request against the same
specification would produce a different _waiver_ outcome depending on which traces happened to land
in the lookback window — the waiver would be applied on a bad day and silently skipped on a good
one, and the published waived count would move with the telemetry.
Making application independent of status keeps `CON-001` intact and makes a stale waiver visible: it
keeps being reported as applied, which is exactly the prompt its expiry exists to create.
It also means a waiver never improves a ratio it was not needed for, because a covered target leaving
the fraction cannot raise it.

The status rather than a side table is where the waiver verdict belongs because the finding is what
every downstream consumer already reads.
`ARCH-010`'s derivation, `SW-031`'s read and `SW-017`'s export all walk findings; a parallel waiver
record would have each of them join two collections and agree about the join.

**Verification Description**
A calculator test emits findings for a known target space, applies a waiver naming one target, and
asserts exactly that finding became `WAIVED`, with the reason and expiry attached, and that every
other finding is untouched.
A test asserts a waiver written against a templated path (`/pets/{petId}`) matches the finding the
calculator emitted for it, with no separate templating logic involved.
A test asserts a waiver naming a response code the specification declares as `default` resolves the
way `SW-002` resolved it when the finding was emitted.
A test asserts a waiver whose target is `COVERED` still produces a `WAIVED` finding, and that the
criterion's coverage is unchanged or lower, never higher, as a result.
A test asserts no code path outside the calculators reads the OpenAPI document in order to resolve a
waiver.

## Relations

**Related**

- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — amended: the complete enumeration this
  selection depends on, and the distinction between waived and inapplicable
- [SW-029](SW-029-finding-identified-by-spec-pointer.md) — the finding identity a waiver selects
  through
- [CON-011](CON-011-waiver-matching-no-finding-fails-the-calculation.md) — what a
  non-matching waiver does
- [SW-042](SW-042-criterion-narrowing-reaches-the-containing-criteria.md) — the criterion
  half of the selection
- [ARCH-017](ARCH-017-waivers-applied-before-the-one-derivation.md) — where in the
  calculator this pass runs
- [SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md) — one of the interpretation
  rules a waiver inherits rather than reimplements
- [SW-004](SW-004-parameter-coverage-matches-by-token-not-substring.md) — another
- [CON-001](CON-001-deterministic-analysis-results.md) — the determinism that forces application to
  ignore a finding's status
- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — amended: the derivation that consumes the
  rewritten findings
