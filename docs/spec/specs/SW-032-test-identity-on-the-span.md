# A span's test identity is the OpenTelemetry `test.case.name` attribute, read as an opaque label

<!-- markdownlint-disable MD036 -->

**Title**
A span's test identity is the OpenTelemetry `test.case.name` attribute, read as an opaque label

**Lens**: SW

**Status**: planned

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
