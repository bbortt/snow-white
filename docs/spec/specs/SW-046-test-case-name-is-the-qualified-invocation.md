# A test's identity is its fully qualified method name, with the invocation where JUnit distinguishes one

<!-- markdownlint-disable MD036 -->

**Title**
A test's identity is its fully qualified method name, with the invocation where JUnit distinguishes
one

**Lens**: SW

**Status**: active

**Description**
The value the extension produces for `test.case.name` is
`<fully qualified class name>#<method name>`, for example
`io.github.bbortt.snow.white.example.application.TestIdentityBaggageAppTest#shouldPingSuccessfully`.
The package is part of it and the `#` separates the method, which is what the existing
`TestIdentityBaggageAppTest` already sends and what `SW-032`'s own examples show.

Where JUnit distinguishes one invocation of a method from another — a `@ParameterizedTest`, a
`@RepeatedTest`, any `@TestTemplate` — the value carries the invocation's own discriminator,
appended as `[<n>]` where `n` is JUnit's 1-based invocation index.
A plain `@Test` carries no suffix.
Both come from the test's unique id, whose final segment JUnit writes as
`[test-template-invocation:#1]` for an invocation and `[method:plainTest()]` for a plain test —
verified against the pinned engine (6.1.3) in this session on 2026-10-07, which is what makes the
index available at all.
The display name is deliberately not used for this: it is author-controlled free text, frequently
contains the argument values, and so would make the identity vary with data the test happens to
be fed.

Two cases produce **no** identity at all, rather than a degraded one:

- **No test method.** A container-level callback, or any context JUnit gives the extension without
  a test method, has nothing to name.
  A `@TestFactory`'s dynamic tests are not named individually — the factory method is the test
  method JUnit reports, so its whole fan-out shares the factory's identity.
  That is accepted rather than solved: this repository contains no `@TestFactory`, and a dynamic
  test has no stable index to discriminate on the way a template invocation does.
- **A name past `FindingEvidence.MAX_TEST_CASE_NAME_BYTES`** (1024 UTF-8 bytes).
  The storing side already drops such a name whole rather than truncating it (`SW-032`), so
  producing one buys a `WARN` log and no identity.
  The producing side applies the same bound and emits nothing, which is the one outcome that is
  indistinguishable from "this suite does not name its tests" — the behaviour that is already
  specified and already handled everywhere downstream.

The bound is read from `FindingEvidence.MAX_TEST_CASE_NAME_BYTES` rather than restated.
That constant lives on the event contract both sides speak precisely so that the side applying it
and the side storing it cannot drift, and the extension is now a third party to that agreement.

Producing no identity is never an error: a test with no name is a test whose findings fall back to
a trace id, exactly as they do today (`SW-031`).

**Rationale**
Qualifying the name is the decision here, and the alternative is what the first cut of the
extension did: `Class.getSimpleName() + "." + method`.
That is ambiguous across a multi-module repository by construction — this repository has two
classes named `TestIdentityBaggageAppTest`, one per example application, and `#2082` is about to
score several modules into one report.
Two different tests resolving to one identity is worse than no identity, because it is wrong
rather than absent, and `SW-032` forbids the reader from parsing the value to disambiguate it.

The `#` separator is not cosmetic either.
A fully qualified name joined by dots cannot be split back into class and method by anything that
does not already know the package depth, and the upstream convention's own second example
(`example/tests/TestCase1.test1`) shows it does not care which separator a producer picks.
Matching what the repository's existing baggage sender writes keeps the one identity already in
use stable, and `#` is the JVM's own convention for naming a member.

Distinguishing invocations matters because a `@ParameterizedTest` is the shape most likely to cover
several targets with one method — one case per status code, one per content type.
Collapsing its invocations would attribute every target to the same name and lose precisely the
information the report exists to show.
Using the index rather than the display name keeps the value stable when a test's arguments change
but its structure does not, which is what keeps a report comparable between runs.

Bounding at the shared constant rather than a local one is the same argument `FindingEvidence`'s own
Javadoc makes for where the constant lives: the side that applies the bound and the side that has
to hold what it admits are built and deployed separately.
Adding a third party that restated the number would reintroduce the drift the constant exists to
prevent.
It also earns the extension's dependency on `internal/commons`, which would otherwise be carrying
a published test artifact's transitive weight for nothing.

**Verification Description**
A test asserts a plain `@Test` in a packaged class yields
`<package>.<SimpleName>#<method>`, with the package present and `#` as the separator.
A test asserts two invocations of one `@ParameterizedTest` method yield different values, that each
ends in its own `[<n>]`, and that the two values differ only in that suffix.
A test asserts the same for `@RepeatedTest`, and that a plain `@Test` value ends in no `[...]`.
A test asserts a value whose arguments change but whose structure does not keeps the same identity
— the display name does not reach the value.
A test asserts a context carrying no test method yields no identity, and that this is not an error.
A test asserts a class and method name summing past `FindingEvidence.MAX_TEST_CASE_NAME_BYTES`
yields no identity rather than a truncated one, that the boundary is applied in UTF-8 bytes — a
multi-byte name under the limit in characters and over it in bytes is dropped — and that a name
exactly at the bound is still produced.
A review confirms the bound is read from `FindingEvidence.MAX_TEST_CASE_NAME_BYTES` and not
restated as a literal.

## Relations

**Realizes**

- [ARCH-013](ARCH-013-test-identity-travels-as-baggage.md) — the runner-side entry this value fills

**Related**

- [SW-032](SW-032-test-identity-on-the-span.md) — the opaque-label rules and the byte bound this
  value is produced to satisfy
- [ARCH-020](ARCH-020-test-identity-published-not-propagated.md) — the two channels this
  value is published through
- [SW-047](SW-047-one-current-span-per-test.md) — the span that carries this value as an
  attribute and as its name
- [CON-013](CON-013-extension-never-changes-a-test-verdict.md) — why producing no identity
  is an outcome and never a failure
- [SW-031](SW-031-findings-served-with-the-report.md) — the trace-id fallback an absent identity
  lands on

## Changes

- **2026-10-07** — Set active: implementation of `STR-026` began.
