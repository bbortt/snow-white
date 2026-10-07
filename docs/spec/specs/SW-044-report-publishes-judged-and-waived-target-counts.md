# Every criterion result publishes how many targets it judged and how many were waived, on every read

<!-- markdownlint-disable MD036 -->

**Title**
Every criterion result publishes how many targets it judged and how many were waived, on every read

**Lens**: SW

**Status**: planned

**Description**
A criterion result carries two integers beside its `coverage`:

- `judgedTargetCount` — the targets that entered the fraction, the denominator `ARCH-010`'s reduction
  used,
- `waivedTargetCount` — the targets removed from it by a waiver.

They are properties of the shared criterion-result component and of its findings-carrying fork, so
the paginated list read, the single-report read and the `202` of both the calculation trigger and the
JUnit poll agree on them, as `SW-039` made the threshold agree.

They are derived by the same single reduction that derives the ratio and are persisted beside it
(`ARCH-012`), never recomputed on a read.

They are **not** `required`.
A result persisted before this change has no counts and serves neither key rather than serving a
false `0` (`CON-010`).
Every result the current pipeline produces carries both, which is a guarantee about what the server
serves, verified by test — the same shape `SW-031` uses for `findings`.

A waived finding is served with its justification: `waiverReason` and `waiverExpiresOn` on the
finding, `WAIVED` joining its `status` enum (`SW-031`).
A finding that is not waived carries neither key.

The JUnit export and `--agentic` carry the same two numbers, so the three surfaces of one report
cannot disagree about what was waived.
`AGENTIC_SCHEMA_VERSION` stays `'1'`.
The counts are additive keys on a payload no integration consumes yet, so a bump would signal a break
to nobody and spend the first version step of the field on a change that cannot break anything.
The next version step is reserved for a change that removes or reshapes a key an integration reads.

**Rationale**
`CON-012`'s invariant is unenforceable without these numbers, because the ratio cannot carry it.
`coverage: 1.00` is produced by an API with no targets, by an API at genuine full coverage, and by an
API whose every target was waived, and `CON-004`'s formula gives the three the same value by design.
The counts are the smallest addition that separates them, and they separate them without touching the
arithmetic.

They are also the numbers that were lost, not new ones.
`CON-004` records that the original covered and required counts are not retained downstream — the
reduction computes them and then publishes only their quotient.
A consumer wanting "9 of 10, one waived" has to reconstruct it from the findings array, which the
list read does not carry at all (`SW-031`).
Publishing the denominator restores an explanation that the system already computes.

Two counts rather than a flag is `SW-039`'s decision applied again: the comparison belongs to the
consumer, and a derived boolean restates a rule in a second place it can drift from.
Two counts rather than four — a covered count and a total as well — is where the line is drawn,
because `covered` is recoverable as `coverage × judgedTargetCount` and the inapplicable total is
already visible in the findings.
`judgedTargetCount` and `waivedTargetCount` are the two a consumer cannot derive from anything else
it is given.

Declining `required` is the one place this spec diverges from `SW-039`, and for the reason `SW-039`
did not face.
There, the stored column had been `NOT NULL` with a default since its migration, so no report existed
without a value.
Here, every report persisted before this change has no counts, and the only available default is `0`
— which `judgedTargetCount: 0` would publish as the assertion "this criterion judged nothing", the
exact false statement `CON-012` exists to prevent, on the entire historical population.
Omitting them says "this report does not know", which is true.
Making them `required` later, after old reports have aged out, is additive.

Carrying them on the shared component rather than the findings fork alone is what makes the list read
usable.
A dashboard listing reports is the surface where a waived pass is most likely to be mistaken for a
clean one, and it is the surface that deliberately does not load findings — so without the counts
there it would have no way to show the difference.
The two components continue to differ in `findings` alone (`SW-031`).

Bumping the agentic schema version rather than adding the fields quietly is what the version exists
for.
An agent that reads `qualityGateFailures` and sees nothing concludes the gate is clean; after this
change that conclusion requires the counts, so a consumer needs to be able to tell which payload it
is holding.

**Verification Description**
A contract test reads a report through all four shapes and asserts every criterion result carries
both counts, with `judgedTargetCount` equal to the denominator its `coverage` was derived from.
A test asserts a result with waived targets reports a non-zero `waivedTargetCount` and a
`judgedTargetCount` reduced by exactly that many relative to the unwaived calculation.
A test asserts a report persisted before this change serves neither key, and that its `coverage` and
`findings` are byte-identical to the pre-change response.
A test asserts a waived finding serialises with `status: WAIVED`, its `waiverReason` and its
`waiverExpiresOn`, and that an unwaived finding carries none of the three keys (`CON-010`).
A test asserts the counts are read from storage, not recomputed: a result whose persisted findings
are altered without recalculation still serves the persisted counts.
A test asserts one report's JUnit export, `--agentic` payload and JSON read agree on which criteria
carried waivers and on how many targets each waived.
A test drives `toolkit/cli`'s generated client over the widened `200` and `202`, confirming the
addition is additive for the client this repo generates.

## Relations

**Related**

- [CON-012](CON-012-criterion-emptied-by-waivers-is-never-a-coverage-pass.md) — the
  invariant these counts make checkable
- [SW-039](SW-039-report-publishes-its-pinned-threshold.md) — the four-shapes-agree precedent, and
  the `required` decision this one departs from
- [SW-031](SW-031-findings-served-with-the-report.md) — amended: the served finding gains its waiver
  reason, expiry and `WAIVED` status
- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — amended: the counts the
  ratio discarded
- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — amended: the single reduction that yields
  them
- [ARCH-012](ARCH-012-findings-on-the-event-coverage-as-cache.md) — amended: the cached shape they
  are persisted in
- [CON-009](CON-009-coverage-agrees-with-findings.md) — amended: the agreement obligation they join
- [SW-017](SW-017-junit-export-mirrors-the-gate-verdict.md) — amended: the export carrying the same
  numbers
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the `202` poll shapes they join
- [CON-010](CON-010-rest-responses-never-carry-null.md) — why an unknown count and an unwaived
  finding's keys are omitted
- [SYS-009](SYS-009-machine-consumable-gate-result.md) — the machine-consumable result they complete
- [SYS-012](SYS-012-result-consumption.md) — the consumption capability they serve
