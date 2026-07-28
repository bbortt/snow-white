---
title: 'Claude Code Skill'
permalink: /claude-skill/
toc: true
toc_sticky: true
---

Snow-White ships a [Claude Code](https://claude.com/claude-code) skill that teaches Claude how to read a Snow-White JUnit XML report and turn failing criteria into concrete test improvements — without you having to explain the report format every time.

The skill lives in the repository at [`.claude/skills/snow-white/snow-white.skill`](https://github.com/bbortt/snow-white/blob/main/.claude/skills/snow-white/snow-white.skill).

## What it does

Once installed, Claude automatically applies the skill whenever it encounters a Snow-White JUnit XML report, a quality gate failure, or a request like "fix the failing coverage criteria." It:

1. Parses the JUnit XML structure Snow-White emits (`testsuites` → `testsuite` per API → `testcase` per criterion).
2. Detects **correlation failures** (`tests="0"`) and walks through the [onboarding checklist](/onboarding/#checklist) instead of treating it as a test gap.
3. Looks up each failing criterion against the [Quality Gate Criteria](/quality-gate-criteria/) hierarchy, so it fixes the highest-level failing criterion first instead of duplicating work on children that will pass automatically once the parent does.
4. Proposes integration tests — real HTTP calls with the OTEL agent attached, since Snow-White correlates traces, not mocks — matching your existing test style.
5. Summarizes what was fixed, what criteria that resolves, and what's left.

## Installation

Copy the skill into your own project's Claude Code skills directory:

```shell
mkdir -p .claude/skills/snow-white
curl -Lo .claude/skills/snow-white/snow-white.skill \
  https://raw.githubusercontent.com/bbortt/snow-white/main/.claude/skills/snow-white/snow-white.skill
```

Claude Code picks up skills automatically from `.claude/skills/` — no further configuration needed.
The next time Claude sees a Snow-White JUnit report in your conversation, it will apply the workflow above.
