---
name: snow-white
description: Read and act on Snow-White JUnit XML quality gate reports. Use this skill whenever a Snow-White report is present in the conversation (a JUnit XML file with testsuites/testsuite/testcase elements representing API coverage criteria), whenever a Snow-White quality gate has failed (e.g. `snow-white calculate` exited non-zero), or whenever the user asks to "fix the failing coverage criteria," "fix the quality gate," "why did Snow-White fail," or similar. Turns failing criteria into concrete, prioritized test improvements instead of requiring the user to explain the report format each time.
---

# Snow-White Quality Gate Skill

Snow-White measures **API coverage**: it correlates an OpenAPI specification against real
OpenTelemetry traces to check whether your integration tests actually exercise the paths,
methods, response codes, parameters, and content types your spec promises.
This skill teaches
Claude how to read the JUnit XML report Snow-White emits and turn a failing quality gate into
concrete, correctly-prioritized fixes.

## When this skill applies

- The user pastes or uploads a JUnit XML report produced by `snow-white calculate`.
- A CI log shows `snow-white calculate` exiting non-zero.
- The user asks to fix failing coverage/quality-gate criteria, or asks why a Snow-White gate failed.

## Report structure

Snow-White's JUnit XML follows this shape:

```text
testsuites
└── testsuite (one per API: service name + api name + api version)
    └── testcase (one per evaluated criterion, e.g. "PATH_COVERAGE: GET /pung/{message}")
```

Each `testcase` corresponds to a single quality-gate criterion evaluated for a single
operation or endpoint.
A `testcase` with a `<failure>` child means that criterion was not met.
The `testsuite`'s `tests` attribute is the total number of criteria evaluated for that API.

## Step 1 — Parse the report

Read the XML and, for every `testsuite`, extract:

- The API identity (service name / api name / api version — usually encoded in the suite name).
- Every failing `testcase`: its criterion name, the specific path/method/param/response-code it concerns, and the failure message.

## Step 2 — Handle correlation failures first

**Before treating anything as a real test gap**, check whether a `testsuite` has `tests="0"`.
This is not "zero criteria passed" — it means Snow-White found **no correlated telemetry at
all** for that API.
Writing more tests will not fix this; it means the OTEL traces and the
OpenAPI spec never linked up.

When you see `tests="0"`, stop and walk through the onboarding checklist instead of proposing
test code:

- [ ] `x-api-name` and `x-service-name` are present in the spec's `info` block
- [ ] `OTEL_SERVICE_NAME` (or `spring.application.name`) matches `x-service-name` exactly
- [ ] The OTEL Java agent is attached to the test run (or spans are manually enriched)
- [ ] `io.github.bbortt.snow-white.toolkit:spring-web-autoconfiguration` is on the classpath (Spring Boot projects)
- [ ] Endpoints are annotated with `@SnowWhiteInformation` matching the spec's identifiers and `operationId`
- [ ] The specification was actually published/indexed before the test run
- [ ] The `--lookback-window` used for `calculate` actually covers when the tests ran

Report to the user which of these looks broken based on what's visible in the conversation
(config files, pom.xml, spec, CI logs), and ask for whichever piece is missing rather than
guessing.

## Step 3 — Prioritize failing criteria using the hierarchy

Several criteria are parents of others: satisfying the parent automatically satisfies the
child, but not vice versa. **Always fix the highest-level failing parent first** — fixing a
child criterion when its parent is also failing duplicates work, since the parent fix resolves
the child automatically.
See `references/criteria-hierarchy.md` for the full hierarchy and
descriptions of every criterion.

Quick lookup — if you see a failing child criterion, check whether its parent is failing too
and fix the parent instead:

| Failing child                                                                      | Fix the parent instead           |
| ---------------------------------------------------------------------------------- | -------------------------------- |
| `PATH_COVERAGE`                                                                    | `HTTP_METHOD_COVERAGE`           |
| `POSITIVE_RESPONSE_CODE_COVERAGE` / `ERROR_RESPONSE_CODE_COVERAGE`                 | `RESPONSE_CODE_COVERAGE`         |
| `REQUIRED_PARAMETER_COVERAGE` / `OPTIONAL_PARAMETER_COVERAGE`                      | `PARAMETER_COVERAGE`             |
| `NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES` / `NO_UNDOCUMENTED_ERROR_RESPONSE_CODES` | `NO_UNDOCUMENTED_RESPONSE_CODES` |

If only a leaf criterion is failing (its parent already passes), fix the leaf directly — don't
over-engineer a broader fix that isn't needed.

## Step 4 — Propose integration tests, not mocks

Snow-White correlates real OTEL traces, so unit tests with mocked HTTP layers do **not**
improve coverage — only tests that make a real HTTP call through the instrumented application
do.
When proposing fixes:

- Write real HTTP integration tests (e.g. `MockMvc` configured for real requests, `RestAssured`,
  `WebTestClient` against a running context, or equivalent for the project's stack) — never
  mock the controller/handler layer itself.
- Match the existing test style/framework already used in the project rather than introducing a new one.
- For missing response-code coverage, target the specific documented code that's uncovered
  (e.g. a 404 or 400 case), not just another happy-path call.
- For missing parameter coverage, exercise the specific parameter (and, for optional params,
  both the with- and without-value cases where relevant).
- For "undocumented response code" failures, the fix is usually in the **spec**, not the
  tests: either document the response code that occurred, or fix the code so it stops
  returning an undocumented one.

## Step 5 — Summarize

After proposing fixes, summarize concisely:

- What was fixed (or what checklist items need the user's input, for correlation failures).
- Which specific criteria that resolves — call out parent/child criteria resolved together.
- What's still failing and why (e.g. blocked on a missing annotation, or intentionally deferred).

## Reference

- `references/criteria-hierarchy.md` — full criteria descriptions and the complete parent/child hierarchy.
- <https://timon-borter.ch/snow-white/quality-gate-criteria/> — full criteria descriptions and the complete parent/child hierarchy, online version.
