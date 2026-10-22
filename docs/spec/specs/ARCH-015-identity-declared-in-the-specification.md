# The indexed identity is declared inside the specification document, at overridable paths

<!-- markdownlint-disable MD036 -->

**Title**
The indexed identity is declared inside the specification document, at overridable paths

**Lens**: ARCH

**Status**: active

**Description**
The (service name, API name, API version) triple a specification is indexed under is read out of the
specification document itself, not from the file's name or location, not from a registry Snow-White
keeps, and not from anything the publishing pipeline passes alongside it.
Each of the three is read at a JSON path into the parsed document, and each path is an operator
setting.
The defaults are `info.title` for the API name, `info.version` for the version, and the
`x-service-name` vendor extension on `info` for the OTel service name.
A path that does not resolve yields no value rather than an error, which leaves the specification
short of a mandatory field and so unpublishable
([SW-034](SW-034-unreadable-spec-skipped-unless-strict.md)).
The same extraction, with the same defaults, is what the CLI applies to a prerelease it uploads, so
a specification resolves to one identity whichever path it enters Snow-White through.

**Rationale**
The service name is the join between a specification and the telemetry it is correlated against — it
has to equal the `service.name` on the spans ([STK-001](STK-001-correlate-specs-with-telemetry.md)).
Keeping it in the document puts it under review, in the same commit, next to the API it describes,
where a mismatch is visible to the person who can fix it.
Any external carrier — a filename convention, a registry entry, a pipeline variable — puts it
somewhere the spec author does not look, and the failure it causes appears as an analysis with no
matching spec rather than as a wrong value.
OpenAPI has no field for it, so a vendor extension is the only in-document home available; the
paths are settable because a house convention may already carry these three values elsewhere in the
document, and forcing a spec to be edited to be indexable would be a poor trade against reading it
from where it already is.

**Verification Description**
A test extracts the triple from a document carrying the three values at the default locations and
asserts each is read.
A second test points the three paths at different locations in a differently shaped document and
asserts the values are read from there instead.
A third asserts an unresolvable path yields no value rather than raising.

## Relations

**Related**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — the correlation the service name is the
  join for
- [SYS-002](SYS-002-indexed-spec-lookup.md) — the addressing scheme the extracted triple keys into
- [SYS-003](SYS-003-openapi-as-indexed-format.md) — the format the paths are read from
- [SYS-013](SYS-013-prerelease-specification-ingestion.md) — the CLI upload path that applies the
  same extraction
- [SW-034](SW-034-unreadable-spec-skipped-unless-strict.md) — what happens to a document a
  path cannot be resolved in

## Changes

- **2026-09-29** — Set active: implementation of STR-019 began.
