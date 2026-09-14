# The `default` OpenAPI response key is classified as the error fallback, never as a positive response

<!-- markdownlint-disable MD036 -->

**Title**
The `default` OpenAPI response key is classified as the error fallback, never as a positive
response

**Lens**: SW

**Status**: active

**Description**
Wherever a calculator must classify a documented or observed response as an error or a positive
(non-error) response, the OpenAPI `default` response key is classified as an error, and never as
positive: `isErrorHttpStatusCode("default")` is `true`; `isPositiveHttpStatusCode("default")` is
`false`.
An exact numeric code (`100`–`599`) and the `1XX`/`2XX`/`3XX`/`4XX`/`5XX` wildcard patterns are
classified by their numeric range as usual; `default` is the only key classified without regard to
range, because it carries none.

This is Snow-White's own interpretive choice about `default`, not one OpenAPI itself mandates —
the specification defines `default` as a catch-all for any status not otherwise listed, which an
API author could use for a fallback success response as easily as a fallback error response.
Snow-White treats it as the error fallback case project-wide.

**Rationale**
A single, consistent classification is needed everywhere a calculator asks "is this an error
response" (error-response-code coverage, required-error-fields coverage, undocumented-error-code
detection); leaving it ambiguous per calculator would make criteria disagree with each other on
the same operation.
Error responses are the more common real-world use of `default` (a catch-all "something went
wrong" response), and are also the higher-value case to guarantee test coverage for, so that is
where this project resolves the ambiguity.

**Verification Description**
A test asserts `isErrorHttpStatusCode("default")` and `isErrorHttpStatusCode("DEFAULT")` are both
`true`, and `isPositiveHttpStatusCode("default")` is `false`, alongside the existing numeric and
wildcard-pattern cases.

## Relations

**Related**

- [SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md) — uses this
  classification to scope `ERROR_RESPONSE_CODE_COVERAGE`
- [SW-006](SW-006-required-error-fields-coverage.md) — uses this classification to select
  which responses require field coverage
