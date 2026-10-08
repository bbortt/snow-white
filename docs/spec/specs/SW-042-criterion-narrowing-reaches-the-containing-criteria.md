# An omitted criterion waives a target outright, and a named one waives it for that criterion and the criteria containing it

<!-- markdownlint-disable MD036 -->

**Title**
An omitted criterion waives a target outright, and a named one waives it for that criterion and the
criteria containing it

**Lens**: SW

**Status**: planned

**Description**
A waiver's `criterion` is optional, and the two forms say different things.

**Omitted** — the target is waived for every criterion that judged it.
It is the broad claim, "nothing can exercise this target", and it is the form a documented `500`
behind a mocked downstream wants: the response code is unprovokable, so
`ERROR_RESPONSE_CODE_COVERAGE`, `RESPONSE_CODE_COVERAGE` and `REQUIRED_ERROR_FIELDS_COVERAGE` all
stop judging it together.

**Named** — the target is waived for that criterion and for every criterion that _contains_ it, as
`ARCH-016` declares containment.
Naming `ERROR_RESPONSE_CODE_COVERAGE` therefore also waives the target for
`RESPONSE_CODE_COVERAGE`, and leaves `REQUIRED_ERROR_FIELDS_COVERAGE` judging the same pointer.
Containment reaches upward only: naming `RESPONSE_CODE_COVERAGE` does not waive
`POSITIVE_RESPONSE_CODE_COVERAGE` or `ERROR_RESPONSE_CODE_COVERAGE` for the target, because a
narrower criterion is a stricter claim and a waiver never widens itself into one.

A named criterion that is not among the criteria judging the target is governed by `CON-011`: the
waiver has to match some finding somewhere, and a criterion the gate excluded is not required to be
running.

**Rationale**
Making `criterion` optional rather than mandatory is a choice about which form of the common case is
cheapest to write correctly.
The motivating case is physical — the response cannot be produced — and it is a property of the
target, not of any one criterion.
A mandatory `criterion` would make the author of that waiver enumerate three criteria for one
unprovokable response code, having first discovered which three they are, and a list that has to be
maintained by hand is a list that silently goes stale the next release a criterion is added.
Optional keeps the narrow form available for the genuinely criterion-specific case — "we accept that
this error body does not declare its required fields, but the code itself must still be exercised" —
which is a real and different claim.

The upward reach is what makes the narrow form usable at all.
A child criterion makes a stricter claim than its parent about the same pointer, in one of the two
forms `ARCH-016` declares.
Under the subset form it judges fewer of the parent's targets:
`ErrorResponseCodeCoverageCalculator` extends `ResponseCodeCoverageCalculator` and both emit a
finding at the same response-entry pointer.
Under the strength form it judges the parent's targets against a harder bar:
`OperationSuccessCoverageCalculator` and `MethodCoverageCalculator` both emit at the operation
pointer, and success is more than having been called.
Under the subset form, waiving only the child leaves the parent failing on the identical target, so
the waiver would buy nothing while reading as though it had — the author would then add the parent by
hand, which is the enumeration the optional form exists to avoid.
Under the strength form that argument does not hold on every target, because the parent can be
passing while the child fails: an operation that was called but never answered `2xx` is covered for
`HTTP_METHOD_COVERAGE` and uncovered for `OPERATION_SUCCESS_COVERAGE`, and reaching up to a criterion
that already passes changes nothing.
It is the operation nothing called at all that both of them fail, and there the reach saves the same
enumeration the subset form does.
Refusing the downward reach is the same reasoning in reverse: the parent's claim is weaker than the
child's, and a waiver that silently satisfied stricter criteria than the one it named would be a
privilege escalation in a mechanism whose whole purpose is to be narrow.
What licenses the reach under both forms is the strength of the claim rather than the subsetting of
targets, which is why the strength edge carries it despite the above: naming
`OPERATION_SUCCESS_COVERAGE` for an operation also
waives `HTTP_METHOD_COVERAGE` for it, because conceding that an operation cannot be made to succeed
concedes the weaker statement about it too, while naming `HTTP_METHOD_COVERAGE` concedes nothing
about success.

Containment has to become machine-readable for this to be specifiable, which is why
`ARCH-016` is a prerequisite rather than a nicety.
Today the relation exists in two places no code can read — the English sentence "This is a subset of
`RESPONSE_CODE_COVERAGE`" inside each enum description, and the ASCII tree in
`pages/_pages/quality-gate-criteria.md`.
Specifying "and its ancestors" against prose would leave the behaviour defined by whichever
documentation a reader happened to find, and the two could drift.

**Verification Description**
A test waives one documented `500` with no `criterion` and asserts findings for that pointer became
`WAIVED` under `ERROR_RESPONSE_CODE_COVERAGE`, `RESPONSE_CODE_COVERAGE` and
`REQUIRED_ERROR_FIELDS_COVERAGE` alike.
A test waives the same target naming `ERROR_RESPONSE_CODE_COVERAGE` and asserts the first two became
`WAIVED` while `REQUIRED_ERROR_FIELDS_COVERAGE` still judges the pointer.
A test waives a target naming `RESPONSE_CODE_COVERAGE` and asserts
`ERROR_RESPONSE_CODE_COVERAGE` still judges it.
A test waives an operation naming `OPERATION_SUCCESS_COVERAGE` and asserts `HTTP_METHOD_COVERAGE`
stopped judging that operation too, so the reach holds for the strength form and not only for
subsets; the same test waives an operation naming `HTTP_METHOD_COVERAGE` and asserts
`OPERATION_SUCCESS_COVERAGE` still judges it.
A test walks every criterion that declares a parent and asserts a waiver naming it also waives the
parent, whichever form the declaration carries, so the rule holds for criteria added after this
spec.
A test asserts a waiver naming a criterion the gate excluded, whose target other criteria do judge,
is accepted and applied to those criteria.

## Relations

**Related**

- [ARCH-016](ARCH-016-criteria-containment-declared-on-the-enum.md) — the containment
  relation this rule reads
- [SW-040](SW-040-waiver-enters-only-on-the-calculation-request.md) — the optional property
  this defines
- [SW-041](SW-041-waiver-resolves-against-the-emitted-findings.md) — the target half of the
  same selection
- [CON-011](CON-011-waiver-matching-no-finding-fails-the-calculation.md) — what a criterion
  nothing matched means
- [SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md) — the containing criterion
  the narrow form reaches upward to
- [SW-006](SW-006-required-error-fields-coverage.md) — the criterion sharing a pointer without
  containing it, which is why the narrow form exists
- [SW-001](SW-001-structural-call-coverage.md) — the operation pointer shared by the strength-form
  pair the narrow form also reaches along
- [ARCH-002](ARCH-002-criteria-metadata-owned-by-enum.md) — amended: the enum ownership this
  extends
- [SYS-006](SYS-006-criteria-based-evaluation.md) — the criteria model the containment belongs to
