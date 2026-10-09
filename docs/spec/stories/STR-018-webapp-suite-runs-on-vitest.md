# The webapp suite runs on vitest, so no hand-maintained list of ESM packages stands between a dependency bump and a green build

<!-- markdownlint-disable MD036 -->

**Title**
The webapp suite runs on vitest, so no hand-maintained list of ESM packages stands between a
dependency bump and a green build

**Status**: planned

**Business Value**
`api-gateway`'s jest configuration carries an allowlist of third-party packages that jest is
permitted to transpile from ESM to CommonJS.
The list has to name every ESM-only package anywhere
in the webapp's transitive dependency graph, which means it is correct only until the next
dependency bump introduces one more.
It has already gone stale twice, and each time the failure
mode is the same: `main` turns red, every branch cut from it inherits the red, and the diagnosis
costs a person an hour of reading stack traces that point at the wrong package.

vitest has no such list.
Vite's pipeline loads ESM natively, so an ESM-only dependency is not a
configuration event at all.
Removing the allowlist removes a recurring class of build break rather
than the latest instance of it.

The workspace also stops carrying two JavaScript test toolchains.
`api-sync-job` and `helm`
already run vitest `5.0.1` with `@vitest/coverage-v8`, both pinned in the same dependency group
that broke jest.
`api-gateway` is the only package still on jest, so it is the only package that
needs `ts-jest`, `jest-fixed-jsdom`, `jest-environment-jsdom`, `jest-junit`, `jest-sonar`,
`identity-obj-proxy`, and a bespoke transform script — none of which the rest of the workspace
needs at all.

**Problem / Context**
`microservices/api-gateway/jest.conf.cjs` configures jest `30.5.1` and hands every `.js`/`.mjs`
file under `node_modules` to `jest-import-meta-transform.cjs`, a 27-line script in the repository
that rewrites `import.meta` and then runs `ts.transpileModule` to produce CommonJS.
That transform
only ever runs for paths the `transformIgnorePatterns` entry does _not_ ignore, and that entry is
an inverted list: a negative lookahead naming nine packages, each written twice — once as a pnpm
store directory (`\.pnpm/react-router`) and once as a symlinked one (`react-router/`) — because a
pnpm path contains `node_modules/` twice and the pattern matching anywhere in the path is enough
to ignore the file.

react-router `8.4.0` added a dependency on `@remix-run/route-pattern`, which ships ESM only and is
loaded eagerly through react-router's own `matcher-route-pattern.js`.
`react-router` was on the
allowlist; its brand-new transitive dependency was not.
Three suites failed to load —
`header.spec.tsx`, `header-components.spec.tsx`, `error-boundary-routes.spec.tsx`, every suite
importing `react-router` — and `main` was red from the bump's merge until the allowlist was
extended by hand.
That patch is the immediate fix; it is not a fix for the next bump.

The suite itself is healthy and must stay that way through the move: 36 spec files, 319 tests, no
snapshots, and coverage thresholds of 90% on statements, branches, functions and lines that fail
the build when missed.
Two consumers read the output and both are wired in `pom.xml`, not in CI
workflow files:

- `sonar.javascript.lcov.reportPaths` reads `target/test-results/lcov.info`, produced by jest's
  default lcov coverage reporter.
- `sonar.testExecutionReportPaths` reads `target/test-results/TESTS-results-sonar.xml`, produced
  by `jest-sonar` in Sonar's Generic Test Execution format — a format vitest has no first-party
  reporter for.

Maven drives the suite through `pnpm run webapp:test`, so the module's `test` script is the seam
the migration works behind; nothing outside `api-gateway` invokes jest directly.

**Solution Approach**
Replace `jest.conf.cjs` with a `vitest.config.ts` following the shape `api-sync-job` and `helm`
already use — `reporters` of `default` and `junit`, plus `github-actions` under `CI`, `globals:
true`, `mockReset: true` — diverging only where the webapp genuinely differs: `environment:
'jsdom'`, the `coverage` block carrying the existing 90% thresholds through
`@vitest/coverage-v8`, and the `resolve.alias` entries replacing jest's `moduleNameMapper`.
Delete
`jest-import-meta-transform.cjs` and the allowlist outright; neither has a counterpart to port.

The spec bodies change mechanically.
`vi.fn` for 51 `jest.fn` call sites, `vi.mock` for 29,
`vi.spyOn` for 15, and the lifecycle helpers `clearAllMocks`, `restoreAllMocks` and
`resetModules`; the 35 uses of the `jest.MockedFn` and `jest.MockedFunction` types become vitest's
`Mock` and `MockedFunction` imports.
`globals: true` keeps `describe`/`it`/`expect` unimported, as
in the sibling packages, so no spec grows an import block it did not have.

Three call sites are not mechanical and are where the work actually sits.
`jest.isolateModules`
has no vitest equivalent and its one use is rewritten as `vi.resetModules()` followed by a dynamic
`import()`; the single `jest.doMock`/`jest.dontMock` pair maps to `vi.doMock`/`vi.doUnmock`, whose
hoisting rules differ from jest's and must be checked against the behavior the test asserts rather
than assumed equivalent.

Three pieces of jest-specific configuration need a decision rather than a translation, and each is
settled by establishing why it is there before replacing it:

- `jest-fixed-jsdom` is in use instead of plain `jest-environment-jsdom`.
  The reason is
  undocumented, so the migration establishes what it patches for this suite and whether vitest's
  `jsdom` environment needs the same, rather than dropping it and finding out from a flake.
- `identity-obj-proxy` maps CSS module class names to their own keys.
  Vite resolves CSS itself, so
  this becomes `css.modules.classNameStrategy: 'non-scoped'` rather than an alias.
- The `globals` block injects `I18N_HASH`, `DEVELOPMENT`, and the contents of
  `webpack/environment.cjs` as compile-time constants; these move to vitest's `define`, which is
  the same mechanism webpack already uses for the production build.

For Sonar, coverage is the easy half: `@vitest/coverage-v8` with an `lcov` reporter writes
`lcov.info` and the existing pom property needs no change.
Test execution is the half with a real
choice, because `jest-sonar` has no first-party vitest counterpart.
The story resolves it by
emitting the JUnit XML vitest produces natively and pointing `sonar.testExecutionReportPaths` at
it via Sonar's `sonar.junit.reportPaths`-style ingestion, adding `vitest-sonar-reporter` only if
Sonar rejects the JUnit form — a third-party reporter is a dependency this story exists to reduce,
so it is the fallback and not the opening move.

Finally, `jest`, `ts-jest`, `jest-environment-jsdom`, `jest-fixed-jsdom`, `jest-junit`,
`jest-sonar`, `identity-obj-proxy` and `@types/jest` leave `package.json`, and the `jest` and
`jest:update` scripts go with them.
`api-gateway` ends up depending on the same two test packages
the rest of the workspace already does.

**Acceptance Criteria**

- `pnpm run webapp:test` in `api-gateway` runs vitest, and `mvn verify` on the module still runs
  the suite through that script and still fails the build on a failing test.
- The suite reports 36 files and 319 tests passing, with none skipped, marked todo, or deleted —
  the same counts jest reports today.
- Coverage below 90% on any of statements, branches, functions or lines fails the run, as it does
  today.
- `target/test-results/lcov.info` is written, and the path
  `sonar.testExecutionReportPaths` names resolves to a report Sonar accepts; the Sonar analysis
  reports a test count for `api-gateway` rather than none.
- `jest.conf.cjs` and `jest-import-meta-transform.cjs` are deleted, and no `jest*` or
  `identity-obj-proxy` dependency remains in `microservices/api-gateway/package.json`.
- The new configuration names no third-party package for transform purposes — there is no
  allowlist, denylist, or `node_modules` pattern to extend when a dependency starts shipping ESM.
- No `.spec.ts`/`.spec.tsx` file references the `jest` global or a `jest.*` type.

**Out of scope**

- The Playwright end-to-end suite (`playwright.config.ts`, `pnpm run e2e`).
  It is a separate
  runner on a separate seam and is untouched by how unit tests execute.
- `toolkit/cli`, which runs `bun test`.
  Consolidating that too is a defensible follow-up, but it
  shares neither the ESM problem nor the Sonar wiring that motivate this story.
- Any change to what a test asserts, and any new test.
  This is a runner change at constant
  behavior and constant coverage; a test that only passes after its assertions are edited is a
  migration bug, not a passing criterion.
- The allowlist patch that unblocks `main` today.
  It ships first and independently, precisely so
  this story is not on the critical path of a red build.
- Any change to the Java test stack or to how Sonar ingests it.

## Relations

**Related**

- [STR-015](STR-015-api-gateway-ingress-routing-security-and-spa-fallback.md) — the story that
  specced this module; it establishes that the React/TypeScript webapp sits outside clew's
  anchorable surface, which is why this story realizes no spec
