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
An error response with no declared required fields contributes nothing to required or covered.

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
