# Parameter coverage matches a query parameter by its parsed name, never by substring containment

<!-- markdownlint-disable MD036 -->

**Title**
Parameter coverage matches a query parameter by its parsed name, never by substring containment

**Lens**: SW

**Status**: active

**Description**
Three criteria measure whether an operation's declared parameters were exercised with a value:

- `PARAMETER_COVERAGE`: required is every parameter (`path`, `query`, or `header` location)
  declared on an operation; covered is a parameter observed in at least one matched telemetry span.
- `REQUIRED_PARAMETER_COVERAGE`: the same, restricted to parameters declared `required`.
- `OPTIONAL_PARAMETER_COVERAGE`: the same, restricted to parameters not declared `required`.

Presence is determined per location:

- `path`: implied by the telemetry span matching the operation's path template at all — a path
  parameter cannot be exercised without a value, so a matched span always covers every path
  parameter on that operation.
- `header`: the request carried a header attribute whose name equals the parameter's name
  (case-insensitively).
- `query`: the request's query string, parsed into its `name=value` (or bare-flag) tokens on `&`
  boundaries, contains a token whose name equals the parameter's name.
  A parameter is covered only when its own name is a token in the parsed query string — never
  because its name is a substring of a different token's name or value.

**Rationale**
A coverage tool that reports a parameter as tested when it was not is worse than one that
under-reports: it hides the gap it exists to surface.
Matching a query parameter's name by substring containment (for example, treating the query string
`validId=5` as covering a parameter named `id`) produces exactly that false positive whenever one
parameter's name happens to be contained in another token's name or value.
Parsing the query string into its actual tokens before comparing names removes the false-positive
class entirely, at no cost to genuine matches.

**Verification Description**
A test declares an operation with a required query parameter `id`, correlates it with telemetry
whose query string is `validId=5` only (never `id` itself), and asserts `REQUIRED_PARAMETER_COVERAGE`
reports `id` as uncovered; a second telemetry sample with query string `id=5&validId=9` asserts `id`
is now reported covered.

## Relations

**Realizes**

- [SYS-006](SYS-006-criteria-based-evaluation.md) — the evaluation this criterion
  family is part of

**Related**

- [CON-004](CON-004-coverage-ratio-is-always-bounded-and-well-defined.md) — the shared
  formula this criterion family uses
