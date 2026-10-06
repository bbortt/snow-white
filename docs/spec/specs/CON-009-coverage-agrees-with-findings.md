# A stored coverage ratio always equals the ratio its own findings imply

<!-- markdownlint-disable MD036 -->

**Title**
A stored coverage ratio always equals the ratio its own findings imply

**Lens**: CON

**Status**: active

**Description**
For every criterion result that carries findings, the stored `coverage` equals the ratio derived
from those findings under `CON-004`'s rules — `COVERED` over `COVERED + UNCOVERED`, with
`NOT_APPLICABLE` outside both sides, rounded to two places half up, clamped below `1.00` unless
covered equals required, and `1` when required is zero.

The two are never permitted to disagree, at rest or in flight.
This holds for the ratio on the Kafka event, for the persisted column, and for the value the report
API serves — every surface on which the pair appears together.

A result carrying no findings — one written before the findings migration — is outside this
constraint, not a violation of it.
The constraint binds the pair, and such a result has no pair.

Violating it is prevented structurally rather than detected: the ratio and the findings are
produced by one derivation from one target list (`ARCH-010`), and no consumer recomputes the
ratio locally (`ARCH-012`).
Nothing in the system may write a `coverage` value from any source other than that derivation.

**Rationale**
The cached ratio and the findings are the same fact at two resolutions, shown to the same person on
the same screen: a coverage bar reading 67%, and a drilldown listing two covered targets out of
three.
A disagreement between them is not a rounding curiosity — it destroys the report's credibility in
the one moment the feature exists for, when a developer opens the drilldown to check the number.
Worse, it is silent: the gate's pass/fail verdict follows the cached ratio (`SW-016`, `SW-017`), so
a wrong cache fails a build while the evidence on screen says it should have passed, and nothing
raises.

Stating this as a constraint rather than leaving it implied in `ARCH-012`'s denormalization is
the point.
A denormalized column is a promise, and a promise with no named invariant is one refactor away from
a helpful-looking local recomputation — a consumer that "fixes" a stale-looking ratio by counting
the findings it can see, a migration that backfills a column from partial data, a test fixture that
sets both by hand and diverges.
Naming the invariant makes each of those a violation with a place to point at, instead of a
judgement call.

The exemption for findings-free results is deliberate and narrow.
It would be cheaper to state the constraint unconditionally and backfill findings for historical
reports, but the findings cannot be backfilled honestly: the telemetry that would evidence them is
subject to the backend's own retention, and a reconstructed finding set would assert per-target
verdicts nobody computed.
An empty collection beside an untouched historical ratio is the truthful record — the ratio is what
was calculated, and the evidence for it was never kept.

**Verification Description**
A property-style test over generated finding lists asserts the derived ratio matches `CON-004`'s
expectations for every combination of covered, uncovered and inapplicable counts, including the
all-inapplicable and near-complete-clamp cases.
An integration test in `report-coordinator-api` asserts, for every criterion result of a completed
report, that the persisted `coverage` equals the ratio recomputed from that result's persisted
findings — run after a redelivery as well as after a first delivery.
A test asserts a pre-migration result with an empty finding collection is not flagged by that
check.
A review check confirms `coverage` is written only from the shared derivation, and that no consumer
of the report or the event recomputes it.

## Relations

**Related**

- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the ratio rules this
  constraint requires both sides to agree under
- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — the single derivation that makes
  the agreement structural instead of asserted
- [ARCH-012](ARCH-012-findings-on-the-event-coverage-as-cache.md) — the denormalization
  that incurs this obligation, and the pre-migration results it exempts
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — why inapplicable findings sit
  outside the fraction on both sides
- [CON-001](CON-001-deterministic-analysis-results.md) — the determinism this pairs with: the same
  input yields the same ratio, and now the same evidence for it
- [SW-016](SW-016-api-test-verdict-is-gate-scoped.md) — the verdict that reads the cached side and
  would silently diverge without this
- [SW-017](SW-017-junit-export-mirrors-the-gate-verdict.md) — the export with the same exposure

## Changes

- **2026-09-24** — Set active: findings now persist beside the ratio, so the pair this constraint
  binds exists for the first time.
  Anchored on the single derivation rather than on a check: there is one place a ratio is produced
  and no consumer recomputes it, which is what makes a disagreement structurally unavailable instead
  of merely untested.
  The findings-free exemption is asserted rather than assumed — a pre-migration result keeps the
  ratio it was calculated with, and the integration test that recomputes every other ratio from its
  persisted rows skips that one by design.
