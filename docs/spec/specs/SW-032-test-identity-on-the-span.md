# A span's test identity is the OpenTelemetry `test.case.name` attribute, read as an opaque label

<!-- markdownlint-disable MD036 -->

**Title**
A span's test identity is the OpenTelemetry `test.case.name` attribute, read as an opaque label

**Lens**: SW

**Status**: active

**Description**
The attribute a span carries its test identity in is `test.case.name`, as defined by the
OpenTelemetry semantic conventions' test registry — the fully qualified, human-readable name of a
test case, for example `org.example.PetstoreIT.shouldRejectUnknownPet` or
`examples/tests/petstore.spec.ts:should reject unknown pet`.
It is documented in `semantic-convention/` alongside `api.name`, `api.version` and
`openapi.operation.id`, marked as an upstream convention Snow-White adopts rather than one it
defines.

The key is operator-configurable on `openapi-coverage-stream`
(`OpenApiCoverageStreamProperties.testCaseNameAttribute`), defaulting to `test.case.name`, exactly
as `operationIdAttribute` is today.

Snow-White treats the value as an opaque label:

- It is copied onto an evidence entry verbatim — never parsed into suite and case, never truncated,
  never lowercased, never split on `.`, `/` or `#`.
- It is never a correlation key.
  No criterion's verdict, no coverage ratio and no report status depends on it: a span with the
  attribute and a span without it are matched identically (`ARCH-011`), so telemetry that never
  carries it produces exactly the coverage it produces today.
- It participates in nothing that must be stable across releases beyond its own column, so a
  consumer renaming its tests changes what a future report says and invalidates nothing.

Absent, blank, or whitespace-only is the same thing as absent: the evidence entry's `testCaseName`
is null (`SW-031`), and every consumer falls back to the trace id.
So is a name Snow-White cannot store — one past `OpenTelemetryData.MAX_TEST_CASE_NAME_BYTES` (1024
UTF-8 bytes).
Truncating it is forbidden above, so it is dropped whole and the span's evidence keeps only its
trace id; the report itself survives, which an insert failing on an over-long name would not allow.
Because a dropped name and an absent one are indistinguishable downstream, the drop is logged at
`WARN` with the span id, the attribute key and the byte count — never the value.
`test.suite.name`, `test.case.result.status` and `test.suite.run.status` — the rest of the upstream
group — are deliberately not read.

**Rationale**
The convention already exists upstream, so defining a Snow-White-specific attribute would be
inventing a second name for a standard thing.
That matters more here than usual: the value is produced by test tooling Snow-White does not own,
and a test harness, CI vendor or OTel instrumentation library that already emits `test.case.name`
gets Snow-White evidence for free, with no mapping step.
Its Development stability upstream is acceptable because the attribute is read, not written, and
because nothing correlates on it — a rename upstream costs a configuration change, not a migration.
Making the key operator-configurable rather than hard-coded absorbs that risk and matches the one
precedent in the service.

Reading the value as opaque is the load-bearing restriction.
A test name is the only field on an evidence entry that originates outside Snow-White's own
correlation, and the temptation to parse it — to group by suite, to shorten a fully qualified Java
name for display — would make Snow-White's behaviour depend on a naming convention no upstream spec
guarantees.
Grouping and shortening are rendering decisions, and they belong where the renderer is.

Refusing to correlate on it is what keeps this an additive change.
If a verdict could depend on a test identity, then enabling the convention would move coverage
numbers, and a consumer's first act of adopting it would be a changed gate result with no changed
test.
Coverage answers "was this exercised", not "was this exercised by a test that named itself".

Not reading the sibling attributes is a scope decision rather than a judgement about them.
`test.case.result.status` is the interesting one — a target covered only by a failing test is
arguably not covered — but that is a semantics change to every criterion, not a display field, and
it deserves its own story rather than arriving as a side effect of this one.

**Verification Description**
A unit test on the coverage calculators asserts that telemetry whose spans carry the configured
attribute produces evidence entries with that exact string as `testCaseName`, byte for byte,
including a value containing `.`, `/`, `#`, spaces and non-ASCII characters.
A test asserts a blank or whitespace-only value yields null rather than an empty string.
A test asserts the same telemetry with and without the attribute produces identical findings,
identical statuses and an identical coverage ratio for every one of the 14 criteria — the attribute
changes evidence and nothing else.
A test asserts a non-default configured key is honoured and that the default is `test.case.name`.
A documentation check asserts `semantic-convention/` lists the attribute and attributes it upstream.

## Relations

**Related**

- [ARCH-013](ARCH-013-test-identity-travels-as-baggage.md) — how the attribute gets onto the
  server span in the first place
- [ARCH-011](ARCH-011-evidence-captured-at-the-match.md) — the evidence entry this value lands
  on, and the match that must not depend on it
- [SW-031](SW-031-findings-served-with-the-report.md) — how a null value is served, and the
  consumer fallback to a trace id
- [SW-021](SW-021-required-attribute-key-set-derivation.md) — the key set the configured
  attribute joins; amended for it on 2026-09-23
- [ARCH-002](ARCH-002-criteria-metadata-owned-by-enum.md) — the criteria metadata this attribute
  deliberately does not enter, since no criterion judges it
- [CON-001](CON-001-deterministic-analysis-results.md) — the determinism this stays clear of by
  never being a correlation key

## Changes

- **2026-09-25** — Set active with the convention written down and nothing reading it yet:
  `semantic-convention/test.md` lists `test.case.name`, states that Snow-White adopts the upstream
  registry rather than defining it, records the opaque-label rules and the three sibling attributes
  that are deliberately not read, and documents how the attribute gets onto a span (`ARCH-013`).
  The document deliberately stops short of claiming the value reaches a finding, because no code
  reads it yet.
  Everything else in the verification description — `testCaseNameAttribute` and its default, blank
  and whitespace-only yielding null, byte-for-byte evidence values, identical findings with and
  without the attribute — waits on the calculators, and so does the automated documentation check:
  the repository has no docs-assertion pattern today, and inventing one for a single file ahead of
  the behaviour it describes buys nothing.
- **2026-09-28** — Implemented: the value now reaches a finding.
  `OpenApiCoverageStreamProperties.testCaseNameAttribute` defaults to `test.case.name`, joins
  `SW-021`'s required key set, and is resolved in exactly one place —
  `OpenApiCoverageService.groupTelemetryByPath` hoists the attribute onto each
  `OpenTelemetryData` as it groups, so the 8 calculators that build evidence read a plain field and
  none of them learns what the operator configured.
  Hoisting at the grouping step also keeps the identity out of the grouping key, which is what makes
  the invariance claim structural rather than a property of each calculator: an integration test
  compares every autowired calculator's result over telemetry differing only by the attribute,
  ignoring `testCaseName`, and a second test guards it against passing vacuously by requiring that
  the same telemetry yields evidence naming a test and evidence naming none.
  Filling the value made `ARCH-011`'s pair model reachable for the first time, and the schema written
  a day after that amendment still encoded "one trace per finding".
  `V2026_09_28__finding_evidence_test_identity.sql` supersedes it: the unique constraint now spans
  the pair with `NULLS NOT DISTINCT`, and `test_case_name` is widened to 1024.
  It is a second migration rather than an edit of `V2026_09_24`, which had already merged — a
  developer whose bind-mounted database applied the original would meet a Flyway checksum mismatch
  and a service that will not boot, and `DBMigrationUnitTest.databaseMigrationsAreImmutable` exists
  to forbid exactly that.
  `NULLS NOT DISTINCT` puts a PostgreSQL 15 floor under the service, now stated in its README.
  Width alone cannot make the insert safe, though, because the upstream convention bounds the value
  not at all: `OpenTelemetryData.MAX_TEST_CASE_NAME_BYTES` does, at capture, dropping a name past
  1024 UTF-8 bytes rather than truncating it — forbidden above — so it costs its own identity and
  not the report it was found in.
  The drop is logged at `WARN`, because downstream it is indistinguishable from an absent attribute.
  The bound is in bytes because it has to clear the column's 1024
  characters and the constraint btree's 2704-byte row limit at once, which 1024 characters of CJK
  would not.
  `SW-031` already published `testCaseName` on `FindingEvidence`; its `maxLength` moved from 256 to
  1024 with the column, so the contract and what can be stored agree again.
  The documentation check remains deferred for the reason recorded above; `semantic-convention/test.md`
  no longer hedges that nothing reads the attribute, and the coverage-stream README documents the
  property.
