# ADR-0003: Evidence is exported as the report's own JSON, never as JUnit properties

- Status: Accepted
- Date: 2026-10-01
- Deciders: @bbortt

## Context

With the drilldown shipped, findings and their evidence are served inline with the report
(`SW-031`) and rendered in the UI — `api-test-findings.tsx` groups evidence by test case name and
falls back to a truncated trace id, which is what issue #1642's third acceptance criterion asked
for.
What has no answer yet is how that evidence leaves the system, and three candidate answers are on
the table: as `<properties>` in the JUnit document, as a "Download Evidence" button beside the
existing JUnit one, or as a file the CLI persists the way it already persists JUnit XML.

Four facts about what exists decide most of it.

**The evidence is already a versioned, machine-readable payload.**
`GET /api/rest/v1/reports/{calculationId}` answers `QualityGateReportWithFindings` →
`ApiTestResultWithFindings` → `ApiTestFinding` → `FindingEvidence`, the last being the
`(traceId, testCaseName)` pair of `ADR-0002`.
That shape is declared in `v1-report-api.yml`, governed by `CON-010`'s null-omission rules, and
three generated clients already read it: the webapp's axios client, `toolkit/cli`'s fetch client,
and whatever `toolkit/openapi-generator` emits for a consumer.
Nothing needs to be built to produce evidence in a consumable form; it is being produced on every
report read.

**The JUnit document is deliberately narrow.**
`SW-017` fixes a three-level mapping — report to `testsuites`, API test to `testsuite`, criterion
result to `testcase` — and states why: a build server's only vocabulary is pass, fail and skip, the
gate is the master, and diagnostics that do not fit that vocabulary go into `system-out`.
The root already carries `calculationId` and `minCoveragePercentage` as properties; a test case
carries the criterion's description.
The document is name-ordered so it is diffable across runs, and compared verbatim against six
committed expected documents, the largest of which is 2.6 KB for two test cases.

**A finding list is not small, and is not bounded by anything a viewport controls.**
`SW-030` makes it the criterion's complete target space, `NOT_APPLICABLE` entries included, so its
size follows the analysed specification across all 14 criteria per API test.
The drilldown pages at 20 findings behind a "show all" and shows at most three unnamed traces
before collapsing the rest into a count — the UI is lossy by design, precisely because the
underlying list is not.

**Only one of the three candidates runs unattended.**
The stated need is an artifact saved alongside the pipeline, and `NF-002` and `NF-004` are about
unattended, CI-suitable operation.
`toolkit/cli` already sits in that position: `--junit-output <path>` fetches the XML after the poll
settles and writes the response body verbatim, refusing to combine with `--async`.
A button in a web UI is, by construction, not something a pipeline does.

## Decision

**D1 — The JUnit document stays a verdict; no finding and no evidence entry enters it.**
`SW-017`'s mapping is unchanged, and neither a `property`, an attribute, nor an embedded document
carries evidence.
Both encodings that could work are worse than not doing it.
A flat key namespace (`finding.0.evidence.1.traceId`) is a schema smuggled through a naming
convention, unreadable by anything that did not agree to it in advance and unversioned by anything
at all.
An embedded JSON document in a property's text node is a second copy of a payload
`v1-report-api.yml` already versions — the same evidence in two representations, one of which no
contract covers and no client is generated from, which is how the two drift.
Either way the artifact a build server blocks on grows by orders of magnitude to carry a payload
nothing in that consumer chain parses, and stops being the diffable document `SW-017` made it.
The document already carries the join key: `calculationId` is a property on the root, so a consumer
holding nothing but the XML can reach the evidence through the endpoint that serves it.

**D2 — The exported artifact is the report read's response body, byte for byte.**
Not a bespoke `evidence.json` schema: a second schema needs its own version, its own null rules,
its own generator and its own tests, to express something the report response already expresses, and
the first consumer that wanted one more field would put the contract in two places.
Not a re-serialisation of a parsed model either — a generated model round-trip silently drops what
it does not know and reorders what it does, so two exports of one report would differ by which
client wrote them.
The artifact is what the endpoint returned.

**D3 — The pipeline writes it, through the CLI, as `--report-output <path>`.**
Symmetric with `--junit-output` in every respect that matters: path required, mutually exclusive
with `--async`, written after the poll settles from the raw response body rather than from the
deserialised model, reported on stdout the way the XML is.
The name is the one part worth arguing about.
`--evidence-output`, the phrasing this came in as, names the interesting part of a payload that is
in fact the whole report; that mislabel is the first step back toward the bespoke schema D2
rejects, because someone will eventually "trim it down to just the evidence".
`--json-output` reads as a format pair with `--junit-output`, but `--agentic` already means "emit
JSON" in this CLI — a single-line summary for an automated consumer on stdout, which is a different
artifact for a different reader and must not be confused with this one.
The default file name is `snow-white-report.json`, beside `JUNIT_XML_FILENAME`'s
`snow-white-junit.xml`.

**D4 — The UI keeps a download of its own, and D3 does not make it redundant.**
They serve different readers.
D3 serves whoever controls the invocation; the button serves whoever opens a report they did not
run — from the report list, days later, wanting the raw findings for a ticket or for the
`snow-white` skill to chew on — and the drilldown they are looking at is truncated at 20 findings
and three unnamed traces, so the screen is not a substitute for the file.
It re-fetches the report endpoint on click and downloads the response, with the file name on the
anchor's `download` attribute, rather than serialising the Redux entity: that makes it the same
bytes as D3's artifact by construction rather than by coincidence, and it keeps
`Content-Disposition` off a read that the SPA and the CLI's two-second poll both perform.

## Consequences

This is the cheapest of the three candidates and the only one that touches no backend: no new
endpoint, no change to `ReportResource`, `JUnitReporter` or the persisted model, no migration, no
contract version.
Two client-side additions over a payload that is already on the wire, and the JUnit document keeps
the properties it has.

The pipeline story becomes symmetric and explainable in one line — `--junit-output` for the verdict
a build server blocks on, `--report-output` for the evidence behind that verdict, upload both — and
the two artifacts are the two representations the API already publishes, rather than one artifact
carrying the other inside it.

What becomes harder is the file's size.
It is proportional to the analysed specification's target space across 14 criteria per API test, it
has no page size, and nothing truncates it; a pipeline archiving it per run pays storage that the
XML did not cost.
That is deliberate: the UI truncates because a screen must, and a truncated evidence file is worse
than no evidence file, because it looks complete.
If this turns out to bite, the lever is retention on the CI side, not a smaller export.

Both additions live outside clew's scope — `toolkit/cli` and the `api-gateway` webapp — so they go
through the `requirements` flow rather than `clew-draft`, and neither gets a spec id.
The only clew-visible statement here is the _non_-change to `SW-017`, which belongs in that spec's
`Changes` as a recorded decision not to widen it, not as an amendment to what it specifies.

Reversibility is asymmetric in the useful direction.
D3 and D4 are additive and independently removable; a flag nobody passes and a button nobody clicks
cost nothing.
D1 is the one that would be expensive to reverse late, which is the argument for holding it now:
evidence in the XML means an `SW-017` amendment, six regenerated expected documents, and — once any
consumer starts parsing it — a format frozen by people who never agreed to a schema.

One thing is deliberately left undecided: a _summary_ of evidence inside the vocabulary `SW-017`
already uses, such as the number of distinct evidencing test names appended to a passing case's
`system-out` beside the coverage gap.
That stays within pass, fail and skip, grows with test cases rather than with findings, and is not
an unversioned copy of anything, so D1 does not rule it out on principle — it is out of scope here
because it rewrites `SW-017`'s text and all six expected documents, and the case for it should be
made by someone who wants it in a build log rather than inferred from this decision.

## Relations

- Constrains [SW-017](../spec/specs/SW-017-junit-export-mirrors-the-gate-verdict.md) — the export
  this decision declines to widen, and whose `system-out` remains the only diagnostic channel in
  that document
- Consumes [SW-031](../spec/specs/SW-031-findings-served-with-the-report.md) — the read whose
  response body _is_ the exported artifact
- Supplements [ADR-0002](ADR-0002-test-identity-is-an-attribute-on-the-span.md) — the
  `(traceId, testCaseName)` entry that makes the export worth keeping, and whose null half every
  reader of the file still has to handle
- Related
  [SW-030](../spec/specs/SW-030-unjudged-target-is-not-applicable.md) — why a finding list is the
  full target space, and therefore why the file is large and the screen is truncated
- Related [CON-010](../spec/specs/CON-010-rest-responses-never-carry-null.md) — the null rules a
  second export schema would have had to restate
- Related [NF-002](../spec/specs/NF-002-unattended-operation.md),
  [NF-004](../spec/specs/NF-004-cicd-suitable-performance.md) — why the pipeline, not a human with
  a browser, produces the artifact
- Related [SYS-009](../spec/specs/SYS-009-machine-consumable-gate-result.md),
  [SYS-012](../spec/specs/SYS-012-result-consumption.md) — the machine-consumable result and the
  consumption capability this widens
- Carried by [STR-017](../spec/stories/STR-017-api-test-findings-back-every-criterion-coverage.md)
  — the story that produced the evidence, whose "out of scope" list left the export unaddressed
