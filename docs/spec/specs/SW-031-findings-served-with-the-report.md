# A criterion result's findings are served inline with the report, not behind a second call

<!-- markdownlint-disable MD036 -->

**Title**
A criterion result's findings are served inline with the report, not behind a second call

**Lens**: SW

**Status**: planned

**Description**
The `ApiTestResult` component of `v1-report-api.yml` gains a `findings` array.
Fetching a report returns every criterion result's findings in the same response — there is no
per-criterion drilldown endpoint, and no query parameter that has to be asked for before the
findings appear.

Each finding carries its `status` (`COVERED`, `UNCOVERED`, `NOT_APPLICABLE`), its `specPointer`
(`SW-029`), its nullable discriminators (`httpPath`, `httpMethod`, `responseCode`,
`parameterName`, `contentType`), and its `evidence`.

`evidence` is an array of objects, not of strings: each entry carries a required `traceId` and a
nullable `testCaseName` (`ARCH-011`).
`testCaseName` is serialised as `null` — not omitted — where the evidencing span carried no test
identity, which is every span until `SW-032`'s convention is in use, so a consumer's
name-or-fall-back-to-trace-id branch is exercisable from the first release.
The status is serialised as its name, not the numeric code `ARCH-012` persists — the code is a
storage contract, the API is a read contract, and a consumer reading JSON should not need a
codebook.

`coverage` and `additionalInformation` stay exactly as they are on the component.
`additionalInformation` continues to carry the calculator's free-text message and is not replaced
by, folded into, or deduplicated against the findings.

The array is present and empty for a result that has none — a report written before findings
existed, or a criterion whose target space is empty.
Empty never means "not loaded", so a consumer never has to distinguish the two.

**Rationale**
Findings are not an optional detail view of the report; they are what the report's numbers mean.
The consumer that motivates them — the drilldown of #1642's phase 2 — expands inline beside a
coverage bar that is already on screen, so a second round trip would buy latency in exchange for
nothing: the report response is already per-report, already includes every criterion result, and
the findings for one report are bounded by the spec's own size.

A drilldown endpoint would also invent an addressing scheme for a criterion result — the entity has
a composite identity (`apiTest` + `apiTestCriteria`) that is not currently exposed as a URL — and
that URL would then be public contract, for a resource nobody has asked to fetch on its own.
Declining to create it is cheaper to reverse than creating it: adding a focused endpoint later is
additive, while removing one is a breaking change.

Serialising the status by name rather than by code is the one place the storage and read contracts
are deliberately allowed to diverge.
`ARCH-006`'s reasons for a numeric code are storage reasons — decoupling the column from source
order and from spelling, for a value written on every high-cardinality row.
None of them apply to a JSON field read by a browser, where an opaque `2` would make the response
unreadable without the enum to hand, and where the eventual `WAIVED` member is more useful as a
word than as a number.

Publishing `evidence` as an array of objects rather than `traceIds` as an array of strings is the
one place this contract pays a small cost now to avoid a breaking change later.
`v1-report-api.yml` is a versioned contract that consumers generate clients from — the same
generator this repo ships in `toolkit/openapi-generator` — so widening a component with a new field
is additive and safe, while changing a `string[]` into an object array is not.
Issue AC 3 already states that a trace entry should read as a test name where one exists; shipping
the array of strings first would mean either a `v2` component or a parallel `testNames` array
positionally zipped against `traceIds`, which is the kind of shape that only ever gets misread.

The empty-array-not-absent rule exists because the migration guarantees a population of reports
with no findings at all (`ARCH-012`).
A consumer that has to tell "this report predates findings" from "this criterion had none" would be
asking a question with no stable answer; making both empty removes the question.

**Verification Description**
A contract test fetches a report whose results carry findings and asserts each result's `findings`
array is present, with status serialised as its name, the spec pointer intact, discriminators
populated exactly where applicable, and evidence present on covered findings only.
A test asserts an evidence entry serialises as an object with `traceId` set and `testCaseName`
explicitly `null` where the span carried no test identity.
A test asserts a report persisted before the findings migration serialises `findings` as `[]` and
leaves `coverage` and `additionalInformation` byte-identical to the pre-change response.
A test asserts no additional request is required to obtain findings — the report response alone
satisfies a drilldown of every criterion in it.
The generated client in `toolkit/openapi-generator`'s consumers is asserted to compile against the
widened component, confirming the change is additive.

## Relations

**Related**

- [ARCH-012](ARCH-012-findings-on-the-event-coverage-as-cache.md) — the persisted shape
  this read contract projects, and the pre-migration reports it must serve
- [SW-029](SW-029-finding-identified-by-spec-pointer.md) — the pointer served as
  `specPointer`
- [ARCH-011](ARCH-011-evidence-captured-at-the-match.md) — the evidence entry this contract
  publishes, and why it is a pair rather than a trace id
- [SW-032](SW-032-test-identity-on-the-span.md) — the convention that fills `testCaseName`
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — the inapplicable findings this
  response carries for the consumer to collapse
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the in-progress read path, which
  returns results without findings for criteria that have not reported yet
- [ARCH-006](ARCH-006-report-status-persisted-as-stable-code.md) — the storage contract this read
  contract deliberately does not mirror
- [SYS-012](SYS-012-result-consumption.md) — the result-consumption capability this widens
- [ARCH-008](ARCH-008-backend-services-addressed-by-path-prefix-with-aggregated-openapi-docs.md) —
  the gateway routing the widened response travels, unchanged
