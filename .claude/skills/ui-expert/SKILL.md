---
name: ui-expert
description: Use when reviewing, auditing, or building the Snow-White web UI — the api-gateway React/TypeScript frontend under microservices/api-gateway/src/main/webapp. Covers UX findings, accessibility, visual verification of a running instance via claude-in-chrome, and the frontend's existing component/state conventions. Trigger for "review the UI", "is the frontend good", requests to add or change a page/component, or design/UX feedback.
---

# Snow-White UI Expert

The frontend is a JHipster-generated React 19 + TypeScript app (Redux Toolkit, `reactstrap`
for Bootstrap components, `recharts` for charts, `react-jhipster` for i18n/pagination helpers) in
`microservices/api-gateway/src/main/webapp`.

## Look at the real thing before proposing changes

Don't review from source alone.
Load the `claude-in-chrome` skill and actually drive the running
app — click through every nav item and dropdown, open a result detail page, and resize to a
mobile viewport (e.g. 390×844) at least once.
Cross-reference every observed behavior against its
source file before calling it a bug; a lot of "looks off" turns out to be intentional once you
read the component.

The app is served at `http://localhost/` when `docker compose -f dev/docker-compose.yaml up -d`
is running (port mapping per `docs/_pages/architecture.md`), or via the webpack dev server on
`9001` for live-reloading local development (`npm run start` / `webapp:dev` in
`microservices/api-gateway`).

## Conventions already established — extend these, don't reinvent

- **List pages** (`quality-gate.tsx`, `api-index.tsx`, `quality-gate-config.tsx`,
  `open-api-coverage-criteria.tsx`) share the `useAnimatedList` hook
  (`app/shared/use-animated-list.ts`) for staggered row enter/exit animation
  (`table-row-animation.scss`), and an `animationsEnabled` flag so the very first paint doesn't
  animate.
  They also share the URL-synced pagination/sort/filter pattern
  (`getPaginationState`/`overridePaginationStateWithQueryParams` from react-jhipster) — copy this
  pattern for new list views.
- **Status colors**: `StatusBadge` (`app/entities/quality-gate/status-badge.tsx`) is the single
  place mapping `ReportStatus` values to Bootstrap badge colors.
  Update it there, don't
  special-case colors elsewhere.
- **Loading state**: every list should show a `Spinner` (reactstrap) while `loading` is true and
  no data has arrived yet, distinct from the "confirmed empty" alert state — don't conflate the
  two, and don't leave a blank gap during the initial fetch.
- **i18n**: all user-facing text goes through `react-jhipster`'s `Translate` (JSX) or `translate`
  (imperative), with matching keys added to **both** `i18n/en/*.json` and `i18n/de/*.json`.
  Never
  hardcode a string that a user will read.
- **Charts**: `recharts`'s `Cell` component is deprecated (removed in v4, not yet released) but
  still fully functional.
  Its documented replacement (the `shape` prop) has a known upstream bug
  that breaks Legend/Tooltip colors — don't migrate away from `Cell` speculatively; it's a real
  regression risk for a cosmetic lint warning.

## Accessibility

Custom interactive widgets need real ARIA semantics, not bare `<div>`/`<li>` with only mouse
handlers — SonarCloud (`S6847`, mirroring `eslint-plugin-jsx-a11y`) flags non-interactive
elements carrying event listeners.
For an autocomplete/combobox-style widget, the correct pattern
is `role="combobox"` on the input, `role="listbox"` on the suggestion container, `role="option"`

- `aria-selected` on each item — see `app/entities/api-index/autocomplete-input.tsx`.
  Changing a
  role changes what Testing Library's `getByRole` queries match — update the component's `.spec.tsx`
  alongside the component.

## Verifying a change

```shell
cd microservices/api-gateway
npx jest --config jest.conf.cjs --maxWorkers=2 [path/to/spec]   # unit tests
npx eslint <changed files>                                       # lint (jsx-a11y, perfectionist/sort-imports, etc.)
npx webpack --config webpack/webpack.dev.cjs --env stats=minimal # compile-check, catches type errors jest/eslint miss
pnpm run e2e                                                      # black-box Playwright suite, see src/apptest/e2e
```

The Playwright suite (`src/apptest/e2e`) is the UI's black-box test layer — real rendering, every
backend call mocked at the network level.
A component change that touches an element another spec
locates should be re-verified against it, same as updating a `.spec.tsx`.
One quirk to know before
adding a locator: some components still carry a leftover `data-cy` attribute from a pre-Playwright
Cypress era instead of `data-testid` — check which one is actually on the element (`data-cy` needs
`page.locator('[data-cy="..."]')`, see `support/locators.ts`, not `getByTestId`) rather than
assuming.

This checkout's `.ts`/`.tsx` files use **CRLF** line endings, and Prettier enforces it.
Prefer the
`Edit` tool for changes; if you script a rewrite (e.g. a Python find/replace across many lines),
verify it didn't corrupt line endings with `npx eslint --fix <file>` afterward — a prior session
in this repo introduced stray `\r` characters this way.

The running instance at `http://localhost/` (Docker) is a **built image**, not a live dev
server — code changes aren't visible there until the `api-gateway` container is rebuilt and
restarted.
Say so explicitly rather than implying a code change is "live" without checking.
