# A finding is identified by an RFC 6901 pointer into the spec, with its discriminators denormalized beside it

<!-- markdownlint-disable MD036 -->

**Title**
A finding is identified by an RFC 6901 pointer into the spec, with its discriminators denormalized
beside it

**Lens**: SW

**Status**: active

**Description**
Every finding names its target by a `specPointer`: an RFC 6901 JSON Pointer into the OpenAPI
document the calculation ran against, resolving to the node the criterion judged.

```text
/paths/~1pung~1{message}/get/responses/404
```

The pointer is built from the spec's own keys, with RFC 6901 escaping applied (`~` as `~0`, `/` as
`~1`) — so a templated path appears exactly as the document spells it, braces included, and is not
normalised, lowercased, or rewritten to a concrete observed path.
It points at the deepest node the criterion actually judges: a response code entry for the
response-code criteria, a parameter entry for the parameter criteria, a content-type entry under
its media-type map for content-type coverage, and the operation node itself for the
operation-level criteria.
An operation's `parameters` is a JSON array, so that rung of the pointer is the parameter's
position in it — `/parameters/0`, not `/parameters/page`.
The name is not lost: it is on the `parameterName` discriminator, which is what a consumer reads it
from.
Position is the only thing RFC 6901 can address in an array, and `CON-007` is what makes it stable
— an indexed reference never changes underneath a pointer, so the index cannot come to mean a
different parameter than the one judged.

Three criteria invert this direction (`SW-003`): `NO_UNDOCUMENTED_RESPONSE_CODES` and its positive
and error variants judge observed status codes against the spec, so their target is an observed
code that may have no node in the document at all.
For them the pointer addresses the **nearest container the document does contain** — the
operation's `responses` map, `/paths/~1pung~1{message}/get/responses` — and the `responseCode`
discriminator carries which code was judged.
"Nearest container" is a ladder rather than a single depth, because the document can stop short at
either level above that map: the operation's `responses` map where the operation documents one,
else the operation node itself, else `/paths` where the telemetry resolved to no documented
operation at all.
The last rung is reached by telemetry for an endpoint the document never describes, which is the
most severe undocumented-response finding there is — dropping it for want of a pointer would lose
exactly the case the criterion exists to catch.
On that rung alone `httpPath` and `httpMethod` carry the observed concrete path and method rather
than a template, because there is no template to name; nothing in the pointer contradicts them, as
it has no path or method segment to disagree with.
A pointer is therefore always present and always resolvable; it is never null, and never invented
for a node that does not exist.

The pointer is the finding's identity within its criterion result, **together with the
discriminators** — for the inverted criteria, several findings legitimately share one container
pointer and are told apart by `responseCode`.
It is that pair — not insertion order, not a generated id — that decides what a redelivered result
replaces.

The discriminators the issue specifies are each nullable and each populated only where the
criterion's target has that dimension: `httpPath`, `httpMethod`, `responseCode`, `parameterName`,
`contentType`.
On the eleven forward criteria they are pure duplication — every one of them is already encoded in
the pointer, and they are stored for query shape alone.
On the three inverted criteria one of them (`responseCode`) carries what the pointer structurally
cannot, which is why identity is the pair rather than the pointer.

**Rationale**
A finding has to be addressable by something stable enough to match a target across two
calculations of the same spec, and specific enough that a consumer can go look at what failed.
A JSON Pointer is both, and it is the OpenAPI ecosystem's own existing answer — the same form
`$ref` resolution and validator diagnostics already use — so a developer who lands on one can
resolve it against the spec they already have, with no Snow-White-specific decoding.
It is also the natural key for the criterion-level waivers of #2009, whose unit of exemption is a
target: a waiver written against a pointer survives a spec edit elsewhere in the document, which a
positional or generated id would not.

Refusing to normalise the templated path is the load-bearing part.
The pointer must address the _specification_, because that is what the finding is a verdict about;
a pointer rewritten toward an observed path would address something the document does not contain
and would stop resolving.
The observed concrete path belongs to the evidencing span, which `ARCH-011` keeps reachable —
the two live on opposite sides of the correlation, and collapsing them would lose exactly the
distinction the report exists to show.

Denormalizing the discriminators is a deliberate duplication, accepted for query shape rather than
for reading convenience.
A pointer is an opaque string to SQL: "which findings concern `POST /orders`" or "which findings
concern response code `404` anywhere" are questions a consumer will ask, and answering them by
string-matching pointer fragments would bind every such query to the pointer's grammar.
The duplication is safe because the discriminators are written from the same target as the pointer,
in the same step, and are never edited independently of it.

The inverted criteria are why identity is the pointer **plus** the discriminators rather than the
pointer alone, and this is the one place the shape is a concession rather than a preference.
A pointer can only address a node the document contains, and the whole point of
`NO_UNDOCUMENTED_RESPONSE_CODES` is a code the document does not contain — so the deepest honest
pointer stops at the `responses` map, and several findings share it.
Pointing at a node that does not exist (`.../responses/418` where no `418` is documented) was
rejected: it reads as an assertion that the spec has that entry, which is the precise opposite of
the finding's meaning, and it would not resolve for any consumer that tries.

**Verification Description**
A unit test asserts that for a spec whose path template contains `/` and `{}` characters, the
generated pointer escapes per RFC 6901 and resolves — parsed as a JSON Pointer against the parsed
document — to the node the criterion judged.
A test per criterion family asserts the pointer's depth matches the target it judges (response
entry, parameter entry, media-type entry, or operation node), and that the three inverted criteria
point at the operation's `responses` map with the judged code on `responseCode`.
Every generated pointer is asserted to resolve against the parsed document — including the inverted
criteria's, which is what would fail if a pointer were ever built toward an undocumented entry.
A test asserts no two findings of one criterion result share a pointer _and_ discriminator set, and
that a redelivered result matches its predecessor's findings on that pair.
A test asserts each discriminator is populated exactly where the criterion's target carries that
dimension and null otherwise, and that each agrees with the corresponding segment of the pointer —
except `parameterName`, which has no segment to agree with: there the test resolves the pointer and
asserts the node it lands on declares that name.

## Relations

**Related**

- [ARCH-010](ARCH-010-coverage-derived-from-findings.md) — the contract this identity
  belongs to
- [ARCH-011](ARCH-011-evidence-captured-at-the-match.md) — the evidencing span that holds
  the concrete observed path this pointer deliberately does not
- [ARCH-012](ARCH-012-findings-on-the-event-coverage-as-cache.md) — the persistence and
  replace-by-pointer semantics
- [SW-030](SW-030-unjudged-target-is-not-applicable.md) — the inapplicable targets that are
  pointed at too, rather than skipped
- [SW-003](SW-003-undocumented-response-code-detection.md) — the three inverted criteria whose
  target is an observed code the document may not contain, and the reason identity is the pointer
  plus its discriminators
- [SW-004](SW-004-parameter-coverage-matches-by-token-not-substring.md) — the parameter-matching
  rule whose target a parameter finding points at
- [SW-005](SW-005-content-type-coverage.md) — the content-type target a content-type finding points
  at
- [CON-007](CON-007-stable-api-reference-is-immutable-once-indexed.md) — the immutability of an
  indexed spec, which is what makes a pointer stable across recalculations of the same reference

## Changes

- **2026-09-24** — Recorded that a parameter pointer addresses the parameter's position in the
  operation's `parameters` array rather than its name, and qualified the discriminator agreement
  accordingly.
  Migrating the parameter criteria was what surfaced it: an array is the one
  place RFC 6901 cannot address by name, so `parameterName` is the single discriminator that has no
  pointer segment to be checked against and must be verified by resolving the pointer instead.
  The last three criteria — parameter, content-type, required-error-fields — moved behind the
  contract in the same step, so the response, parameter and media-type pointer depths this spec
  describes are now all exercised.
- **2026-09-24** — Made the inverted criteria's "nearest container" an explicit ladder, adding the
  operation node and `/paths` as the rungs below the `responses` map.
  Implementing `SW-003` established that
  the `responses` map is not always there to point at: an operation may document no responses, and
  telemetry may resolve to no documented operation at all — the latter being the most severe
  finding the criterion produces, so it cannot be dropped for want of a pointer.
  Also recorded that on the `/paths` rung the path and method discriminators carry observed
  concrete values, which is not an exception to their agreeing with the pointer because that
  pointer has no path or method segment.
- **2026-09-23** — Set active: implementation of `STR-017` began.
  The first criterion behind the
  contract is `PATH_COVERAGE`, whose target is a path item, so the pointer depths this spec
  describes for response, parameter and media-type targets are exercised by the later steps that
  migrate those criteria.
