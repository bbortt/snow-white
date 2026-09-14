# Path, method, and operation-success coverage measure whether an operation was called and succeeded

<!-- markdownlint-disable MD036 -->

**Title**
Path, method, and operation-success coverage measure whether an operation was called and succeeded

**Lens**: SW

**Status**: active

**Description**
Three criteria measure, at increasing precision, whether the specification's operations were
exercised at all:

- `PATH_COVERAGE`: required is every distinct path in the specification (method-agnostic); covered
  is a path with at least one telemetry span whose concrete path matches the path template,
  regardless of HTTP method.
- `HTTP_METHOD_COVERAGE`: required is every path+method operation in the specification; covered is
  an operation with at least one telemetry span whose concrete operation key (method and path)
  matches its template.
- `OPERATION_SUCCESS_COVERAGE`: required is every path+method operation in the specification;
  covered is an operation with at least one telemetry span matched to it whose HTTP status code is
  in the `2xx` range.
  A non-numeric observed status code is skipped when checking for success, not treated as a
  failure to match.

Each is strictly more demanding than the last for the same operation: an operation covered by
`OPERATION_SUCCESS_COVERAGE` is necessarily covered by `HTTP_METHOD_COVERAGE`, and an operation
covered by `HTTP_METHOD_COVERAGE` is necessarily covered by `PATH_COVERAGE` (for its path).

**Rationale**
Being called and succeeding are different facts: an operation exercised only by requests that all
errored out is "called but never working," which `HTTP_METHOD_COVERAGE` alone cannot distinguish
from "never called."
Separating call-coverage from success-coverage lets a quality gate require both.

**Verification Description**
A test correlates a specification with telemetry containing one path/method combination with no
matching span, one with only non-2xx spans, and one with a 2xx span, and asserts `PATH_COVERAGE`,
`HTTP_METHOD_COVERAGE`, and `OPERATION_SUCCESS_COVERAGE` each report exactly the operations meeting
their own definition above as covered.

## Relations

**Realizes**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the evaluation this criterion
  family is part of

**Related**

- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the shared
  formula this criterion family uses
