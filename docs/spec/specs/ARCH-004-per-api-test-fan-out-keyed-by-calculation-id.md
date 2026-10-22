# One calculation request per API test, all keyed by the calculation id

<!-- markdownlint-disable MD036 -->

**Title**
One calculation request per API test, all keyed by the calculation id

**Lens**: ARCH

**Status**: active

**Description**
A triggered calculation is dispatched as **one Kafka record per API test**, not one record per
report.
Every record of a calculation carries the **calculation id as its key** and the same report
parameters — lookback window and attribute filters, the latter all as string-equality filters —
differing only in the API it names.
The active OpenTelemetry trace context is injected into each record's headers, and the responding
side extracts it again, so the asynchronous hop stays inside one trace.

**Rationale**
Per-API-test records are what make the calculation parallelisable: each is independently
consumable, so a report covering ten APIs occupies ten consumers rather than serializing behind
one, which is the scaling property `NF-003` asks for.
Keying every record of a calculation with the same calculation id keeps that parallelism from
costing ordering — a shared key lands the whole fan-out on one partition, so the responses for one
report cannot interleave across partitions, and the key is also what the response side matches a
report on without a lookup table.
Propagating the trace context through headers is the only way a trace survives a message queue:
without it, the calculation would appear in telemetry as an unattributed root span and
`NF-001`'s diagnosability would stop at the producer.

**Verification Description**
A test dispatches a calculation for a report with several API tests and asserts one record per API
test on the configured request topic, every record keyed with the calculation id, each carrying
that API's information plus the shared lookback window and attribute filters converted to
string-equality form, and trace-context headers present on each.
`QualityGateCalculationRequestDispatcherUnitTest` covers the record shape and keying, and
`ReportCoordinatorApiAppTest` observes the records on a real broker.

## Relations

**Related**

- [SW-013](SW-013-calculation-trigger-is-all-or-nothing.md) — the trigger that leads to
  this dispatch
- [ARCH-005](ARCH-005-dispatch-after-transaction-commit.md) — when this dispatch is
  allowed to happen
- [SW-019](SW-019-final-delivery-attempt-absorbs-failure.md) — the handling of the
  responses this fan-out produces
- [NF-003](NF-003-scalability-without-redesign.md) — the scalability this shape serves
- [NF-001](NF-001-operational-observability.md) — the observability the propagated trace context
  preserves
- [SW-008](SW-008-kafka-as-async-calculation-driver.md) — Kafka as the asynchronous calculation
  driver this dispatch feeds
- [SW-040](SW-040-waiver-enters-only-on-the-calculation-request.md) — the waivers this dispatch
  filters so each record carries only its own API's
- [SW-043](SW-043-expiry-judged-against-the-triggering-instant.md) — the pinned calculation instant
  each dispatched record also carries

## Changes

- **2026-09-17** — Set active: anchored against `report-coordinator-api`'s existing
  implementation (`STR-011`).
- **2026-10-04** — Records the two fields each dispatched record gains, for `SW-040` and `SW-043`
  (`STR-023`).
  The fan-out shape is unchanged — still one record per API test, keyed by calculation id — and both
  additions follow from it rather than bending it.
  Because a record already addresses exactly one API, the waiver array can be filtered at dispatch to
  the entries matching that API, which is what keeps a waiver document off every other record and
  bounds the payload without a cap.
  A waiver whose `apiVersion` is `'*'` is matched on service and API name alone and so reaches every
  version of that API in the report.
  The pinned instant travels the same way, as its own field, rather than being read from the record
  timestamp — whose meaning is a broker setting.
