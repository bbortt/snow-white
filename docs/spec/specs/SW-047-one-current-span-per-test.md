# Each test is one span, current for its duration, carrying its own identity and its own verdict

<!-- markdownlint-disable MD036 -->

**Title**
Each test is one span, current for its duration, carrying its own identity and its own verdict

**Lens**: SW

**Status**: active

**Description**
For every test it sees, the extension produces exactly one span:

- **Named** for the test's identity (`SW-046`), kind `INTERNAL`.
- **Carrying `test.case.name` as an attribute** with that same identity.
  The upstream test registry puts the attribute on the test's own span; Snow-White never reads it
  there (`ARCH-013` rules out resolving identity from the trace), but a consumer's dashboard does,
  and a span named after a test that does not say so in its attributes is a dead end for anything
  that queries rather than eyeballs.
- **Current** from before the test body runs until after it has finished, so work the test performs
  in-process nests under it and the server spans of an instrumented in-process service share its
  trace.
- **Ended exactly once**, with the status its verdict implies: `OK` for a passing test, `ERROR`
  with the throwable recorded for a failing one, and `UNSET` for an aborted test — an assumption
  that did not hold is not a failure of the thing under test.
- **Ended on the thread that started it**, releasing the context scope it opened there.

Nothing above may depend on tests running one at a time.
The extension holds no per-test state of its own: the span and its scope live in the store JUnit
already scopes to that test's context, so two tests in flight on two threads cannot see each
other's.
This repository enables `junit.jupiter.execution.parallel.enabled` for Surefire and has not yet set
`parallel.mode.default` to `concurrent`, so the guarantee is not currently exercised by the suite —
it is stated and tested here so that switch can be thrown without reopening this spec.

A test with no identity (`SW-046`) gets no span.
There is nothing to name it after, and an unnamed span per test is noise rather than signal.

The verdict is read from JUnit's own record of it on the test's `ExtensionContext`, within the
after-test callback, rather than from `TestWatcher`.
That is a deliberate narrowing: `TestWatcher` reports only `@Test` and `@TestTemplate` methods, is
documented as running after the context's closeable resources have been closed, and gives no
guarantee the extension can rely on about which thread it runs on.
Reading the execution exception in the after-test callback is a stable API, runs on the test's own
thread, and covers every test kind.

The span is explicitly **not** the identity's transport.
Snow-White's correlation reads `test.case.name` from the server span the service produced
(`SW-032`), which it gets from baggage, not from this span's parentage.
This span exists so that a trace is navigable by a human and nestable by an in-process caller — if
it were deleted, every `ARCH-020` channel would still work.

**Rationale**
Making the span current is the decision; the first cut of the extension started a span and never
did, which is what made it worthless.
An un-current span parents nothing: every request the test issues opens its own root trace, so the
test span is one extra orphan trace per test and the thing it was created to group is not grouped.
That is strictly worse than no span at all, because it costs export bandwidth and storage to say
nothing.

Scoping the span to the whole test rather than to the test method body is what makes it useful for
this repository's suites.
A Citrus application test does most of its HTTP work inside the test method, but a `@BeforeEach`
that seeds a quality gate through the API is exercising the API too, and a span that started after
setup would leave that traffic outside the trace while the baggage channel still named it.
Both channels covering the same window is what keeps the two consistent.

Ending the span on the starting thread is not an implementation detail.
A context scope is thread-confined: closing one from another thread corrupts that thread's context
rather than the intended one, and the failure is silent and remote.
Keeping the open and the close
in a matched pair of callbacks JUnit runs on one thread is what makes the invariant structural
instead of a property of the current engine's scheduling.
It is also what lets the extension hold no cross-callback state of its own — the per-test values
live in the context JUnit already scopes per test, which is why this class can be unit-tested at
all.

Mapping an aborted test to `UNSET` rather than `ERROR` follows what an abort means: the test
declined to judge, so the span carries no verdict rather than a false one.
An `ERROR` there would
make a span's status say the service misbehaved when the suite merely skipped a case.

**Verification Description**
A test runs a passing test through a real JUnit engine against an in-memory span exporter and
asserts exactly one span was exported, named for the test's identity, of kind `INTERNAL`, carrying
`test.case.name` as an attribute equal to its name, with status `OK`.
A test asserts a failing test yields one span with status `ERROR` carrying a recorded exception
whose message is the assertion's, and that an aborted test — a failed assumption — yields `UNSET`
with no recorded exception.
A test asserts a span created inside the test body is a child of the test's span, which is what
"current" means observably.
A test asserts the span is no longer current once the test has finished, and that no scope leaks
into the next test on the same thread.
A test asserts a test whose identity is absent produces no span.
A test asserts a `@ParameterizedTest` with two invocations produces two spans with two different
names.
A test asserts each span is ended once — a second end is not emitted — by exporting and counting.
A test runs a class of tests under `@Execution(CONCURRENT)` and asserts every test still gets its
own span, named for itself, with no span left current afterwards — the claim that the per-test state
is JUnit's and not the extension's.

## Relations

**Realizes**

- [ARCH-013](ARCH-013-test-identity-travels-as-baggage.md) — the runner side whose span this is,
  distinct from the server span the attribute is read off

**Related**

- [SW-046](SW-046-test-case-name-is-the-qualified-invocation.md) — the identity that names
  this span and fills its attribute
- [ARCH-020](ARCH-020-test-identity-published-not-propagated.md) — why this span is not the
  identity's transport
- [CON-013](CON-013-extension-never-changes-a-test-verdict.md) — the rule that applies when
  no tracer is available to make a span with
- [SW-032](SW-032-test-identity-on-the-span.md) — the server-side attribute Snow-White actually
  reads, which this span does not replace

## Changes

- **2026-10-07** — Set active: implementation of `STR-026` began.
