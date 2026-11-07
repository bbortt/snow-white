# Response-schema conformance judges the responses a harness vouched for, and no others

<!-- markdownlint-disable MD036 -->

**Title**
Response-schema conformance judges the responses a harness vouched for, and no others

**Lens**: SW

**Status**: active

**Description**
`RESPONSE_SCHEMA_CONFORMANCE` is a fifteenth `OpenApiCoverageCriteria` constant, and the first that
judges what an API answered rather than whether it was reached.

Its target is one documented response entry of one documented operation carrying a conformance
verdict ([SW-049](SW-049-conformance-verdict-attributes.md)) — the unit
[SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md) already enumerates, matched to
an observed status code by that rule and `default` included, so a code observed under two concrete
paths of one templated operation is one target between them.
Required is every such pair whose telemetry carries a verdict.
Covered is a pair whose every verdict was conforming: one violation anywhere in the pair's telemetry
leaves the pair uncovered, because a schema the API breaks sometimes is a schema the API does not
keep.

A verdict naming an observed status code no declared entry matches is not a target of this criterion
and moves no ratio of it: a response the document does not declare has no documented schema to be
judged against, so there is nothing to judge it for and no node a spec pointer could address.
The traffic itself is [SW-003](SW-003-undocumented-response-code-detection.md)'s to report, and a
second criterion reporting it would count one gap twice.
What this criterion owes such a verdict is that it was read and left uncounted, which it states in
`additionalInformation`, naming every undeclared response a verdict arrived for — otherwise an
operator who published one and received a ratio taken over something else cannot learn the
difference from the report.
A documented `default` entry declares every response the operation can answer with, so nothing an
operation observed can go undeclared where one is stated.

A pair with no verdict is a target the criterion does not judge
([SW-030](SW-030-unjudged-target-is-not-applicable.md)), never an uncovered one.
So a suite that judges some responses is measured over those alone: a partial rollout of a producer
is a legitimate state, not a defect.
It also means the criterion's own denominator moves as a consumer adopts the harness, which is
correct and is why the ratio must never be read as a share of the API.

**No verdict at all is a failure, not a pass.**
Where the document declares at least one (operation, response code) pair and no pair carries a
verdict, the criterion emits one further finding: `UNCOVERED`, at the pointer `#/paths` — the root of
its own target space — with no evidence and every location field null.
It stands for "no response of this API was judged", and it is emitted only in that case, never
alongside a judged pair.
The per-pair findings are still all `NOT_APPLICABLE`, so the list still enumerates the target space
(`SW-030`); the derivation (`ARCH-010`) then reports `0` over `1` judged target.
A document declaring no pair at all has an empty target space and emits nothing, so it keeps
`CON-004`'s vacuous `1`.

Without that finding every pair would be inapplicable, the judged count zero, and `CON-004` would
report `1.00` — a green pass for a check that never ran, which is the shape
[CON-012](CON-012-criterion-emptied-by-waivers-is-never-a-coverage-pass.md) already forbids for
waivers.
The finding fails the criterion through the existing derivation, so no rule of `CON-004` or
[CON-009](CON-009-coverage-agrees-with-findings.md) is amended: the stored ratio still equals the
ratio the findings imply.
It is the one target of this criterion no waiver can address, and the remedy for a producer not yet
wired up is to unconfigure the criterion rather than to waive its root.
A waiver names its target by the location fields the finding carries — path and method, plus at most
one of response code, parameter name or content type
([SW-041](SW-041-waiver-resolves-against-the-emitted-findings.md)) — and this finding carries none of
them; a waiver has no pointer field an author could write `#/paths` into.
Making it waivable would mean a second addressing language for waivers, derived from the
specification, which is the one thing `SW-041` exists to forbid.
`SW-016` already answers the case: a criterion a gate does not name decides nothing, so a partial
rollout is handled by one line of gate configuration.
That is also the more honest report — a waived root would read "Fully waived", claiming somebody
decided this API need not be judged, where what happened is that nobody judged it.
Attempting the waiver instead is worse than ineffective: an entry that matches no finding fails the
whole calculation as an unmatched waiver, across every criterion of it, not only this one.

That result also carries an `additionalInformation` text saying that no verdict matched a documented
response of this API, naming both ways that happens — no harness published one, or the verdicts
published matched no response the document declares, whether the status code or the operation is the
undeclared one — pointing at
[SW-003](SW-003-undocumented-response-code-detection.md)'s findings for the second, saying that
responses are judged only where a test harness reports them, that Citrus does so through
`citrus-junit-jupiter-extension` and any other framework can through the producer contract
([SW-049](SW-049-conformance-verdict-attributes.md)), and linking `semantic-convention/openapi.md`.
The text states what is true in both cases rather than asserting the first: a verdict naming a code
the document does not declare satisfies no documented pair, so every pair stays `NOT_APPLICABLE` and
this finding is emitted while verdicts exist — and a text claiming none were found would send an
operator looking for a producer that is already running.
`additionalInformation` is already published on every surface a result appears on, so the
explanation travels to the UI, the JUnit export and the CLI without a new field.
The text is emitted only in the no-verdict case, and where verdicts arrived for undeclared responses
the sentence naming them follows it — the second half says which responses they were, so the reader
is not left to guess which of the two causes applies.

Each violation becomes one finding, identified by the JSON Pointer the verdict carried, resolved
against the response schema in the document through the existing pointer ladder
([SW-029](SW-029-finding-identified-by-spec-pointer.md)) —
`#/paths/<path>/<method>/responses/<code>/content/<mediaType>/schema` with the verdict's pointer
appended where it resolves, and the nearest resolvable ancestor where it does not, down to the
response entry itself.
The media type is part of the pointer because the schema is declared under it; a verdict names no
media type of its own, so the first one whose schema the violation resolves in is the one published.
A resolved pointer longer than a finding is stored under walks one node further up as well, because
a pointer no report can hold would cost the whole result rather than one finding's precision.
Several violations on one pair are several findings, discriminated by their resolved pointers; two
pointers that resolve to the same node are one finding.

**The unresolved `$ref` is the ordinary case, not an edge case.**
The document is parsed without dereferencing, so a response schema declared as
`$ref: '#/components/schemas/<name>'` carries only its reference and no walkable node: every
violation pointer on such a pair resolves nowhere, every finding collapses to the response entry,
and because findings are grouped by resolved pointer, several distinct breaks become one finding.
Most real specifications `$ref` their response schemas, so this is what most consumers will see, and
per-pointer precision is the exception rather than the rule.
The pair is still uncovered and the failure is still reported, but what is lost is not only
precision: the derivation counts findings, so the same API scores differently depending on whether
its response schemas are inlined or referenced — one pair covered beside one pair breaking three
pointers is `0.25` inlined and `0.50` referenced.
Nothing may be planned around one finding per violation, and a ratio of this criterion is
comparable only between documents of the same shape.
Dereferencing is not the remedy: it would change the document every criterion measures and publish
pointers addressing nodes the source document does not literally contain, which is the premise
`SW-029` rests on.
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

An unjudged pair is inapplicable, but a criterion with no judged pair at all fails, and the two
rules answer different questions.
Harvested conformance exists only where a harness publishes it
([ARCH-023](ARCH-023-conformance-verdict-is-harvested-not-recomputed.md)), so a pair without a
verdict says nothing about the API, and counting it uncovered would punish a partial rollout.
A whole API with no verdict on any documented pair says something about the setup instead: an
operator who included this criterion in a gate asked for the check, and receiving nothing means the
producer is missing or misconfigured — a REST-assured suite with no producer, an exporter pointed
nowhere, a collector filter dropping the spans — or else publishing verdicts for responses this
document never declared, which `SW-003` reports and this criterion cannot judge.
Passing that case would report the opposite of what happened.
Failing it costs no consumer who did not ask: the criterion is in no shipped gate, and a criterion
outside a gate decides nothing ([SW-016](SW-016-api-test-verdict-is-gate-scoped.md)).

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
A test asserts telemetry carrying no verdict at all, over a document declaring pairs, reports every
pair `NOT_APPLICABLE`, one `UNCOVERED` finding at `#/paths`, coverage `0` over one judged target, and
an `additionalInformation` naming the producer contract.
A test asserts a document declaring no pair emits no finding and reports `1`.
A test asserts that once a single pair carries a verdict, the `#/paths` finding and the text are
absent and the unjudged pairs stay `NOT_APPLICABLE`.
A test asserts the `#/paths` finding carries no path, method or response code, so no waiver can
address it.
A test asserts a verdict on an observed status code no declared entry matches emits no finding and
moves no ratio, and that `additionalInformation` names that response beside the no-verdict text.
A test asserts a documented `default` entry catches such a response, so nothing is reported as
undeclared.
A test asserts two violations at different pointers on one pair are two findings, and the same
pointer twice is one.
A test asserts a pointer that does not resolve in the document falls back to the shortest resolvable
ancestor rather than being dropped.
A test asserts that over a response schema the document declares as a `$ref` to a component, two
violations at different pointers collapse to one finding at the response entry, pinning the
unresolved `$ref` as the ordinary case.
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
- [CON-009](CON-009-coverage-agrees-with-findings.md) — why the no-verdict failure is a finding
  rather than an amended ratio
- [CON-012](CON-012-criterion-emptied-by-waivers-is-never-a-coverage-pass.md) — the precedent: a
  pass with nothing judged behind it must not read as coverage
- [SW-041](SW-041-waiver-resolves-against-the-emitted-findings.md) — the addressing language that
  leaves this criterion's root finding unwaivable
- [ARCH-016](ARCH-016-criteria-containment-declared-on-the-enum.md) — the containment families this
  criterion joins none of
- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — the gate scoping that makes arriving
  unconfigured a no-op
- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — the derivation this calculator plugs into
- [ARCH-011](ARCH-011-evidence-captured-at-the-match.md) — where the violation's evidence is taken
