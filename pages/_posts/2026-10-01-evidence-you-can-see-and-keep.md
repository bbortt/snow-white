---
title: 'Release 1.13.0: evidence you can see — and keep'
excerpt: >
  1.12.0 put findings underneath every coverage ratio but gave you no way to
  look at them. 1.13.0 adds the drilldown that does, plus two ways to take the
  whole report with you: a download button in the UI and `--report-output` in
  the CLI.
tags:
  - release
  - coverage
  - traceability
  - cli
---

Last release ended on an admission: findings were computed, persisted and served, but
"you can't _see_ findings in Snow-White yet".
That was the missing half of
[#1642 — drill into which tests covered a criterion](https://github.com/bbortt/snow-white/issues/1642).
1.13.0 is that half.

## The drilldown

Open any criterion in a report and it now unfolds into the spec locations it actually evaluated,
grouped by what happened to each: **uncovered first**, then covered, then not applicable.
Uncovered leads because it is the only group you can act on.

Each finding names its location — path and method, plus the response code, parameter or content
type where the criterion works at that granularity.
Covered ones carry their evidence: the traces that satisfied them, shown as shortened trace IDs
with a copy button, and the test name where a test identified itself through
`test.case.name` baggage.
Named tests are listed individually; anonymous traces collapse after the first three.
Long groups show their first 20 findings with the rest one click away, because a 14-criteria API
test can produce a lot of them and an accordion that dumps all of it is not a drilldown.

Nothing is required on your side.
Every report calculated on 1.12.0 or later already carries the
findings this renders.

## Taking the report with you

The screen is good for "why did this fail".
It is not good for attaching to a ticket six weeks
later, and it was never going to be good for a pipeline.
So there are now two ways to get the report itself:

**In the UI**, a _Download Report JSON_ button next to the existing _Download JUnit Report_ one.

**In the CLI**, a new flag beside `--junit-output`:

```shell
snow-white calculate \
  --config-file snow-white.json \
  --junit-output snow-white-junit.xml \
  --report-output snow-white-report.json
```

Both produce the **same bytes** — the `GET /reports/{id}` response body verbatim, findings and
evidence included.
Not a trimmed-down "evidence file", not a re-serialised view: one payload, one
schema, already versioned by the report API.
[ADR-0003](https://github.com/bbortt/snow-white/blob/main/docs/adr/ADR-0003-evidence-is-exported-as-the-reports-own-json.md)
records why, including why the evidence did _not_ go into the JUnit XML — a build server is the one
consumer that acts on that document, and it acts on pass/fail/skip.

`--report-output` writes the file whether or not `--agentic` is set, and refuses to combine with
`--async`, where there is no result to report.

## Also in this release

- **Spec sync** now has a stated contract for its cadence and for how it behaves when an API's
  identity is unreadable, and it counts every sync cycle under the outcome it actually reached —
  so a cycle that failed halfway is no longer indistinguishable from one that did nothing.
- **REST responses no longer carry null-valued properties**; an unset property is simply absent.
  If you consume the API with a generated client this is a non-event, and if you parse it by hand
  it means one less case to handle.
- The usual round of UI fixes behind the new drilldown.

The full list is in the
[changelog](https://github.com/bbortt/snow-white/blob/main/CHANGELOG.md#1130-2026-10-01).
