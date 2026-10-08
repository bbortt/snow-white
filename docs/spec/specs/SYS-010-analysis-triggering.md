# Analysis triggerable via CLI and HTTP API, scoped to an explicit API and quality gate

<!-- markdownlint-disable MD036 -->

**Title**
Analysis triggerable via CLI and HTTP API, scoped to an explicit API and quality gate

**Lens**: SYS

**Status**: active

**Description**
Snow-White exposes two ways to trigger an analysis — a command-line interface and an HTTP-based
API — and both require the caller to name the exact API (service name, API name, version) and the
quality gate to evaluate.
There is no implicit "analyze everything" trigger.

**Rationale**
CI pipelines need a scriptable CLI entry point; other automation and the web UI need a
programmatic HTTP entry point.
Requiring the API and gate to be named explicitly keeps a trigger unambiguous and keeps the
analysis scoped, rather than fanning out to every indexed API.

**Verification Description**
A test triggers an analysis through the CLI naming a specific API and gate, and a second test
triggers the equivalent analysis through the HTTP API; both assert the analysis ran only for the
named API and gate.

## Relations

**Realizes**

- [STK-001](STK-001-correlate-specs-with-telemetry.md) — the entry point into the
  correlation

**Related**

- [SYS-002](SYS-002-indexed-spec-lookup.md) — how the named API resolves
- [SYS-011](SYS-011-bounded-time-resolution.md) — what happens after a trigger
