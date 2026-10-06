# Required error-fields coverage measures whether a schema-constrained error response was ever observed

<!-- markdownlint-disable MD036 -->

**Title**
Required error-fields coverage measures whether a schema-constrained error response was ever
observed

**Lens**: SW

**Status**: active

**Description**
`REQUIRED_ERROR_FIELDS_COVERAGE` measures whether an operation's documented error responses that
declare required schema fields were exercised.
Required is every error status-code entry (per
[SW-007](SW-007-default-response-key-is-the-error-fallback-case.md)'s error classification)
on an operation whose response schema declares at least one required field; covered is such an
entry matched by an observed status code — an exact match for a literal code, a shared leading
digit for a wildcard pattern (`4XX`), or any observed error code at all for a `default` entry.
Observed means observed on the operation: a span evidences an entry when its concrete operation key
matches the operation's template, the same correlation every other criterion uses, so an entry on
`GET /pung/{message}` is covered by a span that arrived on `GET /pung/hello`.
An error response with no declared required fields contributes nothing to required or covered, and
neither does a positive entry.
Both are targets this criterion looks at and has nothing to say about, so both are recorded as
`NOT_APPLICABLE` findings rather than dropped (`SW-030`) — the document contains the entry, and a
reader of the list is entitled to see that this criterion passed over it.

A `default` entry is covered by any observed error code, including one that a more specific
documented sibling already matched.
It is not evidenced by exclusion: unlike the response-code family's treatment of the same key
(`SW-002`), nothing is subtracted from what may evidence it.

This criterion validates that the response was observed, not that its body actually carried every
required field — OpenTelemetry spans do not capture response bodies, so field-level content cannot
be checked from telemetry alone.
Field-level validation is out of scope until additional instrumentation captures response bodies.

**Rationale**
An error response with mandatory fields (for example a `code` and `message` on every `400`) is a
contract callers rely on; never having exercised that response at all is a real, checkable gap
even without inspecting its body.
Stating the limitation explicitly — presence, not field content — keeps the criterion's name from
promising more than the underlying telemetry can verify.

**Verification Description**
A test declares an operation whose `400` response schema requires `code` and `message`, correlates
it with telemetry containing no `400` response, and asserts the entry is reported uncovered;
adding a `400` response to the telemetry and rerunning asserts it is now reported covered.

## Relations

**Realizes**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the evaluation this criterion is
  part of

**Related**

- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the shared
  formula this criterion uses
- [SW-007](SW-007-default-response-key-is-the-error-fallback-case.md) — the error
  classification and `default` handling this criterion reuses
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — why the entries this criterion passes over
  are recorded rather than omitted
- [SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md) — the sibling criterion
  whose `default` handling this one deliberately does not share

## Changes

- **2026-09-24** — Stated that an entry is evidenced by the spans matching its operation's
  template, and fixed the implementation to do so.
  It had looked telemetry up by exact operation key, which silently found nothing for a templated
  operation whose spans carry a concrete path — reporting a documented error response as uncovered
  when it had been exercised.
  Ratios rise for specifications with path parameters.

- **2026-09-24** — Recorded what happens to the entries this criterion passes over — a positive
  entry, and an error entry declaring no required field — now that migrating it onto the findings
  contract means its output enumerates targets rather than only counting them: both are
  `NOT_APPLICABLE` findings.
  The ratio is unchanged; what was an invisible absence is now a visible verdict.
  Also stated the `default` rule's consequence explicitly, because the neighbouring criterion reads
  the same key differently: here any observed error code covers a `default` entry, with nothing
  subtracted for the siblings that matched it too.
