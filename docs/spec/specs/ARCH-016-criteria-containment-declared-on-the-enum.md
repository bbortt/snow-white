# Criteria containment is declared on the criteria enum, not described in prose beside it

<!-- markdownlint-disable MD036 -->

**Title**
Criteria containment is declared on the criteria enum, not described in prose beside it

**Lens**: ARCH

**Status**: active

**Description**
`OpenApiCoverageCriteria` declares, per member, the criterion that contains it — a reference to
another member of the same enum, or none for a root criterion.
Containment means "every target this criterion judges is also a target the containing criterion
judges, at the same spec pointer, and the container judges targets this one does not" — the same
check applied to a strictly smaller set of targets.
Requiring the set to be strictly smaller is what makes the relation directional: two criteria
judging exactly the same targets contain neither the other, so the forest below follows from the
definition instead of being a convention layered over it.
It is the relation `pages/_pages/quality-gate-criteria.md` draws as a tree, moved into the enum
`ARCH-002` already makes the single source of criteria metadata.

The relation is a forest: every member has at most one container, and no cycle exists.
Both are asserted by test, not merely intended.

The enum exposes the derived walk — a member's containers, transitively, up to its root — so a caller
asking "which criteria contain this one" does not reimplement the traversal.
The initial declaration is the published tree's groups minus the one the definition above rejects,
and nothing beyond them:
`POSITIVE_RESPONSE_CODE_COVERAGE` and `ERROR_RESPONSE_CODE_COVERAGE` under `RESPONSE_CODE_COVERAGE`;
the two restricted undocumented-code criteria under `NO_UNDOCUMENTED_RESPONSE_CODES`;
`REQUIRED_PARAMETER_COVERAGE` and `OPTIONAL_PARAMETER_COVERAGE` under `PARAMETER_COVERAGE`.
`PATH_COVERAGE` is not contained by `HTTP_METHOD_COVERAGE`, which the tree claimed until this spec
was written: a path item and an operation within it are different targets at different pointers, so
neither criterion's target space is a subset of the other's.
What holds between them is an implication between ratios at full coverage, stated by `SW-001`, which
is not what this relation declares and not what a waiver may propagate along.
`OPERATION_SUCCESS_COVERAGE` under `HTTP_METHOD_COVERAGE` is the near miss worth naming, because it
is the pair that tempts the relation the other way: `SW-001` gives the two the same targets at the
same pointer, and success is the stricter check of them, but neither judges a target the other
leaves out.
What relates them is strength of check rather than containment of targets, so this declaration has
no place for it either.

The criteria-reference documentation becomes a view of the declaration rather than a second source of
it, and loses that group.

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
A test asserts the declared relation reproduces the published tree exactly — the three containment
groups above and nothing else — so an undeclared relation and an invented one both fail.
`PATH_COVERAGE` reaching no container is part of that assertion, so redrawing it under
`HTTP_METHOD_COVERAGE` on the page fails the suite until someone declares the edge in code too —
where the definition above is the thing that says no.
A test asserts the transitive walk answers the full container chain for a two-level criterion and an
empty chain for a root.
A test asserts a criterion whose declaration and published tree disagree is caught in either
direction: declaring a container the page does not draw fails, and drawing one the enum does not
declare fails.
A criterion added as a root is the one case that passes silently, which is the deliberate default —
the page documents every criterion by test either way.
A test asserts containment changes no criterion's calculation, and a test asserts it adds no
criterion to a predefined gate.

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
- [SW-001](SW-001-structural-call-coverage.md) — the implication between path, method and
  operation-success coverage, which is not containment and is not declared here
- [SW-006](SW-006-required-error-fields-coverage.md) — a criterion that shares a pointer with
  another without being contained by it
- [SYS-008](SYS-008-quality-gate-definitions.md) — unchanged: containment grants no inclusion

## Changes

- **2026-10-07** — Set active: implementation of `STR-023`'s first increment began.
  The containment relation is the one part of the story nothing else depends on, so it lands before
  the waiver intake that will read it.
- **2026-10-07** — Dropped `PATH_COVERAGE` under `HTTP_METHOD_COVERAGE` from the initial
  declaration, and with it from the published tree, the enum description and the skill reference.
  The group was in the documentation before this spec existed and was carried over unexamined; it
  does not satisfy the containment definition this spec gives, because the two criteria judge
  different targets at different pointers rather than the same target at the same one.
  The implication between their coverage ratios is real and stays in `SW-001` — it just is not the
  relation `SW-042` propagates a waiver along, and declaring it here would have sent an
  operation-scoped waiver up to a path item it never judged.
