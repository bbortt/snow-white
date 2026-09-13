# Telemetry ingestion scoped strictly to the requested analysis, not general-purpose storage

<!-- markdownlint-disable MD036 -->

**Title**
Telemetry ingestion scoped strictly to the requested analysis, not general-purpose storage

**Lens**: SYS

**Status**: active

**Description**
Snow-White ingests telemetry only for the duration and scope a specific analysis request needs
(the API in question and its configured lookback window) — it does not aim to be, or replace, a
general-purpose observability or long-term telemetry storage system.

**Rationale**
Ingesting and retaining telemetry beyond what an analysis needs would duplicate the job of
dedicated observability platforms and grow storage and privacy exposure without benefit; scoping
ingestion to the request keeps Snow-White a consumer of telemetry, not a competing store.

**Verification Description**
A review of a telemetry query confirms it is bounded by the requested API and lookback window, and
that no separate long-term retention path for ingested telemetry exists in Snow-White's own
storage.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — bounds how the telemetry side of
  the correlation is obtained

**Related**

- [SYS-004](SYS-004-telemetry-ingestion-and-correlation.md) — what is ingested
- [SYS-010](SYS-010-analysis-triggering.md) — the request that defines the scope
