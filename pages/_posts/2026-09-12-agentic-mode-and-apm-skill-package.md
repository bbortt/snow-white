---
title: 'Release 1.10.0: agentic CLI mode, the skill as an APM package, and Tempo v2'
excerpt: >
  Snow-White now speaks a language coding agents can parse directly, the
  Claude Code skill installs as a proper APM package, and Grafana Tempo
  queries move to the v2 trace API for compatibility with newer backends.
tags:
  - release
  - agentic
  - cli
  - claude-code
---

The upcoming 1.10.0 release is about closing the loop between Snow-White and the coding agents
that increasingly write the code it's supposed to be checking.

## `snow-white calculate --agentic`

The CLI's `calculate` command has always been usable in CI, but its output was designed for a
human watching a terminal: progress logs, colored checkmarks, a final pass/fail line.
Fine for a
pipeline log, useless for a coding agent trying to decide what to fix next.

The new `--agentic` flag replaces all of that with a single line of structured JSON:

```shell
snow-white calculate \
  --config-file snow-white.json \
  --agentic
```

```json
{
  "schemaVersion": "1",
  "status": "FAILED",
  "calculationId": "...",
  "qualityGateConfigName": "default",
  "apiLocation": "http://snow-white.example.com/api/rest/v1/reports/...",
  "initiatedAt": "2026-09-12T08:00:00.000Z",
  "summary": { "apiCount": 12, "failedApiCount": 3, "qualityGateFailureCount": 5 },
  "interfaces": [ ... ]
}
```

No progress logs to strip out, no ANSI colors to parse around — one JSON object naming exactly
which interfaces failed and why.
It's mutually exclusive with `--async`, since agentic mode
needs the finished result to report and async mode returns before one exists.
Full shape and an
example are documented on the [CLI Reference](/cli/#calculate).

This is also the flag the [Claude Code skill](/claude-skill/) is built around: instead of
scraping a JUnit XML report meant for humans, the skill (or any agent) can invoke `--agentic` and
get a backlog it can act on directly.

## The skill becomes an APM package

Speaking of the skill — installing it used to mean copying `SKILL.md` into your repo by hand.
It's moving to [APM](https://microsoft.github.io/apm) (Agent Package Manager), so it can be
declared as a dependency and updated like one:

```yaml
# apm.yml
dependencies:
  apm:
    - bbortt/snow-white/.apm/skills/snow-white#v1.10.0
```

```shell
apm install
```

`.apm/skills/snow-white/` becomes the source of truth in the repository; `.claude/skills/snow-white/`
stays as a generated, committed copy so Claude Code sessions working inside this repo keep
working without the APM CLI installed.
Manual copying remains an option if you'd rather not add
APM as a dependency — see the [Claude Code Skill](/claude-skill/) page for both paths.

## Grafana Tempo: migrating to the v2 trace API

A smaller but important fix for anyone using the Grafana Tempo integration introduced back in
1.7.0: `openapi-coverage-stream` queried Tempo's trace-by-id endpoint at its original, unversioned
path (`/api/traces/{traceId}`).
That was never formally deprecated in Tempo's docs, so we kept
assuming it would keep working — until testing 1.9.0 against a newer Grafana-managed Tempo
instance turned up one with no v1 endpoint exposed at all.

1.10.0 moves that query to the explicit `/api/v2/traces/{traceId}` endpoint, which is what
current and newer Grafana Tempo backends actually expect.
If you're running Snow-White against a
recent Tempo instance and coverage calculation was silently failing to resolve trace details,
this release should fix it — no configuration changes required.
