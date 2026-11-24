# Snow-White's own traces are never head-sampled, because a derived metric would silently become an estimate

<!-- markdownlint-disable MD036 -->

**Title**
Snow-White's own traces are never head-sampled, because a derived metric would silently become an
estimate

**Lens**: CON

**Status**: planned

**Description**
No Snow-White deployment configures a head sampler for Snow-White's own services.
The OpenTelemetry Java agent's default, `parentbased_always_on`, stands, and every span Snow-White
emits reaches the collector.

Concretely:

- Neither the development stack nor the Helm chart sets `OTEL_TRACES_SAMPLER`,
  `OTEL_TRACES_SAMPLER_ARG`, or their system-property equivalents for any Snow-White service.
- The chart exposes no value that configures one, so a sampler cannot be reached through the
  documented surface.
- The dependence is recorded where an operator changing sampling would read it, not only here.

This constrains Snow-White's own telemetry.
It says nothing about how a customer samples the application under test, which is upstream of
Snow-White and governed by what that customer wants analysed.

**Rationale**
The `span_metrics` connector measures the spans that arrive.
Under head sampling, what arrives is a subset, and every derived count and histogram becomes an
estimate scaled by a sampling rate the metric does not carry.
The failure mode is the dangerous kind: nothing errors, no pipeline breaks, and the dashboards keep
rendering — the numbers are simply wrong, by a factor nobody is looking for.
A derived rate reading a tenth of the truth is indistinguishable from a system doing a tenth of the
work, which is precisely the condition an operator consults it to rule out.

Stating it as an invariant rather than leaving it implicit is the whole value of the spec.
Sampling is the first thing proposed when telemetry cost is questioned, often years later and by
someone with no reason to know that a metrics pipeline reads those spans.
Writing the coupling down converts that from an accident into a decision with a visible
consequence: sampling Snow-White's own traces means abandoning the derived metrics or moving them
into application code.

The constraint is cheap to hold today because nothing configures a sampler anywhere — the
development stack, the chart templates, and the service properties are all silent, so the agent
default already applies.
This spec keeps it that way rather than introducing anything.

**Verification Description**
A review confirms no sampler environment variable or property is set for a Snow-White service in
the development stack, the chart templates, the chart values, or any module's application
properties.
A Helm template test renders the chart and asserts no rendered Snow-White workload carries a
sampler variable.
A review confirms the dependence is documented alongside the collector's connector configuration,
so it is visible to whoever would change sampling.

## Relations

**Realizes**

- [NF-001](NF-001-operational-observability.md) — the observability quality that a sampled metric
  would quietly stop providing

**Related**

- [ARCH-027](ARCH-027-connector-reads-only-snow-whites-own-spans.md) — the derivation
  whose accuracy this invariant protects
- [ARCH-026](ARCH-026-durations-derived-from-spans-counts-instrumented.md) — the
  code-instrumented half, which is unaffected by sampling and is the fallback if this invariant
  is ever abandoned
- [NF-008](NF-008-tempo-search-returns-every-matched-span-per-trace.md) — the sibling
  completeness requirement on the query side
