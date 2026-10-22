# Technology Contract — Webapp (React/TypeScript)

The stack the web UI is built on: `microservices/api-gateway/src/main/webapp`.
Built via
`frontend-maven-plugin` as part of the `api-gateway` Maven module, but is its own runtime/tooling
stack — the backend side of `api-gateway` follows `004-technology-contract-backend.md`.

## 1. Runtime

- Node.js v24.15.0, pinned via `node.version` in the root `pom.xml` (downloaded by
  `frontend-maven-plugin` for reproducible builds — not assumed present on the host).
- TypeScript 6.0.3.
- npm (package manager for this module; the CLI module uses Bun instead — see
  `004-technology-contract-cli.md`).
- Webpack 5 (`webpack/webpack.dev.cjs`, `webpack/webpack.prod.cjs`) for bundling.

## 2. Approved libraries

- Framework: React 19, react-jhipster for i18n (`Translate` / `translate` — never hardcode
  user-facing text; matching keys required in `i18n/en/*.json` and `i18n/de/*.json`).
- State: Redux Toolkit, react-redux.
- Routing/UI: react-router, react-bootstrap / reactstrap / bootstrap, FontAwesome, recharts.
- Forms: react-hook-form.
- Testing: Jest + ts-jest + Testing Library for unit tests (`npx jest --config jest.conf.cjs`);
  Playwright for end-to-end tests.
- Lint/format: ESLint, Prettier.

## 3. Requires justification

- Introducing a new dependency, framework, or build tool beyond the above.
- A version bump that changes behavior or compatibility (React major, TypeScript major, Webpack
  major).

## 4. Out of scope

The architecture is stated in `docs/spec/architecture.md`.
Further libraries and build-tooling
changes are the project's to add as its specs require.
This contract stays to the stack.
