# Criteria containment is declared on the criteria enum, not described in prose beside it

<!-- markdownlint-disable MD036 -->

**Title**
Criteria containment is declared on the criteria enum, not described in prose beside it

**Lens**: ARCH

**Status**: active

**Description**
`OpenApiCoverageCriteria` declares, per member, the criterion that contains it — a reference to
another member of the same enum, or none for a root criterion — together with the form that
containment takes.
A criterion is fixed by two things: the set of targets it selects, and the check it applies to each
of them.
Containment means the contained criterion matches its container on one of those two and is strictly
narrower on the other, at the same spec pointer.
That admits exactly two forms:

- **subset** — the same check over a strictly smaller set of targets, the container judging targets
  the contained criterion does not;
- **strength** — a strictly stronger check over the same set of targets, every target satisfying the
  container being one the contained criterion may still fail.

Requiring strictness in one of the two dimensions is what makes the relation directional: two
criteria judging the same targets with the same check contain neither the other, so the forest below
follows from the definition instead of being a convention layered over it.
Every declared edge carries its form, because the two cannot be stated in the same words — "subset
of" is simply false of a strength edge, whose two criteria judge the same targets — and because what
may be read off an edge differs between them.
It is the relation `pages/_pages/quality-gate-criteria.md` draws as a tree, moved into the enum
`ARCH-002` already makes the single source of criteria metadata.

The relation is a forest: every member has at most one container, and no cycle exists.
Both are asserted by test, not merely intended.

The enum exposes the derived walk — a member's containers, transitively, up to its root — so a caller
asking "which criteria contain this one" does not reimplement the traversal.
One walk serves both forms, because what it answers is "which criteria is this one a narrowing of",
and `SW-042`'s upward reach is licensed by the narrowness of the claim rather than by subsetting of
targets specifically.

The declaration is the published tree's groups, minus the one the definition rejects and plus the one
it admits in its second form.
Six edges in the subset form:
`POSITIVE_RESPONSE_CODE_COVERAGE` and `ERROR_RESPONSE_CODE_COVERAGE` under `RESPONSE_CODE_COVERAGE`;
the two restricted undocumented-code criteria under `NO_UNDOCUMENTED_RESPONSE_CODES`;
`REQUIRED_PARAMETER_COVERAGE` and `OPTIONAL_PARAMETER_COVERAGE` under `PARAMETER_COVERAGE`.
One edge in the strength form: `OPERATION_SUCCESS_COVERAGE` under `HTTP_METHOD_COVERAGE`.
`SW-001` gives those last two the same targets at the same pointer — one operation within one path
item — and success is the stricter check of them: an operation that was called at all satisfies the
container, and that same operation satisfies the contained criterion only if one of those calls
answered `2xx`.
Neither judges a target the other leaves out, which is why the subset form has no place for it; what
relates them is strength of check, which is the second way of being a narrowing rather than a
disqualification from being one.
Declaring it is what lets `SW-042` carry a waiver on an operation's success up to the method coverage
of that same operation, which is the reach that spec already describes.

`PATH_COVERAGE` is not contained by `HTTP_METHOD_COVERAGE` under either form, which the tree claimed
until this spec was written: a path item and an operation within it are different targets at
different pointers, so neither criterion's target space is a subset of the other's, and neither is a
stricter check over a set they share.
What holds between them is an implication between ratios at full coverage, stated by `SW-001`, which
is not what this relation declares and not what a waiver may propagate along.

No coverage implication rides along a containment edge in a fixed direction, and the two forms are
why.
Under subset, a container at full coverage puts everything below it at full coverage: full
`RESPONSE_CODE_COVERAGE` is full `ERROR_RESPONSE_CODE_COVERAGE`.
Under strength the implication runs the other way: full `OPERATION_SUCCESS_COVERAGE` is full
`HTTP_METHOD_COVERAGE`, while full method coverage says only that every operation was called, not
that any of them ever worked.
A consumer reading an implication off an edge without reading its form would be right about six edges
and wrong about the seventh, so this relation licenses neither direction and every view of it states
the form alongside the edge.

The criteria-reference documentation becomes a view of the declaration rather than a second source of
it: it loses the path group and gains the operation-success one, because the declaration is now what
decides which groups it draws.

Nothing about a criterion's own behaviour changes: containment carries no inheritance of calculation,
of inclusion in a quality gate, or of coverage.
It is a declared fact about target spaces and the strength of the checks over them, consumed by
`SW-042` and by documentation.
One consequence is worth stating because it looks like a defect and is not: a gate requiring both
`HTTP_METHOD_COVERAGE` and `OPERATION_SUCCESS_COVERAGE` at the same threshold requires the weaker of
the two for nothing, which is the mirror image of the redundancy a subset group produces and is a
gate-composition question rather than a containment one.
This relation grants no inclusion and takes none away.

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

That same argument is why the two forms are one relation carrying a tag rather than two relations
side by side.
A separate strength relation would need its own forest assertion, its own transitive walk and its own
answer in `SW-042`, and the first question anyone would ask of it is whether a waiver reaches along
both — which is to say, whether the two closures should have been one.
The forms differ in what they license a reader to conclude about coverage, and nowhere else: the
forest property, the walk, and the waiver reach are identical across them, because `SW-042`'s reach
is justified by a narrower criterion being a stricter claim, and subsetting the targets is only one
way to make a claim stricter.
Tagging the edge keeps the single closure and still lets the documentation views say the true thing
in each case, which is the smallest change that admits the second form without weakening the first.

Making the documentation a view is what stops the drift from returning.
The page stays the thing a human reads; it just stops being a place the relation can be edited into a
state the code disagrees with.

**Verification Description**
A test asserts every member's declared container is a member of the same enum, that no member
contains itself transitively, and that every member reaches a root in finitely many steps.
A test asserts every member declaring a container also declares a form, and every root declares
none, so an edge cannot arrive with the question of how it narrows left open.
A test asserts the declared relation reproduces the published tree exactly — the four containment
groups above and nothing else — so an undeclared relation and an invented one both fail.
A test names the single strength edge and its form, because the definition admits it on grounds the
subset groups do not use, and a regression silently returning the relation to subsets only would
otherwise pass.
`PATH_COVERAGE` reaching no container is part of that assertion, so redrawing it under
`HTTP_METHOD_COVERAGE` on the page fails the suite until someone declares the edge in code too —
where the definition above is the thing that says no, now under both forms.
A test asserts the transitive walk answers the full container chain for a two-level criterion and an
empty chain for a root.
That a container does not reach back down to the criterion below it is asserted at the container's
own end, where its chain is required to be empty, because asserting it from the contained end would
hold whatever the edge said.
A test asserts a criterion whose declaration and published tree disagree is caught in either
direction: declaring a container the page does not draw fails, and drawing one the enum does not
declare fails.
A test asserts each form is published in its own words: a criterion's description ends in the
sentence its declared form calls for and does not carry the sentence the other form would, and the
page section for it carries that sentence in the declared form wherever in the section it sits, so
publishing a subset claim over a strength edge fails even though the edge itself is declared.
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
- [SW-001](SW-001-structural-call-coverage.md) — the shared operation pointer that makes
  operation-success coverage the strength form of method coverage, and the path-to-method implication
  that is neither form and stays undeclared
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
- **2026-10-08** — Widened containment from one form to two, and declared
  `OPERATION_SUCCESS_COVERAGE` under `HTTP_METHOD_COVERAGE` in the second.
  The previous definition required a strictly smaller target set, which made this pair a near miss
  this spec named and then rejected: the two judge the same operations at the same pointer, so
  neither target space is smaller.
  That rejection was too narrow a reading of its own purpose.
  `SW-042`'s upward reach is justified by a narrower criterion being a stricter claim, and a smaller
  target set is only one way of being stricter — a stronger check over the same targets is the other,
  and the waiver reach, the forest property and the transitive walk are unchanged by which one an
  edge uses.
  So the edge was not invented here, only made explicit, and it is declared as one relation with a
  form on each edge rather than two relations: a second relation would need a second closure, and two
  closures over one relation is the drift this spec was written to stop.
  What does differ between the forms is the coverage implication, which runs the opposite way under
  strength, so no direction may be read off an edge without its form and every view now states both.
  One consequence is recorded and deliberately not acted on: `full-feature` and `basic-coverage` both
  require `HTTP_METHOD_COVERAGE` beside `OPERATION_SUCCESS_COVERAGE`, which the strength implication
  makes the weaker of the two.
  Containment grants no inclusion and takes none away, so pruning a gate stays a separate decision.
