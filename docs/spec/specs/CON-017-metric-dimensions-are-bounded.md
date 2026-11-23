# Every metric dimension is drawn from a bounded set; API identity never becomes one

<!-- markdownlint-disable MD036 -->

**Title**
Every metric dimension is drawn from a bounded set; API identity never becomes one

**Lens**: CON

**Status**: planned

**Description**
A Snow-White metric carries only dimensions whose value domain is fixed by the code rather than by
use.

Permitted, and the complete list for this increment:

- `snow.white.load.status` — the ten `ApiLoadStatus` constants.
- `snow.white.outcome` — the bounded coverage taxonomy of
  [SW-056](SW-056-calculation-outcome-rides-on-the-span.md).
- `snow.white.report.status` — the six persisted `ReportStatus` constants.
- `snow.white.api.type` — `ApiType`, where a metric needs it.
- The dimensions the `spanmetrics` connector adds by default: service name, span name, span kind,
  and span status.
  Span name is what distinguishes the calculation phases of
  [SW-055](SW-055-coverage-phases-are-the-span-boundary.md), so no separate phase
  dimension is introduced.

Forbidden as a metric dimension, on any metric, by either mechanism:

- Service name of an API under test, API name, API version, or the composite identity of a
  specification.
- A request path, HTTP route, operation key, or criterion pointer.
- A quality gate name, calculation id, report id, or any other identifier minted per request.
- A free-text error message.

The connector's attribute promotion is held to the same rule: no span attribute is promoted to a
metric dimension except the bounded outcome attribute named above.

**Rationale**
A metric's cost is its series count, and a series exists per distinct combination of dimension
values.
API identity grows with every API version ever synced and is never reclaimed, so admitting it
makes the series count a function of the catalogue's history rather than of the system's size.
The same argument applies more sharply to a calculation id, which is unique per request.

The information is not lost by excluding it.
API identity already travels on the spans — `api.name` and `api.version` are attributes the
ingestion filters already read, and the harness publishes under the API identity
([ARCH-024](ARCH-024-harness-publishes-under-the-api-identity.md)).
Spans are the right home for per-request identity, and a metric that narrows to one API is a trace
query wearing the wrong clothes.

The rule is stated as an invariant rather than as guidance because a dimension is a one-way door.
Once a dashboard or an alert groups by it, removing it breaks them, so the moment to refuse is
before the first one exists.
A free-text error message is called out separately: it is the most tempting of the forbidden set,
because the outcome taxonomy that replaces it took a deliberate decision to define, and a root
cause message is right there in the `catch` branch.

**Verification Description**
A review of every metric registration and of both collectors' connector configuration confirms the
dimension set against the permitted list, and confirms no span attribute beyond the outcome
attribute is promoted.
A unit test per code-instrumented meter asserts its tag keys exactly, so adding a tag fails a test
rather than passing review.
A review confirms the forbidden identifiers do not appear as tag keys or as connector dimensions.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality that stays affordable
  only while the series count does

**Related**

- [ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md) — the two
  mechanisms this invariant binds equally
- [ARCH-027](ARCH-027-connector-reads-only-snow-whites-own-spans.md) — the span
  population excluded partly to hold this invariant
- [SW-056](SW-056-calculation-outcome-rides-on-the-span.md) — the bounded taxonomy that
  exists so an error message does not have to be a dimension
- [ARCH-024](ARCH-024-harness-publishes-under-the-api-identity.md) — the identity that stays on
  spans rather than moving onto metrics
- [NF-006](NF-006-bounded-telemetry-fetch-footprint.md) — the sibling bounding decision on the
  ingestion side
