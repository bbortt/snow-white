# Waivers are applied to a calculator's findings before the single derivation, never to a coverage already derived

<!-- markdownlint-disable MD036 -->

**Title**
Waivers are applied to a calculator's findings before the single derivation, never to a coverage
already derived

**Lens**: ARCH

**Status**: planned

**Description**
The waiver pass runs inside `AbstractOpenApiCoverageCalculator.calculate`, between the subclass's
`calculateFindings` and the shared `deriveCoverage`.
The subclass emits its complete finding set; the shared template rewrites the matched findings to
`WAIVED`; the shared reduction then derives the ratio and the counts from the rewritten set.

The waivers for the calculation arrive with the request event and are passed into the template, not
into each subclass.
No subclass sees a waiver or decides what to do with one — `calculate` stays `final`, so a calculator
cannot opt out of the pass or apply it twice.

Nothing downstream re-applies a waiver.
`report-coordinator-api` persists the findings and the derived numbers as they arrive on
`OpenApiCoverageResponseEvent` (`ARCH-012`), and neither it, nor the API gateway, nor any read path
recomputes a coverage from waived findings.

**Rationale**
`ARCH-010` puts the derivation in exactly one place, and `CON-009` requires the published ratio to
agree with the published findings.
Applying waivers after the derivation would break both at once: whoever subtracted the waived targets
would have to recompute the ratio, which is a second derivation, and the version persisted from the
event would disagree with the findings served beside it.
The only position that satisfies both is upstream of the single reduction — rewrite the input, let
the one formula run.

Doing it in the shared template rather than in each calculator is what makes the rule enforceable
instead of conventional.
`calculate` is already `final`, which is the mechanism `ARCH-010` uses to stop a subclass producing
a ratio of its own; routing waivers through the same method extends that guarantee to waivers for
free.
Fourteen concrete calculators each remembering to apply a waiver pass is fourteen chances for one to
forget, and the symptom of forgetting — a criterion that quietly ignores its waivers — looks exactly
like a mismatched target.

Doing it in the coverage stream rather than in `report-coordinator-api` is the service-boundary half
of the same decision.
The stream owns calculation; the coordinator owns reports.
A waiver changes what was judged, which is a calculation concern, and moving it into the coordinator
would mean the coordinator either recomputes coverage — the forbidden second derivation — or
publishes findings whose statuses contradict the number it received.
It would also put the rule in the one service that never reads a specification, so the propagation
`SW-042` needs would be decided without the enum walk being anywhere near the calculation.

The cost of this position is that a waiver cannot be applied to an existing report: changing one
means re-triggering the calculation.
That is accepted, and it is the same shape as every other calculation input — nobody expects to
re-scope a report's lookback window after the fact either — and it is what `SYS-014`'s
input-not-state rule implies anyway.

**Verification Description**
A unit test over the abstract calculator asserts a waived target is absent from both sides of the
derived ratio, and that the derived counts describe the rewritten set.
A test asserts every concrete calculator inherits the pass without implementing it — no subclass
overrides `calculate` and no subclass references a waiver.
A test asserts the same findings with and without the pass differ only in the statuses of the matched
findings and in the derived numbers.
A test asserts `report-coordinator-api` persists the coverage from the response event unchanged for a
result carrying waived findings, computing nothing of its own.
A contract test asserts the served ratio, the served counts and the served findings of a waived
result agree (`CON-009`).

## Relations

**Related**

- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — amended: the single derivation this pass
  feeds, which also yields the judged and waived counts
- [CON-009](CON-009-coverage-agrees-with-findings.md) — amended: the agreement this position is
  chosen to preserve
- [ARCH-012](ARCH-012-findings-on-the-event-coverage-as-cache.md) — amended: the cached coverage the
  coordinator persists without recomputing
- [SW-041](SW-041-waiver-resolves-against-the-emitted-findings.md) — the selection this pass
  executes
- [SW-044](SW-044-report-publishes-judged-and-waived-target-counts.md) — the counts the same
  reduction produces
- [SYS-014](SYS-014-exemption-is-an-input-never-stored-state.md) — the input-not-state rule
  that also forbids re-waiving an existing report
- [ARCH-004](ARCH-004-per-api-test-fan-out-keyed-by-calculation-id.md) — amended: the event that
  delivers the waivers to the pass
- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — the service boundary this respects
