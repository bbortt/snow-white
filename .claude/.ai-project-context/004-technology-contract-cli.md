# Technology Contract — CLI (Bun/TypeScript)

The stack `toolkit/cli` is built on — a standalone Node/TypeScript CLI for triggering syncs,
calculations, and gate checks.
Built via `frontend-maven-plugin` as part of the Maven reactor, but
its own runtime/tooling stack, separate from the webapp (`004-technology-contract-webapp.md`).

## 1. Runtime

- Bun — own test runner and build (`bun test`, `bun build ... --compile` for the packaged binaries
  per OS/arch: linux/macos/windows, x64/arm64).
- Node.js >= 22.14.0 (declared `engines` constraint; the packaged output also runs under Node, not
  only Bun).
- TypeScript 6.0.3.

## 2. Approved libraries

- CLI: commander (argument parsing), chalk (terminal color), cosmiconfig (config resolution).
- HTTP: undici.
- Parsing: js-yaml.
- Testing: `bun test` with coverage (`--coverage --isolate`); WireMock for HTTP stubbing in tests
  (`wiremock`, `wiremock-captain`), waited on via `wait-on` before the suite runs.
- Lint/format: ESLint (`typescript-eslint`, `eslint-plugin-import-x`, `eslint-plugin-n`,
  `eslint-plugin-regexp`), Prettier (incl. `prettier-plugin-packagejson`,
  `@prettier/plugin-xml`).

## 3. Requires justification

- Introducing a new dependency, framework, or build tool beyond the above.
- A version bump that changes behavior or compatibility (TypeScript major, Bun major, dropping the
  Node `engines` floor).

## 4. Out of scope

The architecture is stated in `docs/spec/architecture.md`.
Further libraries are the project's to
add as its specs require.
This contract stays to the stack.
