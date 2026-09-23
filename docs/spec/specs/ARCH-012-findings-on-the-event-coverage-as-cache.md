# Findings travel with the calculation result and persist beside it; coverage stays as a denormalized cache

<!-- markdownlint-disable MD036 -->

**Title**
Findings travel with the calculation result and persist beside it; coverage stays as a
denormalized cache

**Lens**: ARCH

**Status**: planned

**Description**
`OpenApiTestResult` — the `internal/commons` DTO that carries a criterion's outcome from
`openapi-coverage-stream` to `report-coordinator-api` over Kafka — gains the calculator's findings
as a field, alongside the `coverage`, `duration` and `additionalInformation` it already carries.
Findings cross the service boundary on the result event they belong to.
There is no second topic, no separate producer, and no lookup back into the stream service.

On the receiving side, `ApiTestResult` gains a `findings` child collection, each row carrying its
status, spec pointer, discriminators and evidence; evidence is an `@ElementCollection` in its own
table, one row per `(traceId, testCaseName)` pair (`ARCH-011`), indexed on the trace id.
`test_case_name` is nullable and stays null until `SW-032`'s convention is both emitted by a
consumer's test harness and requested in the narrowed attribute set — the column ships with the
table rather than as a later migration, because a nullable column on an empty table is free and the
same change to a published `v1-report-api.yml` component is not (`SW-031`).
The finding status persists as a stable numeric code, exactly as `ARCH-006` requires of report and
API-test status, with the same total decoding: a stored code matching no constant must not raise.

`ApiTestResult.coverage` remains, and remains the column every list-shaped read uses.
It is a denormalized cache of what the findings imply (`CON-009`), not an independent value —
it is written from the same derivation that produced the findings (`ARCH-010`), never
recomputed locally by the consumer.

`SW-020`'s replace-not-accumulate guarantee extends to the child collection: a redelivered result
for the same `(apiTest, apiTestCriteria)` replaces its findings wholesale, leaving nothing from the
superseded delivery.

The schema change ships as a forward migration under
`microservices/report-coordinator-api/src/main/resources/db/migration`, following the existing
`V<yyyy_MM_dd>__<description>.sql` convention.
Reports written before it carry no findings; that is an empty collection, not an error, and no
consumer may treat an empty finding list as a failed calculation.

**Rationale**
Findings are derived from, and only meaningful with, the criterion result they explain.
Splitting them onto their own event would make the consumer responsible for correlating two
deliveries that Kafka guarantees nothing about jointly, and would leave a window in which a
persisted result's cached ratio has no findings to agree with — breaking `CON-009` as a
transactional property.
One event keeps the result and its evidence atomic, and lets `SW-020`'s existing replace semantics
extend to the collection instead of needing their own ordering rules.

Keeping `coverage` as a stored column is the deliberate denormalization the issue's own sketch
calls for.
The report list view reads one ratio per criterion across every result in a report; deriving that
by aggregating a child collection — and its grandchild evidence table — would trade a column read
for a join per row to recompute a number the producer already knew.
`SW-016`'s gate-scoped verdict and `SW-017`'s JUnit export both read that ratio and are unchanged
by this story.
The cost is a consistency obligation, which is why `CON-009` is stated as a constraint rather
than left implied.

The stable-code choice for status follows `ARCH-006` for the same reasons it gave: an ordinal binds
the database to source order and a name binds it to spelling, while findings are the
highest-cardinality rows this system writes — one per target per criterion per API test — so the
per-row cost of a name is paid the most times here.
Tolerant decoding matters more here than anywhere: a finding is evidence attached to a verdict, and
one unrecognised row must not make a report unreadable.
The `WAIVED` status that #2009 will add is exactly the forward-compatibility case this buys — an
older reader decodes it rather than failing.

**Verification Description**
An integration test in `report-coordinator-api` consumes a result event carrying findings and
asserts they persist with their status codes, spec pointers, discriminators and evidence, and that
the evidence table is populated and indexed on the trace id.
A finding whose evidence entries carry no test identity is asserted to persist with
`test_case_name` null rather than absent rows.
A second delivery of the same `(apiTest, apiTestCriteria)` with different findings is asserted to
leave only the new ones — no orphan row from the superseded delivery, in either table.
A test asserts each finding-status constant round-trips through its documented numeric code and
that an unknown code decodes without raising.
Reading a report persisted before the migration is asserted to yield results with empty finding
collections and unchanged `coverage` values.

## Relations

**Related**

- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — the derivation that produces
  both the findings and the ratio written here
- [CON-009](CON-009-coverage-agrees-with-findings.md) — the consistency obligation this
  denormalization incurs
- [SW-031](SW-031-findings-served-with-the-report.md) — how the persisted findings reach a
  consumer
- [ARCH-006](ARCH-006-report-status-persisted-as-stable-code.md) — the stable-numeric-code decision
  this extends to finding status; amended for it on 2026-09-22
- [SW-020](SW-020-redelivered-criterion-result-replaces-existing-one.md) — the replace-not
  -accumulate guarantee this extends to the child collection; amended for it on 2026-09-22
- [ARCH-004](ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — the fan-out this event
  rides; unchanged, only its payload widens
- [ARCH-005](ARCH-005-dispatch-after-transaction-commit.md) — the commit-then-dispatch rule the
  widened write must continue to observe
- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — a cached-ratio consumer this story leaves
  untouched
- [SW-017](SW-017-junit-export-mirrors-the-gate-verdict.md) — the other cached-ratio consumer
