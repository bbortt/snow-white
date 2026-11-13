# A managed instance refuses a gate write with 409, and keeps every read

<!-- markdownlint-disable MD036 -->

**Title**
A managed instance refuses a gate write with 409, and keeps every read

**Lens**: SW

**Status**: planned

**Description**
While an instance's gates are file-owned
([ARCH-TMP-001](ARCH-TMP-001-gitops-mode-is-instance-configuration.md)),
`POST /api/rest/v1/quality-gates`, `PUT /api/rest/v1/quality-gates/{name}` and
`DELETE /api/rest/v1/quality-gates/{name}` answer `409` with the surface's `Error` body, carrying a
code that names the mode.
No record is read or written in the course of the refusal: the answer does not depend on whether the
named gate exists, so a `404` never pre-empts it and a probe cannot use the refusal to enumerate
gates.

`GET /api/rest/v1/quality-gates`, `GET /api/rest/v1/quality-gates/{name}` and the criteria catalog
([SW-012](SW-012-criteria-catalog-endpoint.md)) answer exactly as they do with the mode off, status
codes included.
Read access is not what the mode restricts.

`409` and not `405`: the method is routed, implemented and available on this path — the same build
serves it with the mode off — so a claim that the method is not allowed is false.
What conflicts is the request against the instance's declared ownership, which is a state conflict,
and the same status this surface already uses for one ([SW-009](SW-009-quality-gate-crud-contract.md)
answers `409` when a name is taken).
`403` is also wrong: nothing about the caller's identity or permissions produced the refusal, and
no credential would change the outcome.
A caller distinguishes the two `409`s by the `Error` body's `code`, which is why sharing the status
costs them nothing.

With the mode off, none of these paths can answer `409` for this reason, and `SW-009`'s contract is
unchanged.

**Rationale**
Two writers and no arbiter is worse than one bad writer: a gate can disagree with the repository
that claims to define it and nothing says which is wrong.
Closing the REST writes is what makes the file authoritative rather than merely first.

Refusing before touching a record keeps the refusal a statement about the instance rather than about
the target.
If the mode were enforced after a lookup, a `DELETE` of a nonexistent gate would answer `404` and a
caller would reasonably conclude writes work here.

**Verification Description**
An application test with the mode on asserts `409` for create, update and delete — against an
existing gate, a nonexistent name and a predefined gate alike — with the mode-specific error code,
and asserts the existing gate is unchanged afterwards.
The same test asserts the gate list, a gate by name and the criteria catalog still answer `200` with
the same bodies.
A test with the mode off asserts every status code `SW-009` lists is still produced, and that `409`
on create still means only that the name is taken.

## Relations

**Realizes**

- [CON-TMP-001](CON-TMP-001-managed-instance-has-exactly-one-gate-writer.md) — the one-writer
  invariant, enforced at this surface

**Related**

- [SW-009](SW-009-quality-gate-crud-contract.md) — the contract this suspends, and whose `409` this
  one shares
- [ARCH-TMP-001](ARCH-TMP-001-gitops-mode-is-instance-configuration.md) — the switch that decides
  whether the refusal applies
- [SW-TMP-001](SW-TMP-001-startup-makes-the-gate-set-exactly-the-file.md) — the write path that
  remains open, and the only one
- [SW-TMP-003](SW-TMP-003-managed-mode-published-on-gate-reads.md) — how a client avoids earning
  this refusal
- [SW-012](SW-012-criteria-catalog-endpoint.md) — the catalog read the mode leaves alone
- [CON-010](CON-010-rest-responses-never-carry-null.md) — the response shape the refusal body obeys
