# A criterion is exempted for one target by an input to the calculation, never by state the system holds

<!-- markdownlint-disable MD036 -->

**Title**
A criterion is exempted for one target by an input to the calculation, never by state the system
holds

**Lens**: SYS

**Status**: planned

**Description**
A criterion can be declared inapplicable to a single target of a single API — one response code of
one operation, one parameter, one content type — while the same criterion keeps judging every other
target of that API.
The declaration is a **waiver**, and it is an input to the calculation.
It arrives with the request that triggers the calculation, is recorded as part of what that
calculation was told, and is answered back with the report.

The system holds no waiver state of its own.
There is no waiver resource to create, no approval to grant, and no setting on a quality gate that
suppresses a criterion for a target.
A calculation that is not given a waiver has none, whatever an earlier calculation was given.

Every waiver carries a reason and an expiry.
Neither is advisory: a waiver without either is not a waiver this system accepts.

A waiver reduces the target space a criterion judges; it never produces a verdict.
A criterion all of whose judged targets are waived has no coverage verdict left, and every surface
that publishes it says so: a result resting on waivers stays distinguishable from one resting on
coverage (`CON-012`), rather than reading as a criterion that was satisfied.

**Rationale**
The capability `SYS-006` grants today is all-or-nothing at the criterion level: a quality gate either
includes a criterion for an API or it does not.
That is the right granularity for "we do not measure this yet" and the wrong one for "this one
response code cannot be provoked".
Teams meeting the second case have to answer it with the first lever, which drops the criterion
across every endpoint it was working for — the gate gets weaker everywhere in order to go green in
one place.

Making the exemption an input rather than stored state is the decision this spec exists to fix in
place, and it follows from `CON-001`.
A report's verdict has to be a function of what the calculation was given.
A waiver living in a server-side store would make two identical calculations answer differently
because someone edited a record between them, and would make a historical report's re-derivation
depend on state that has since moved.
It would also need a lifecycle the system has no owner for — who may create one, who approves it,
what happens to in-flight reports when one is deleted — which is a governance system, not a coverage
one.
As an input, a waiver is reviewed where every other change to a build is reviewed: in the repository
that submits it.

The mandatory reason and expiry exist because the failure mode of this feature is not a wrong number,
it is a forgotten one.
An exemption with no stated reason is indistinguishable from an exemption nobody can justify, and an
exemption with no end date is a permanent reduction in scope that no one will ever be prompted to
revisit.
Requiring both costs the submitter one line each and is the only part of the mechanism that cannot be
added later without invalidating every waiver written before it.

Declining to let a waiver produce a verdict is what keeps the feature from becoming a way to pass.
A waiver answers "this target is not judged"; it never answers "this target is fine".
The distinction is invisible in a ratio, which is why the counts and the honesty rule are part of the
same capability rather than a later refinement of it.

**Verification Description**
An acceptance test triggers a calculation with a waiver for one response code of one operation,
asserts the criterion no longer fails, and asserts every other target of the same criterion is still
judged — including a second uncovered target, which still fails.
A test triggers the same calculation without the waiver and asserts the original verdict returns,
establishing that nothing was retained between the two.
A test asserts a waiver missing its reason, and one missing its expiry, are both rejected at the
trigger.
A test asserts no API accepts, stores or lists a waiver outside a calculation request, and that no
quality-gate field suppresses a criterion for a target.
A test asserts a criterion whose every judged target is waived is not reported as satisfied.

## Relations

**Related**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the criterion-level evaluation this
  narrows to a target
- [SYS-008](SYS-008-quality-gate-definitions.md) — the gate-level inclusion lever this
  deliberately does not reuse
- [SYS-010](SYS-010-analysis-triggering.md) — the trigger a waiver arrives on
- [SYS-009](SYS-009-machine-consumable-gate-result.md) — the result that has to make a
  waived pass visible
- [CON-001](CON-001-deterministic-analysis-results.md) — the determinism that forces an
  input rather than a store
- [CON-003](CON-003-predefined-quality-gates-are-immutable.md) — the immutability a
  gate-side waiver would have collided with
- [SW-040](SW-040-waiver-enters-only-on-the-calculation-request.md) — the request property
  this capability is delivered through
- [CON-012](CON-012-criterion-emptied-by-waivers-is-never-a-coverage-pass.md) — the honesty
  rule that keeps a waiver from becoming a verdict
