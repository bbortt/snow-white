# Triggering a calculation is all-or-nothing: any unresolvable API rejects the whole request

<!-- markdownlint-disable MD036 -->

**Title**
Triggering a calculation is all-or-nothing: any unresolvable API rejects the whole request

**Lens**: SW

**Status**: active

**Description**
`report-coordinator-api` exposes a calculation trigger that names one quality gate and one or more
APIs.
Before anything is persisted, every named API is resolved against `api-index-api`; the results are
collected as a set, not consumed fail-fast.

- **Accepted** — `202` with a `Location` header addressing the report on the **public**
  `api-gateway` URL (not this service's own address), and the freshly created report as the body.
- **Rejected, API side** — `400` if _any_ named API fails to resolve or resolves to a
  specification format nothing can analyze, with **every** failure message joined into a single
  body: an API that `api-index-api` answers `404` for is reported as not indexed, an API whose
  indexed type no coverage calculator consumes is reported with that type named, any other failure
  as an unexpected error carrying its root cause.
- **Rejected, gate side** — `404` if the named quality gate does not exist in `quality-gate-api`.

In both rejection cases **no report is persisted and no calculation request is dispatched**: a
request naming four APIs of which one is unindexed leaves no trace behind.

Which types are analyzable is declared on `ApiType` itself, so the day a second coverage
calculator is deployed the rejection stops applying to its format without this contract changing.

The OpenAPI contract states this: the operation description carries the all-or-nothing rule and
the absence of any trace after a rejection, the `400` response describes the aggregation and every
failure classification, and the `Location` header records that it addresses the public gateway.

**Rationale**
A caller is a CI pipeline, and a pipeline that must re-run to discover its second broken API wastes
a build cycle per fault — so validation reports every failure at once rather than the first.
The contract has to say so, because none of it is inferable from the schemas: a generated client
sees a `400` carrying `{code, message}` and has no way to learn that the request was atomic, that
`message` holds more than one failure, or that there is no calculation id to poll afterwards.
All-or-nothing creation follows from that: a partially dispatched calculation would produce a
report that can never complete, because the coverage responses it waits for will never arrive for
the API that was rejected, leaving it to expire via
[SW-018](SW-018-stale-reports-time-out-not-deleted.md) instead of failing immediately and
legibly.
An API indexed under a type no calculator consumes produces exactly that report: the request is
published to the shared calculation-request topic, every stream filters it away by type, and the
caller's only feedback is a timeout minutes later.
Rejecting it at the trigger is the same
all-or-nothing rule applied one step further on — resolvable is not the same as analyzable.
The `Location` header addresses the public gateway because the URL is handed to a human or a build
log, where this service's cluster-internal address would be unreachable.

**Verification Description**
A black-box test triggers a calculation against a stubbed `api-index-api` and `quality-gate-api`
and asserts: `202` plus a `Location` built from the configured public gateway URL for the happy
path; `404` for an unknown gate name; `400` naming the unindexed API when one of the requested
APIs is not indexed; and `400` naming the indexed type when a requested API carries one no
coverage calculator consumes — each rejection carrying the error body alone, with no report id to
poll.
`ReportCoordinatorApiAppTest` covers all four outcomes end to end, and
`ApiIndexServiceUnitTest` covers the per-API classification of the `404`, unanalyzable-type and
unexpected-error cases.
That nothing reaches the calculation-request topic is not asserted end to end — the harness
consumes only the coverage-response topic — and rests instead on rejection happening before any
report is persisted, which [ARCH-005](ARCH-005-dispatch-after-transaction-commit.md) sequences
dispatch behind.

## Relations

**Related**

- [SYS-010](SYS-010-analysis-triggering.md) — the triggering capability this contract implements
- [NF-005](NF-005-clear-failure-feedback.md) — the clear-feedback attribute the aggregated
  failure body serves
- [ARCH-005](ARCH-005-dispatch-after-transaction-commit.md) — why an accepted request
  dispatches only once its report is durable
- [SW-014](SW-014-in-progress-report-answers-accepted.md) — the retrieval contract the
  returned `Location` leads to
- [SW-018](SW-018-stale-reports-time-out-not-deleted.md) — the timeout that a partially
  dispatched report would otherwise depend on
- [SYS-003](SYS-003-openapi-as-indexed-format.md) — why OpenAPI is currently the only analyzable
  format, and therefore the only one this trigger accepts

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
- **2026-09-18** — The all-or-nothing rule, the aggregated `400` body and the public-gateway
  `Location` are now documented in the OpenAPI contract, where they were previously invisible.
  Documentation only — the trigger's behavior is unchanged.
- **2026-10-27** — A third API-side rejection: an API indexed under a type no coverage calculator
  consumes is now rejected with `400` naming that type, where it was previously accepted,
  dispatched and left to time out via [SW-018](SW-018-stale-reports-time-out-not-deleted.md).
