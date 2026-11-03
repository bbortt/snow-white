# Response-schema conformance judges the responses a harness vouched for, and no others

<!-- markdownlint-disable MD036 -->

**Title**
Response-schema conformance judges the responses a harness vouched for, and no others

**Lens**: SW

**Status**: planned

**Description**
`RESPONSE_SCHEMA_CONFORMANCE` is a fifteenth `OpenApiCoverageCriteria` constant, and the first that
judges what an API answered rather than whether it was reached.

Its target is one (documented operation, observed status code) pair carrying a conformance verdict
([SW-049](SW-049-conformance-verdict-attributes.md)) — the same unit
[SW-003](SW-003-undocumented-response-code-detection.md) counts, so a code observed under two
concrete paths of one templated operation is one target between them.
Required is every such pair whose telemetry carries a verdict.
Covered is a pair whose every verdict was conforming: one violation anywhere in the pair's telemetry
leaves the pair uncovered, because a schema the API breaks sometimes is a schema the API does not
keep.

A pair with no verdict is a target the criterion does not judge
([SW-030](SW-030-unjudged-target-is-not-applicable.md)), never an uncovered one.
That is the load-bearing asymmetry: a response nobody validated is a response Snow-White has nothing
to say about, so a consumer whose suite publishes no verdicts sees the criterion as inapplicable
rather than as a red gate it did not ask for.
It also means the criterion's own denominator moves as a consumer adopts the harness, which is
correct and is why the ratio must never be read as a share of the API.

Each violation becomes one finding, identified by the JSON Pointer the verdict carried, resolved
against the response schema in the document through the existing pointer ladder
([SW-029](SW-029-finding-identified-by-spec-pointer.md)) — `#/paths/<path>/<method>/responses/<code>`
with the verdict's pointer appended where it resolves, and the shortest resolvable ancestor where it
does not.
Several violations on one pair are several findings, discriminated by their pointers; the same
pointer seen twice on the same pair is one finding.
The violation message is not a finding field: it is Citrus's and
`com.atlassian.oai.validator`'s wording, not Snow-White's contract, and persisting it would publish
a third party's prose as a stable surface.

A non-conforming response still counts as coverage for every criterion that judges reach.
A `200` that broke its schema was observed, so `RESPONSE_CODE_COVERAGE` and its relatives count it
covered while this criterion counts it uncovered.
The two disagree about the same response deliberately: one says the reach was taken, the other says
the answer was wrong, and collapsing them would make a conformance failure look like an untested
operation.

The criterion is not a member of any existing containment family
(`ARCH-016`): nothing is a subset of it and it is a subset of nothing.

It is added to no gate configuration this repository ships.
A new enum constant is available to be configured, and an operator opts into it exactly as into any
other criterion; a gate that does not name it is scored as before
([SW-016](SW-016-api-test-verdict-is-gate-scoped.md)).
Arriving pre-enabled would change a configured gate's verdict on an upgrade, which is the one thing
`CON-015` exists to prevent at the telemetry level and would be no better done through
configuration.

**Rationale**
Reach and conformance are different questions, and Snow-White answered only the first.
An API can be fully covered and still not keep its contract, which is a hole in the ground-truth
claim rather than a missing report column.

Making the unjudged case inapplicable rather than failing is what makes the criterion shippable at
all.
Harvested conformance exists only where a harness publishes it
([ARCH-023](ARCH-023-conformance-verdict-is-harvested-not-recomputed.md)), so absence is the
normal state for every consumer on day one; a criterion that read absence as failure would turn a
Snow-White upgrade into a red gate on an unchanged API, and consumers would disable it before ever
seeing it work.

Judging the pair rather than the individual response is what keeps the number comparable with the
response-code family it will be read beside.
A per-response ratio would let a thousand conforming calls bury one violation, which is the opposite
of what a gate is for — hence "one violation uncovers the pair".

Dropping the validator's message is the same restraint `SW-032` applies to a test name for the
opposite reason: there the value is opaque because Snow-White does not own the naming convention,
here it is dropped because Snow-White does not own the wording and could not keep it stable across a
dependency bump.
The pointer is the part that is Snow-White's own model and is already how a finding is identified.

**Verification Description**
A unit test on the new calculator asserts that telemetry for an operation documenting `200`, with
one span carrying a conforming verdict and one carrying a violation at a pointer, reports the pair
uncovered and produces one finding whose pointer resolves into the `200` response schema.
A test asserts two spans with conforming verdicts for the same pair report it covered.
A test asserts telemetry carrying no verdict at all reports the criterion inapplicable rather than
0%.
A test asserts two violations at different pointers on one pair are two findings, and the same
pointer twice is one.
A test asserts a pointer that does not resolve in the document falls back to the shortest resolvable
ancestor rather than being dropped.
A test asserts the same telemetry reports `RESPONSE_CODE_COVERAGE` 100% and
`RESPONSE_SCHEMA_CONFORMANCE` below it, for the response that was observed and non-conforming.
A test asserts no finding field carries the validator's message.

## Relations

**Realizes**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the evaluation this criterion joins

**Related**

- [SW-049](SW-049-conformance-verdict-attributes.md) — the verdict this criterion reads
- [ARCH-023](ARCH-023-conformance-verdict-is-harvested-not-recomputed.md) — where the
  verdict is produced, and why absence is normal
- [CON-015](CON-015-harness-span-never-moves-a-coverage-verdict.md) — the invariant that
  keeps this criterion's arrival additive
- [SW-029](SW-029-finding-identified-by-spec-pointer.md) — the pointer ladder a violation's finding
  uses
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — what an unvouched response becomes
- [SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md) — the reach criterion this
  one is allowed to disagree with about the same response
- [SW-003](SW-003-undocumented-response-code-detection.md) — the per-documented-operation target
  unit this criterion reuses
- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the shared formula
- [ARCH-016](ARCH-016-criteria-containment-declared-on-the-enum.md) — the containment families this
  criterion joins none of
- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — the gate scoping that makes arriving
  unconfigured a no-op
- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — the derivation this calculator plugs into
- [ARCH-011](ARCH-011-evidence-captured-at-the-match.md) — where the violation's evidence is taken
