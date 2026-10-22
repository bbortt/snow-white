# Specification reference resolution is off by default and is an operator setting

<!-- markdownlint-disable MD036 -->

**Title**
Specification reference resolution is off by default and is an operator setting

**Lens**: NF

**Status**: active

**Description**
When a sync cycle parses a candidate file it reads the document as written and does not follow its
`$ref` pointers.
An operator may turn resolution on per deployment with a single setting, which is off by default.
With resolution off, a specification that reaches its identity fields through a `$ref` yields no
identity, and is skipped and counted under its own reason
([SW-034](SW-034-unreadable-spec-skipped-unless-strict.md)) rather than failing the cycle.
With resolution on, the parser dereferences each `$ref` — including ones naming sibling files and
remote URLs — and such a specification indexes.
The setting changes only how much of the document is assembled before the identity is read; it
changes neither the paths the identity is read from
([ARCH-015](ARCH-015-identity-declared-in-the-specification.md)) nor what is published for a
specification whose identity resolves either way.

**Rationale**
Resolution is the dominant cost of a sync cycle.
The parser dereferences every `$ref` in the document, reaching out to the filesystem and the network
once per pointer, for a job that then reads a handful of `info` fields and discards the rest.
Across a whole repository that is a great deal of I/O bought for nothing, and it lands on the same
shared Artifactory instance the fan-out bound
([NF-009](NF-009-bounded-sync-fan-out-with-backpressure.md)) exists to protect — a bound on
concurrency does not help if each unit of work is itself unbounded.
Off is therefore the right default: the OpenAPI specification does not permit `$ref` inside `info`,
so the fields this job reads are literal in a conforming document.
It is a setting rather than a constant because the repositories this job is pointed at are not
Snow-White's, and a publishing pipeline that assembles a specification from fragments is a real
shape; for those deployments the I/O buys something, and the alternative is a specification that
silently never indexes with no way to change that short of a code change
([NF-007](NF-007-tempo-search-limit-is-operator-configurable.md) makes the same call for the Tempo
search limit).

**Verification Description**
A test drives one candidate file through a cycle with nothing configured and asserts the parser was
asked not to resolve, so a deployment that sets nothing performs no dereferencing.
A second test sets the property and asserts the same cycle asks the parser to resolve.
A third asserts the property's own default is off, so the default survives a binding that never sees
the key.
The first fails if resolution is turned on anywhere in the path from the property to the parser; the
second fails if the setting is read but not passed on.

## Relations

**Related**

- [NF-009](NF-009-bounded-sync-fan-out-with-backpressure.md) — the fan-out bound this
  per-item bound complements; the two together bound one cycle
- [NF-007](NF-007-tempo-search-limit-is-operator-configurable.md) — the sibling decision to make a
  cost bound an operator setting rather than a constant
- [ARCH-015](ARCH-015-identity-declared-in-the-specification.md) — the identity fields whose
  literalness makes off the safe default
- [SW-034](SW-034-unreadable-spec-skipped-unless-strict.md) — the skip that an unresolvable
  identity falls into
- [SYS-003](SYS-003-openapi-as-indexed-format.md) — the format whose `$ref` mechanism this governs

## Changes

- **2026-09-29** — Set active: implementation of STR-019 began.
