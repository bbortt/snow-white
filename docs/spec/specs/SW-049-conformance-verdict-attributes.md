# A conformance verdict is a boolean plus bounded pointers, read fail-closed

<!-- markdownlint-disable MD036 -->

**Title**
A conformance verdict is a boolean plus bounded pointers, read fail-closed

**Lens**: SW

**Status**: planned

**Description**
A conformance verdict travels on a span as two attributes Snow-White defines, documented in
`semantic-convention/openapi.md` beside `api.name`, `api.version` and `openapi.operation.id`, at
**Development** stability:

- `openapi.response.schema.valid` — boolean.
  Present means a validator judged this response against
  its documented schema; `true` means it conformed.
- `openapi.response.schema.violations` — string array of JSON Pointers, each relative to the
  response schema the document declares for this response.
  Present only where `valid` is `false`.

The contract is that each entry is a JSON Pointer intended to resolve into that response's schema;
how a validator's own message is turned into one is **deferred to the increment that builds the
harvest**, because `com.atlassian.oai.validator` reports an instance path into the payload rather
than a pointer into the schema, and the translation across arrays and `oneOf` is not knowable from
the document alone.
The deferral is safe rather than open-ended: a pointer that does not resolve already has a defined
outcome — [SW-029](SW-029-finding-identified-by-spec-pointer.md)'s ladder falls back to the shortest
resolvable ancestor — so the worst case of an imperfect translation is a coarser finding, never a
lost one.

Neither is a correlation key.
The pair being judged is located by attributes that already exist: `openapi.operation.id` for the
operation and the upstream `http.response.status_code` for the code.
A span missing either of those identifies no pair and contributes no target, which is the same
outcome as carrying no verdict.

Both keys are operator-configurable on `openapi-coverage-stream`, defaulting to the names above,
exactly as `operationIdAttribute` and `testCaseNameAttribute` are today
([SW-021](SW-021-required-attribute-key-set-derivation.md)).

Three readings are fixed because each is a place a lenient reader would silently lose a failure:

- **Absent `valid` is unjudged, not conforming.** A span with violations but no `valid` is
  malformed, and is read as a violation rather than discarded.
- **`valid=true` alongside a non-empty violation list is a violation.** The two disagree, and the
  reading that fails the gate is the one that cannot hide a real break.
- **A non-boolean or unparseable `valid` is a violation**, logged at `WARN` with the span id and the
  attribute key — never the value.

The violation list is bounded at capture, not at read: at most 32 pointers, each at most 512 UTF-8
bytes, declared as constants on the published event contract the way
`FindingEvidence.MAX_TEST_CASE_NAME_BYTES` is.
Beyond either bound the surplus pointers are dropped — never truncated — the `valid=false` verdict
survives, and the drop is logged at `WARN` with the counts.
A dropped pointer costs a finding's detail, never the finding that the pair did not conform.

**Rationale**
The boolean is what a gate needs and the pointers are what makes a finding actionable; the issue
behind this work posed them as alternatives, and they are not — the boolean carries the verdict and
the pointers carry only detail, which is why the bound can discard pointers without weakening the
verdict.

Defining these rather than adopting an upstream convention is forced: OpenTelemetry's registry has
no notion of a response conforming to an API description, so there is nothing to adopt.
That makes them unlike `test.case.name` ([SW-032](SW-032-test-identity-on-the-span.md)), which is
read because it already existed upstream.
Development stability is honest about a contract this new, and the whole of
`semantic-convention/openapi.md` is already at that level, so the addition changes no promise.

Making every ambiguous reading fail closed is the opposite of the usual permissiveness in this
corpus, and it is deliberate.
Elsewhere a missing attribute costs evidence or leaves a target unjudged — nothing is at stake but
detail.
Here a malformed verdict is the one case where a reader's charity converts a real contract break
into a green gate, and a criterion that can be silenced by emitting a malformed attribute is worth
less than no criterion.
A reader flipping to a known-good interpretation also mirrors the Sonar S1940 trap this repository
has already paid for once: "the opposite operator" is not equivalence-preserving when an operand can
be absent or unparseable.

Bounding at capture rather than at the column follows the precedent set when `test.case.name` gained
a bound: the producing service and the storing service are deployed separately, so the limit belongs
on the contract both see.
32 and 512 are chosen to keep a pathological response — a large array failing one field per
element — from turning a single span into thousands of findings, which would make the report
unreadable and the finding table unbounded in the size of someone else's payload.

**Verification Description**
A unit test asserts a span with `valid=true` and no violations yields a conforming verdict, and one
with `valid=false` and two pointers yields a non-conforming verdict carrying both.
A test asserts `valid=true` with a non-empty violation list is read as non-conforming.
A test asserts violations present with `valid` absent is read as non-conforming.
A test asserts a non-boolean `valid` is read as non-conforming and logged at `WARN` without the
value.
A test asserts a span with a verdict but no `openapi.operation.id`, and one with no status code,
each contribute no target.
A test asserts a 33rd pointer and an over-long pointer are dropped with the verdict preserved, and
that the bounds are the constants on the event contract rather than literals in either service.
A test asserts non-default configured keys are honoured and the defaults are the two names above.
A documentation check asserts `semantic-convention/openapi.md` lists both attributes with their
stability — deferred for the reason `SW-032` records if no docs-assertion pattern exists yet.

## Relations

**Related**

- [SW-048](SW-048-response-schema-conformance-criterion.md) — the criterion that reads these
  attributes
- [ARCH-023](ARCH-023-conformance-verdict-is-harvested-not-recomputed.md) — what produces
  the values, and why the bound is applied there
- [ARCH-024](ARCH-024-harness-publishes-under-the-api-identity.md) — how a span carrying
  them reaches the analysed dataset
- [SW-021](SW-021-required-attribute-key-set-derivation.md) — the key set these two join
- [SW-032](SW-032-test-identity-on-the-span.md) — the adopted-attribute precedent these two
  deliberately depart from, and the capture-side byte bound they copy
- [SW-029](SW-029-finding-identified-by-spec-pointer.md) — the pointer model a violation's pointer
  is resolved into
- [CON-001](CON-001-deterministic-analysis-results.md) — the determinism the fail-closed readings
  keep, by leaving no outcome dependent on how charitable a reader was
