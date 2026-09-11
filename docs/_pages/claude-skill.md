---
title: 'Claude Code Skill'
permalink: /claude-skill/
toc: true
toc_sticky: true
---

Snow-White ships a [Claude Code](https://claude.com/claude-code) skill that teaches Claude how to read a Snow-White JUnit XML report and turn failing criteria into concrete test improvements — without you having to explain the report format every time.

The skill lives in the repository at [`.apm/skills/snow-white/`](https://github.com/bbortt/snow-white/tree/main/.apm/skills/snow-white) and is published as an [APM](https://microsoft.github.io/apm) (Agent Package Manager) package, so you can install and update it like any other dependency.

## Why this matters for agentic development

When an AI coding agent writes both the implementation and the tests that exercise it, a green
test suite stops being independent evidence — it can just as easily mean the agent's tests agree
with the agent's code, not that either actually matches the API contract you specified.
Snow-White
sits outside that loop: it correlates real OpenTelemetry traces from the running application
against the OpenAPI spec on file, so coverage results stay a ground-truth signal no agent can
satisfy by construction.

The [`--agentic` flag](/cli/#calculate) on `snow-white calculate` is the direct integration point:
instead of human-readable progress logs, it prints one line of JSON with the pass/fail result and
a per-criterion breakdown of what's still undercovered — built for a coding agent (or this skill)
to parse and act on directly, without scraping a report format meant for people.

## What it does

Once installed, Claude automatically applies the skill whenever it encounters a Snow-White JUnit XML report, a quality gate failure, or a request like "fix the failing coverage criteria." It:

1. Parses the JUnit XML structure Snow-White emits (`testsuites` → `testsuite` per API → `testcase` per criterion).
2. Detects **correlation failures** (`tests="0"`) and walks through the [onboarding checklist](/onboarding/#checklist) instead of treating it as a test gap.
3. Looks up each failing criterion against the [Quality Gate Criteria](/quality-gate-criteria) hierarchy, so it fixes the highest-level failing criterion first instead of duplicating work on children that will pass automatically once the parent does.
4. Proposes integration tests — real HTTP calls with the OTEL agent attached, since Snow-White correlates traces, not mocks — matching your existing test style.
5. Summarizes what was fixed, what criteria that resolves, and what's left.

The package bundles `references/criteria-hierarchy.md` alongside the skill, so Claude can look up the full criteria hierarchy offline rather than fetching this site.

## Installation

### With APM (recommended)

[Install the APM CLI](https://microsoft.github.io/apm/getting-started/installation/), then add the skill to your project's `apm.yml`:

```yaml
dependencies:
  apm:
    - bbortt/snow-white/.apm/skills/snow-white#v1.10.0
```

```shell
apm install
```

Pin a tag from the [releases page](https://github.com/bbortt/snow-white/releases) rather than tracking `main`.
APM packaging ships from the first release containing `apm.yml`; the example above is illustrative.

The `/.apm/skills/snow-white` path segment matters.
Without it APM resolves the whole Snow-White monorepo (~18 MB) into `apm_modules/` to deploy two files; with it, only the skill directory is fetched.

APM deploys the skill to the directory your harness expects — `.claude/skills/snow-white/` for Claude Code, and `.agents/skills/snow-white/` for the harnesses that share that location.
Run `apm update` to pick up a newer release.

### Manual copy

If you would rather not add APM, copy the two files directly:

```shell
mkdir -p .claude/skills/snow-white/references
curl -Lo .claude/skills/snow-white/SKILL.md \
  https://raw.githubusercontent.com/bbortt/snow-white/main/.apm/skills/snow-white/SKILL.md
curl -Lo .claude/skills/snow-white/references/criteria-hierarchy.md \
  https://raw.githubusercontent.com/bbortt/snow-white/main/.apm/skills/snow-white/references/criteria-hierarchy.md
```

Claude Code picks up skills automatically from `.claude/skills/` — no further configuration needed.
The next time Claude sees a Snow-White JUnit report in your conversation, it will apply the workflow above.
