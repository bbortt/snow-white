# Criteria containment is declared on the criteria enum, not described in prose beside it

<!-- markdownlint-disable MD036 -->

**Title**
Criteria containment is declared on the criteria enum, not described in prose beside it

**Lens**: ARCH

**Status**: planned

**Description**
`OpenApiCoverageCriteria` declares, per member, the criterion that contains it — a reference to
another member of the same enum, or none for a root criterion.
Containment means "every target this criterion judges is also a target the containing criterion
judges, at the same spec pointer".
It is the relation `pages/_pages/quality-gate-criteria.md` draws as a tree, moved into the enum
`ARCH-002` already makes the single source of criteria metadata.

The relation is a forest: every member has at most one container, and no cycle exists.
Both are asserted by test, not merely intended.

The enum exposes the derived walk — a member's containers, transitively, up to its root — so a caller
asking "which criteria contain this one" does not reimplement the traversal.
The initial declaration reproduces the published tree exactly, adding no relation the documentation
does not already claim:
`POSITIVE_RESPONSE_CODE_COVERAGE` and `ERROR_RESPONSE_CODE_COVERAGE` under `RESPONSE_CODE_COVERAGE`;
the two restricted undocumented-code criteria under `NO_UNDOCUMENTED_RESPONSE_CODES`;
`REQUIRED_PARAMETER_COVERAGE` and `OPTIONAL_PARAMETER_COVERAGE` under `PARAMETER_COVERAGE`;
`PATH_COVERAGE` under `HTTP_METHOD_COVERAGE`.
The criteria-reference documentation becomes a view of the declaration rather than a second source of
it.

Nothing about a criterion's own behaviour changes: containment carries no inheritance of calculation,
of inclusion in a quality gate, or of coverage.
It is a declared fact about target spaces, consumed by `SW-042` and by documentation.

**Rationale**
`SW-042` needs to answer "which criteria contain this one" in code, and today there is no code to
ask.
The relation lives in two prose artifacts — a sentence inside each enum description string and an
ASCII tree on a documentation page — neither of which is parseable and which can disagree with each
other without anything failing.
Specifying waiver propagation against prose would make the behaviour of a gate depend on which
document a reader found.

The enum is where this belongs because `ARCH-002` already put the criteria vocabulary there: label
and description are owned by the enum so that a criterion's metadata cannot drift between the service
that calculates it and the one that reports it.
Containment is the same kind of fact, and the same argument applies with more force, because this one
is load-bearing for a verdict.
The alternatives are worse in the usual ways: a lookup table in the calculators would put it in the
one service that does not report, a database table would make a compile-time fact into runtime state
with a migration per criterion added, and a configuration file would let a deployment redefine what
a waiver propagates to.

Declaring one container rather than a set keeps the relation checkable.
A criterion with two containers would make `SW-042`'s upward reach a lattice walk whose order of
application is observable, and the published tree has never needed it.
A single reference also makes the forest property a test rather than an argument.

Exposing the transitive walk on the enum, rather than letting each caller climb the references,
matters because there will be more than one caller: the waiver pass needs it now, and the criteria
documentation and any future "this failure also fails its parent" presentation need the same answer.
Two implementations of a transitive closure over the same relation is how the prose and the tree
drifted in the first place.

Making the documentation a view is what stops the drift from returning.
The page stays the thing a human reads; it just stops being a place the relation can be edited into a
state the code disagrees with.

**Verification Description**
A test asserts every member's declared container is a member of the same enum, that no member
contains itself transitively, and that every member reaches a root in finitely many steps.
A test asserts the declared relation reproduces the published tree exactly — the four containment
groups above and nothing else — so an undeclared relation and an invented one both fail.
A test asserts the transitive walk answers the full container chain for a two-level criterion and an
empty chain for a root.
A test asserts a criterion added to the enum without a deliberate containment decision is caught: the
assertion over the published tree fails until the documentation and the declaration agree.
A test asserts containment changes no criterion's calculation, inclusion or coverage.

## Relations

**Related**

- [ARCH-002](ARCH-002-criteria-metadata-owned-by-enum.md) — amended: the enum ownership this extends
  from label and description to containment
- [SW-042](SW-042-criterion-narrowing-reaches-the-containing-criteria.md) — the one consumer
  that needs the relation in code
- [SYS-006](SYS-006-criteria-based-evaluation.md) — the criteria model the relation describes
- [SW-002](SW-002-response-code-coverage-treats-default-as-wildcard.md) — a containing criterion of
  the published tree
- [SW-003](SW-003-undocumented-response-code-detection.md) — the criterion family whose restricted
  variants the tree places under their root
- [SW-006](SW-006-required-error-fields-coverage.md) — a criterion that shares a pointer with
  another without being contained by it
- [SYS-008](SYS-008-quality-gate-definitions.md) — unchanged: containment grants no inclusion
