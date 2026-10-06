# Technology Contract

This repository is multi-stack — a Maven reactor holding a Java backend, a React/TypeScript
webapp, and a standalone Bun/TypeScript CLI.
Rather than one contract folding all three together
(and hiding which rules apply where), each stack has its own peer contract at this same authority
level:

- `004-technology-contract-backend.md` — Java/Spring Boot microservices, `internal/commons`,
  `toolkit/spring-web-autoconfiguration`, `toolkit/openapi-generator`, `examples/*`.
- `004-technology-contract-webapp.md` — the React/TypeScript web UI
  (`microservices/api-gateway/src/main/webapp`).
- `004-technology-contract-cli.md` — the Bun/TypeScript CLI (`toolkit/cli`).

Apply the one matching the module you're changing; a change that spans stacks is bound by all the
contracts it touches.
