# A report publishes the threshold it was scored against, pinned, on every read

<!-- markdownlint-disable MD036 -->

**Title**
A report publishes the threshold it was scored against, pinned, on every read

**Lens**: SW

**Status**: planned

**Description**
Every read that answers a quality-gate report carries `minCoveragePercentage`: the paginated list
read, the single-report read, and the `202` of the calculation trigger and of the JUnit poll.
It is a property of the shared report component and of its findings-carrying fork, so the four
shapes agree on it.

The value is the one **pinned on the report** when the calculation was triggered — the same number
`SW-016` scores the report with and `SW-017` exports — not the one the gate carries at the moment of
the read.
A gate edited, renamed or deleted after a calculation does not change what that report's read
answers.

It is `required`, and an integer in `[80, 100]`, mirroring the stored column: `NOT NULL`,
non-updatable, with a check constraint.
No report can be served without it, so no consumer has to default it.

What is _not_ published is a per-criterion verdict.
A criterion result keeps `coverage` and `isIncludedInQualityGate`, and a consumer that wants the
verdict applies `SW-016`'s comparison — `coverage >= minCoveragePercentage / 100` over the included
results — itself.

**Rationale**
A report that does not publish its bar cannot be read correctly by anyone but the service that
stored it.
Both existing consumers proved it: `toolkit/cli`'s agentic summary substituted inclusion for
failure, reporting every included criterion as a failure on a passing gate, and the webapp
substituted the **live** configuration's number — the reading `SW-016` explicitly rejects, and one
that also couples a historical report's detail view to the continued existence of the gate it was
run under.
Neither consumer was careless; neither had the number.

The threshold rather than a verdict flag is the part worth pinning here.
The comparison is a single `>=` already fixed by `SW-016`, so publishing it as a boolean would
restate the rule in a second place that can drift from the first, while the number restates nothing.
The number is also what a reader needs in order to _explain_ a verdict — "0.9 against a bar of 0.8"
is the sentence a drilldown and an agent's next action are both built from, and a boolean erases the
margin.
It is what the webapp's coverage bar already draws its marker from, and what the JUnit export
already publishes as a suite `<property>` to the very same consumers; putting it on the JSON reads
makes the two artifacts answer from one source rather than two.
Declining the flag is also the cheaper direction to be wrong in: adding a derived field later is
additive, removing a published one is not.

Declaring it `required` rather than optional is what makes the derivation safe to write.
An optional bar forces every consumer into a default, and the only available default — `100` — is
the strictest bar there is, so a consumer that silently fell back to it would report failures the
gate passed, which is the defect this spec exists to remove.
The storage contract supports the stronger declaration: the column has been `NOT NULL` with a
default since the migration that introduced it, so there is no population of reports the server
would have to omit it for, and `CON-010`'s omit-nulls rule never engages.

**Verification Description**
A contract test reads a report through all four shapes — the list read, the single-report read, and
both `202` responses — and asserts each carries the report's `minCoveragePercentage`.
A test persists a report under a gate at one threshold, changes the gate's `minCoveragePercentage`,
and asserts the report's read still answers the pinned value.
A test asserts a report whose gate has since been deleted still answers its threshold.
A test asserts the property is absent from no report the service serves — including a report
persisted before this change, which the migration's `NOT NULL DEFAULT 100` guarantees a value for.
A test drives `toolkit/cli`'s generated client over the widened `200` and `202`, confirming the
addition is additive for the client this repo generates, and that a consumer applying `SW-016`'s
comparison to the published pair reproduces the `<failure>` set of the same report's JUnit export.

## Relations

**Related**

- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — the rule this makes computable, and the
  pinning this read honours
- [SW-017](SW-017-junit-export-mirrors-the-gate-verdict.md) — the export that already publishes
  this number as a suite property, and whose `<failure>` set a consumer can now reproduce
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the `202` poll shapes that gain the
  property
- [SW-031](SW-031-findings-served-with-the-report.md) — the forked report component the property
  joins alongside the shared one
- [SYS-009](SYS-009-machine-consumable-gate-result.md) — the machine-consumable result this
  completes
- [SYS-012](SYS-012-result-consumption.md) — the result-consumption capability it serves
- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the bounded ratio the
  published bar is compared against
- [CON-010](CON-010-rest-responses-never-carry-null.md) — unengaged here by construction: the
  value is never null, so it is never omitted
- [ARCH-006](ARCH-006-report-status-persisted-as-stable-code.md) — the sibling storage-versus-read
  divergence this property deliberately does not need
