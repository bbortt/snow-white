# A conformance verdict is a boolean plus bounded pointers, read fail-closed

<!-- markdownlint-disable MD036 -->

**Title**
A conformance verdict is a boolean plus bounded pointers, read fail-closed

**Lens**: SW

**Status**: active

**Description**
A conformance verdict travels on a span as two attributes Snow-White defines, documented in
`semantic-convention/openapi.md` beside `api.name`, `api.version` and `openapi.operation.id`, at
**Development** stability:

- `openapi.response.schema.valid` — boolean.
  Present means a validator judged this response against
  its documented schema; `true` means it conformed.
- `openapi.response.schema.violations` — a **string** whose value is a JSON array of JSON Pointers,
  each relative to the response schema the document declares for this response, e.g.
  `["/properties/id","/required"]`.
  Present only where `valid` is `false`.

The violation list is a serialised string rather than an OpenTelemetry array attribute because the
analysed dataset cannot carry an array through both backends alike.
The Tempo backend builds its attributes from the OTLP value members it recognises — `stringValue`,
`intValue`, `doubleValue`, `boolValue` — and silently drops an `arrayValue`, which costs the
pointers without reporting that it did.
The InfluxDB backend carries an array faithfully, because it JSON-encodes each attribute rather
than coercing it to text
([SW-023](SW-023-influxdb-query-narrows-attributes-to-required-keys.md)).
A string passes through both backends unchanged, and any producer can emit one by serialising a
list.
A value that is not a JSON array of strings is unparseable, and read under the rules below.

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

These attributes, together with the span shape that carries them, are a **producer contract**
that does not depend on any framework.
Citrus is the reference producer
([ARCH-023](ARCH-023-conformance-verdict-is-harvested-not-recomputed.md)), not the only
permitted one: Snow-White reads the span, not who emitted it, so any test framework or harness
that emits the following is judged exactly as a Citrus-driven suite is:

- one span per validated response — never two responses' verdicts on one span;
- `service.name` as a **resource** attribute, not a span attribute, equal to the service name the
  specification declares ([ARCH-015](ARCH-015-identity-declared-in-the-specification.md));
- `api.name` and `api.version` as span attributes, equal to the values the specification declares;
- `openapi.operation.id` and `http.response.status_code` as span attributes, locating the pair;
- `openapi.response.schema.valid` and, where it is `false`, `openapi.response.schema.violations`,
  read as described below;
- every **resource** attribute a calculation filters on, with the value the application's own spans
  carry — a calculation's attribute filters are applied to the verdict span exactly as to the
  application's (Tempo matches them as `resource.<key>`), so a verdict span lacking, say, the
  `deployment.environment` the calculation filters on is never read, and the criterion reports no
  verdict at all ([SW-048](SW-048-response-schema-conformance-criterion.md)) rather than the
  verdicts that exist;
- export through any OTLP path that reaches the configured telemetry backend.

A producer need not emit `http.request.method`.
The collector configurations Snow-White ships admit a span into the analysed dataset when it
carries either `http.request.method` or `openapi.response.schema.valid`, alongside `api.name` and
`api.version`; an operator running their own collector filter has to admit the same.

Carrying `openapi.response.schema.valid` also marks the span as a harness span, so it contributes to
this criterion and to no other
([CON-015](CON-015-harness-span-never-moves-a-coverage-verdict.md)).
A producer need not and must not do anything further to keep it out of reach coverage.
This whole contract — not only the two new attributes — is documented in
`semantic-convention/openapi.md`, which is the page user-facing documentation links to for
anyone implementing a producer.

Both keys are operator-configurable on `openapi-coverage-stream`, defaulting to the names above,
exactly as `operationIdAttribute` and `testCaseNameAttribute` are today
([SW-021](SW-021-required-attribute-key-set-derivation.md)).

Four readings are fixed because each is a place a lenient reader would silently lose a failure:

- **Absent `valid` is unjudged, not conforming.** A span with violations but no `valid` is
  malformed, and is read as a violation rather than discarded.
- **`valid=true` alongside a non-empty violation list is a violation.** The two disagree, and the
  reading that fails the gate is the one that cannot hide a real break.
- **A non-boolean or unparseable `valid` is a violation**, logged at `WARN` with the span id and the
  attribute key — never the value.
- **An unparseable violation list is a violation**, whatever `valid` states alongside it, logged the
  same way.
  A value that is not a JSON array of strings states no violation list, so there is nothing for
  `valid=true` to be read against, and reading it as an empty list would let one malformed attribute
  silence the criterion.
  An empty array is not that case: it states a list, and `valid=true` alongside it conforms.

The violation list is bounded at most 32 pointers, each at most 512 UTF-8 bytes, declared as
constants on the published event contract the way `FindingEvidence.MAX_TEST_CASE_NAME_BYTES` is.
The bound is applied twice: at capture by a producer Snow-White ships, and again at read by
`openapi-coverage-stream`, because the producer contract admits producers Snow-White does not ship
and cannot vouch for.
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

Bounding on the contract rather than at the column follows the precedent set when `test.case.name`
gained a bound: the producing service and the storing service are deployed separately, so the limit
belongs on the contract both see.
Applying it at read as well is what keeps the bound a guarantee rather than a courtesy once a
third-party producer is in the picture; applying it at capture too keeps the shipped producer from
exporting what the reader would only discard.
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
A test asserts a violation list that is not a JSON array of strings is read as non-conforming
alongside `valid=false` and alongside `valid=true` alike, and that an empty array alongside
`valid=true` conforms.
A test asserts, on the reading side, a 33rd pointer and an over-long pointer are dropped with the
verdict preserved, and
that the bounds are the constants on the event contract rather than literals in either service.
A test asserts non-default configured keys are honoured and the defaults are the two names above.
A test asserts a span emitted directly through the OpenTelemetry SDK — no Citrus on the
classpath — that meets the producer contract yields the same verdict as a Citrus-harvested one.
A documentation check asserts `semantic-convention/openapi.md` lists both attributes with their
stability and states the full producer contract above — deferred for the reason `SW-032` records if
no docs-assertion pattern exists yet.

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
