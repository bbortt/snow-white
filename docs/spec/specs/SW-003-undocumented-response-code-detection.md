# Undocumented-response-code detection measures observed codes against the specification, not the reverse

<!-- markdownlint-disable MD036 -->

**Title**
Undocumented-response-code detection measures observed codes against the specification, not the
reverse

**Lens**: SW

**Status**: active

**Description**
Three criteria invert the direction of the response-code family
([SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md)): instead of asking
"was every documented code observed," they ask "was every observed code documented."

- `NO_UNDOCUMENTED_RESPONSE_CODES`: required is every distinct status code actually observed for an
  operation; covered is an observed code matched by an exact entry, a wildcard entry (`4XX`) sharing
  its leading digit, or a `default` entry in the operation's specification.
  "An operation" is the one the document describes, not the request path a span arrived on: a code
  observed under two concrete paths of one templated operation is one target between them, and
  enters the fraction once.
- `NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES`: the same, restricted to observed codes in the `1xx`–
  `3xx` range.
- `NO_UNDOCUMENTED_ERROR_RESPONSE_CODES`: the same, restricted to observed codes in the `4xx`–`5xx`
  range.

An operation with no telemetry contributes nothing to required or covered for these criteria — an
untested operation is neither documented nor undocumented, it is simply absent from this
calculation (it is covered instead by
[SW-001](SW-001-structural-call-coverage.md)'s `HTTP_METHOD_COVERAGE`).
The converse is not symmetric: telemetry that resolves to no documented operation is judged, and
every code observed on it is undocumented, because an endpoint the specification omits entirely is
the strongest form of the drift these criteria exist to catch.

An observed code the restricted variants leave outside their range is a target they do not judge
rather than one that disappears ([SW-030](SW-030-unjudged-target-is-not-applicable.md)), so a
report on error codes still shows the successful ones it has nothing to say about.

**Rationale**
A specification can under-document its API just as easily as an API can under-implement its
specification; both directions matter to trust the contract.
This direction catches drift the response-code-coverage direction cannot: a response the
implementation actually sends but the specification never lists.
A `default` entry in the specification is deliberately permissive here — it documents the operation
author's intent to allow any unlisted status, so any observed code is "documented" once a `default`
entry exists, consistent with how `default` is treated as a wildcard in
[SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md).

**Verification Description**
A test correlates an operation whose specification lists only `200` with telemetry containing a
`200` and an undocumented `503`, and asserts `NO_UNDOCUMENTED_RESPONSE_CODES` and
`NO_UNDOCUMENTED_ERROR_RESPONSE_CODES` both report the `503` as uncovered; adding a `default` entry
to the same operation's specification and rerunning asserts both now report 100%.

## Relations

**Realizes**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the evaluation this criterion
  family is part of

**Related**

- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the shared
  formula this criterion family uses
- [SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md) — the forward
  direction of the same concept
- [SW-029](SW-029-finding-identified-by-spec-pointer.md) — the pointer ladder these criteria use,
  their target being a code the document may not contain
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — what becomes of an observed code the
  restricted variants do not judge

## Changes

- **2026-09-24** — Stated the target as one distinct status code per _documented operation_ rather
  than per concrete request path.
  The implementation counted observed codes per telemetry group, so a code observed under two
  concrete paths of one templated operation entered the fraction twice and produced two findings
  identical in every field.
  The findings model ([SW-029](SW-029-finding-identified-by-spec-pointer.md)) made that a
  contradiction rather than a rounding difference, since a finding is identified by its pointer and
  discriminators.
  Also recorded two things the prose left implicit: telemetry resolving to no documented operation
  is still judged, and a code outside a restricted variant's range is inapplicable rather than
  absent.
