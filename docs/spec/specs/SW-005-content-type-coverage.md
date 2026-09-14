# Content-type coverage measures documented request-body media types, ignoring operations without a body

<!-- markdownlint-disable MD036 -->

**Title**
Content-type coverage measures documented request-body media types, ignoring operations without a
body

**Lens**: SW

**Status**: active

**Description**
`CONTENT_TYPE_COVERAGE` measures whether each request-body media type an operation declares (for
example `application/json`, `multipart/form-data`) was actually sent.
Required is every media type key under an operation's request body content map; covered is a media
type observed as the request's `content-type` header in a matched telemetry span.
An observed header matches a declared media type when it starts with the declared value, so a
charset or boundary parameter (`application/json; charset=utf-8`) does not prevent a match.
An operation with no request body, or a request body with no content map, contributes nothing to
required or covered — it is excluded from the calculation entirely, not counted as a covered or
uncovered zero.

This criterion depends on the `content-type` request header being present on the span in the first
place, which is not on by default: the OTel Java agent only captures it once
`OTEL_INSTRUMENTATION_HTTP_SERVER_CAPTURE_REQUEST_HEADERS=content-type` is set (manual OTel
enrichment has no equivalent default to opt into either — the header must be attached by hand), per
`pages/_pages/onboarding.md:150`.
Without it, every operation with a request body reports zero coverage regardless of what was
actually sent, indistinguishable from an operation that was genuinely never exercised with the
right media type.
When none of the telemetry correlated to the operations under evaluation carries a
`content-type` header at all, the calculator's `additionalInformation` must say so explicitly —
that this manual capture step may not have been enabled — rather than only listing the uncovered
media types as if every one of them were a genuine gap.

**Rationale**
Excluding bodyless operations from both sides of the ratio keeps an API dominated by
parameter-only operations (e.g. `GET` endpoints) from appearing to under-perform on a criterion
that does not apply to them; including them with a vacuous "required: 0, covered: 0" per operation
would double-count against
[CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md)'s per-operation
framing without adding information.
Matching by prefix rather than exact string tolerates the parameters a real `content-type` header
commonly carries without requiring the specification to enumerate every charset variant.
A zero score caused by a missed manual configuration step looks identical, in the number alone, to
a zero score caused by a genuinely untested media type; without a hint distinguishing the two, an
operator chasing a `full-feature` quality gate failure has no way to tell "go add tests" from "go
set an environment variable" apart from re-reading the onboarding guide from scratch.

**Verification Description**
A test declares one operation with a `multipart/form-data` request body and one `GET` operation
with no body, correlates both with telemetry, and asserts the `GET` operation contributes neither
to required nor covered while the multipart operation's coverage reflects only whether that media
type was actually sent.
A second test correlates an operation with a request body against telemetry that carries no
`content-type` header on any span, and asserts `additionalInformation` includes a hint that header
capture may not be enabled, in addition to reporting zero coverage.

## Relations

**Realizes**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the evaluation this criterion is
  part of

**Related**

- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the shared
  formula this criterion uses
