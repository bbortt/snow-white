# Usable in live production without application code changes beyond standard OpenTelemetry instrumentation

<!-- markdownlint-disable MD036 -->

**Title**
Usable in live production without application code changes beyond standard OpenTelemetry
instrumentation

**Lens**: STK

**Status**: active

**Description**
Platform and API teams need to adopt Snow-White against services they already run, without
rewriting those services to speak a Snow-White-specific protocol.
Standard OpenTelemetry instrumentation — the `@SnowWhiteInformation` metadata Snow-White's own
`toolkit/spring-web-autoconfiguration` attaches to spans — is the only integration surface a
service exposes.

**Rationale**
A tool that demanded bespoke instrumentation would only be adoptable at greenfield, and would tie
every consuming service to Snow-White's own SDK.
Riding on OpenTelemetry, an ecosystem standard already present in most services, keeps adoption to
configuration rather than rewrites.

**Verification Description**
A review of a newly onboarded service confirms it required only standard OpenTelemetry
instrumentation (optionally enriched via `@SnowWhiteInformation`) and no Snow-White-specific
application code.

## Relations

**Related**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — the correlation this
  instrumentation feeds
