# A waiver enters only on the calculation request, naming one exact target with a reason and an expiry

<!-- markdownlint-disable MD036 -->

**Title**
A waiver enters only on the calculation request, naming one exact target with a reason and an expiry

**Lens**: SW

**Status**: planned

**Description**
`QualityGateCalculationRequest` gains an optional `waivers` array.
It is the only way a waiver reaches a calculation: no query parameter, no header, no gate property,
and no separate endpoint supplies one.

Each entry names, all required:

- the API it applies to — `serviceName` and `apiName`, each an exact value, and `apiVersion`, either
  an exact value or the single wildcard `'*'` meaning every version of that API,
- the target — `path` and `method`, written as the specification writes them, plus exactly one of
  `responseCode`, `parameterName` or `contentType` where the criterion's target is narrower than an
  operation,
- `reason`, a non-blank free-text justification,
- `expires`, a date.

`criterion` is optional; `SW-042` defines what naming it does and what omitting it does.

The array is validated at the trigger, before any calculation exists: an entry missing `reason` or
`expires`, carrying a blank `reason`, carrying more than one target discriminator, or naming an API
that is not among the report's API tests is a `400`, and no report is created
(`SW-013`).
An entry whose `apiVersion` is `'*'` is matched on `serviceName` and `apiName` alone, both at that
rejection and at the per-API dispatch of `ARCH-004`: it reaches the calculation of every version of
that API in the report, and an unknown service or API name is still a `400`.
`waivers` is absent rather than empty when none were given (`CON-010`).

The accepted document is recorded on `ReportParameter`, beside the lookback window and the attribute
filters that calculation was told, and is served back on every report read through
`QualityGateReport.calculationRequest`.
A report therefore always answers which waivers it was calculated with, including after the pipeline
that submitted it has changed.

**Rationale**
The request body is where the other two inputs of this kind already live.
`lookbackWindow` and `attributeFilters` are per-calculation instructions that shape what gets judged,
are recorded on `ReportParameter`, and are echoed with the report; a waiver is the same kind of
thing, and putting it anywhere else would mean a second mechanism for the same category of input.
It also gets the determinism of `SYS-014` for free: the waiver set is part of the request, so
re-running the request re-runs the waivers.

Admitting `'*'` on `apiVersion` and nowhere else is a bounded relaxation, not a general wildcard
grammar.
A waiver that had to name the exact version would have to be re-edited on every release of an API
whose gap has not closed, and a waiver file that must be touched each release is one that gets
rubber-stamped — which costs more honesty than the strictness buys.
The cost it does carry is real and is accepted: `SW-029`'s pointer is a pointer into one version's
document, so a `'*'` waiver carries a judgement made against one release into the next, including the
release where the behaviour became provokable.

Two independent controls bound that, and a wildcard waiver has to clear both.
`expires` is mandatory on every entry, so no wildcard survives its own date, and `CON-011` fails
the calculation when a waiver matches no finding — so a stale wildcard whose pointer the next version
no longer emits turns the build red rather than going quiet.
What is left over is the narrow case where the pointer still exists and the behaviour started being
provoked, which the expiry date exists to force back into review.
Overriding both deliberately is a choice a team makes in a reviewed file, with its name on it.

Restricting the wildcard to `apiVersion` keeps that choice legible.
`'*'` on `serviceName` or `apiName` would be an exemption nobody could scope by reading it, and the
narrow discriminators below stay exact for the same reason: a wildcard `path` or `responseCode` would
waive a target space rather than a target, which is the blast radius this spec exists to bound.

One target discriminator rather than several is what keeps a waiver's blast radius legible.
`ApiTestFinding` carries five discriminators but a criterion's target is addressed by at most one of
the narrow three — a response code, a parameter, or a content type — and accepting two at once would
describe a target no criterion enumerates.
Rejecting it at the trigger turns an unanswerable waiver into a message at the moment the author can
still fix it.

Rejecting an unknown API at the trigger follows the same logic one level up, and uses information the
trigger already has: the report's API tests are resolved before dispatch, so a waiver for a service
that is not in this calculation is a typo the system can name immediately rather than a silent no-op.
`SW-013` already makes the trigger all-or-nothing, so the rejection costs no partial report.

Recording the document on `ReportParameter` rather than deriving it back from findings is what makes
a report auditable.
The applied waivers are visible in the findings, but the ones that were _asked for_ — including their
reasons — are an input, and an input that is not persisted cannot be reviewed six months later when
somebody asks why a criterion stopped failing.
`calculationRequest` being `required` on the report component means this costs no new read contract.

**Verification Description**
A contract test triggers a calculation with a `waivers` array and asserts the report read answers it
back under `calculationRequest`, with reasons and expiry dates intact.
A test asserts a report triggered without waivers omits the property entirely rather than serving an
empty array or `null`.
Tests assert `400` and no created report for: a missing `reason`, a blank `reason`, a missing
`expires`, two target discriminators on one entry, and an API block matching no API test of the
report.
A test asserts an `apiVersion` of `'*'` is accepted and reaches the calculation of every version of
that API in the report, while an unknown `serviceName` or `apiName` alongside it is still a `400`.
A test asserts `'*'` is rejected as a `serviceName`, an `apiName`, a `path`, a `method` and each
narrow discriminator, so the wildcard is admitted on `apiVersion` only.
A test asserts a `'*'` entry is subject to the same mandatory `expires` as an exact-version entry.
A test asserts the persisted `ReportParameter` holds the document and that re-reading an old report
answers the waivers it was calculated with, not the ones a later calculation used.
A test drives `toolkit/cli`'s generated client over the widened request and the widened report read,
confirming the addition is additive for the client this repo generates.

## Relations

**Related**

- [SYS-014](SYS-014-exemption-is-an-input-never-stored-state.md) — the capability this
  intake delivers
- [SW-042](SW-042-criterion-narrowing-reaches-the-containing-criteria.md) — what the
  optional `criterion` means
- [SW-041](SW-041-waiver-resolves-against-the-emitted-findings.md) — how the named target is
  matched once the calculation runs
- [SW-043](SW-043-expiry-judged-against-the-triggering-instant.md) — what `expires` is
  compared against
- [SW-013](SW-013-calculation-trigger-is-all-or-nothing.md) — the all-or-nothing trigger a rejected
  waiver relies on
- [SW-029](SW-029-finding-identified-by-spec-pointer.md) — the version-bound pointer a waiver selects
  through, which is what makes `'*'` on `apiVersion` a bounded relaxation and keeps every narrower
  field exact
- [ARCH-004](ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — amended: the dispatch that
  sends each API test only its own waivers
- [SW-031](SW-031-findings-served-with-the-report.md) — the read that publishes the resulting waived
  findings
- [CON-010](CON-010-rest-responses-never-carry-null.md) — why an absent `waivers` is omitted
- [SYS-011](SYS-011-bounded-time-resolution.md) — the sibling per-calculation time input recorded on
  the same entity
