# Quality-gate configurations support a full CRUD lifecycle with defined conflict, not-found, and immutability errors

<!-- markdownlint-disable MD036 -->

**Title**
Quality-gate configurations support a full CRUD lifecycle with defined conflict, not-found, and
immutability errors

**Lens**: SW

**Status**: active

**Description**
`quality-gate-api` exposes `/api/rest/v1/quality-gates` with:

- **Create** (`POST`) — `201` with a `Location` header on success; `409` if the name already
  exists.
- **List** (`GET`) — paginated, with total count returned via an `X-Total-Count` header.
- **Get by name** (`GET /{name}`) — `200`, or `404` if no gate with that name exists.
- **Update** (`PUT /{name}`) — `200` on success; `404` if the gate does not exist; `400` if the
  target gate is predefined, or if the request references a criterion name that does not exist.
- **Delete** (`DELETE /{name}`) — `204` on success; `404` if the gate does not exist; `400` if
  the target gate is predefined.

The predefined-target `400` on update and delete is this component's enforcement of
[CON-003](CON-003-predefined-quality-gates-are-immutable.md); every other status above is this
component's own contract, independent of that constraint.

**Rationale**
A named-resource CRUD contract needs an explicit, stable status code per outcome so a caller (the
`api-gateway` UI, the CLI, or a script) can branch on the result without parsing a message string.
Rejecting the predefined-gate cases at this layer, rather than only preventing the underlying
data from changing, gives the caller a diagnosable `400` instead of a silent no-op or an
unrelated failure.

**Verification Description**
A test suite exercises every listed operation against both a fresh (nonexistent) name, an
existing user-defined gate, and an existing predefined gate, asserting the exact status code
listed for each combination — matching `QualityGateApiAppTest`'s existing black-box coverage.

## Relations

**Realizes**

- [CON-003](CON-003-predefined-quality-gates-are-immutable.md) — enforced via this contract's
  `400` responses on update/delete of a predefined gate

**Related**

- [SYS-008](SYS-008-quality-gate-definitions.md) — the capability this CRUD surface implements
- [SW-010](SW-010-update-merge-patch-empty-criteria-clears.md) — the update
  operation's field-level semantics
- [CON-005](CON-005-api-created-gates-are-never-predefined.md) — why create can never
  produce a gate this contract's predefined-target rules would apply to

## Changes

- **2026-09-17** — Set active: anchored against `quality-gate-api`'s existing implementation
  (`STR-009`).
