# InfluxDB telemetry query narrows each span's returned attributes to the required keys

<!-- markdownlint-disable MD036 -->

**Title**
InfluxDB telemetry query narrows each span's returned attributes to the required keys

**Lens**: SW

**Status**: planned

**Description**
`InfluxDBTelemetryServiceImpl.findOpenTelemetryTracingData` builds its Flux query so the `_value`
assigned to each row, after parsing the stored attribute blob, contains only the given required
attribute-key set (`SW-021`) — not the full parsed blob.
Every `OpenTelemetryData` this
implementation returns therefore carries an `attributes` `JsonNode` containing at most the required
keys, even when the underlying span carried additional attributes InfluxDB stored for it.

**Rationale**
This mirrors `SW-022`'s Tempo behavior at the response-payload level: neither backend hands the
calculators (or the network layer, or process memory) more attribute data per span than any
calculator reads.
It does not mirror it at the storage-read level — InfluxDB stores each span's
attributes as one JSON blob per row (the `attributes` field), so InfluxDB itself still reads the
full blob to parse and filter it; this spec narrows what leaves that parse step, not what InfluxDB
does internally to produce it.
Closing that deeper gap would mean changing how attributes are
written to InfluxDB in the first place (`STR-013`'s Out of scope), a separate, larger change
this spec does not make.

**Verification Description**
A unit test on the Flux-query builder asserts the generated query's final mapping step constructs
its `_value` object from only the given required-key set.
An integration test against an InfluxDB
test instance seeded with spans carrying attributes outside the required set asserts every
returned `OpenTelemetryData`'s `attributes` `JsonNode` contains only keys from that set, and that
the values for keys within the set are unchanged from what was seeded.

## Relations

**Related**

- [SW-021](SW-021-required-attribute-key-set-derivation.md) — the key set this query
  narrows to
- [ARCH-007](ARCH-007-required-attribute-keys-computed-once-by-caller.md) — how this
  backend receives the key set it narrows to
- [SW-022](SW-022-tempo-search-returns-only-required-keys.md) — the sibling behavior on the
  other backend, at the response-payload level rather than the storage-read level
- [ARCH-001](ARCH-001-pluggable-influxdb-or-tempo-telemetry-backend.md) — the pluggable-backend
  interface this implementation satisfies
- [NF-006](NF-006-bounded-telemetry-fetch-footprint.md) — bounds fetch _count_ for this backend
  (currently unaddressed for InfluxDB); this spec narrows fetch _width_ instead, a distinct axis
  that does not close that gap
