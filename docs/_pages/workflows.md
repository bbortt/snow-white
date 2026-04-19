---
title: 'Pipeline Workflows'
permalink: /workflows/
toc: true
toc_sticky: true
---

Snow-White integrates into your CI/CD pipeline to enforce API coverage automatically.
Two primary workflow patterns are supported depending on how your repositories are organized.

---

## Specification-First Workflow

The recommended workflow for teams practicing specification-first API design.
The OpenAPI specification is the source of truth and is published to the spec repository before implementation begins.

[![Specification-First Workflow]({{ "/workflow-specification-first.png" | relative_url }})]({{ "/workflow-specification-first.png" | relative_url }})

**How it works:**

1. A developer writes or updates the OpenAPI spec and opens a PR.
2. On merge, the spec is published to the central spec repository (e.g. JFrog Artifactory).
3. Snow-White's sync job picks up the new spec and indexes it.
4. The service implementation PR runs integration tests with the OTEL agent attached.
5. The CI pipeline triggers `snow-white calculate` — the CLI evaluates the quality gate and fails the build if coverage is insufficient.

---

## Mono-Repository Workflow

For teams using a mono-repository, the spec and implementation live side-by-side and are always in sync.

[![Mono-Repository Workflow]({{ "/workflow-monorepository.png" | relative_url }})]({{ "/workflow-monorepository.png" | relative_url }})

**How it works:**

1. The spec and implementation are committed together.
2. The CI pipeline builds the service, runs integration tests with OTEL instrumentation, and publishes the spec.
3. Snow-White indexes the spec and correlates it with the traces produced during the test run.
4. `snow-white calculate` evaluates the quality gate inline in the same pipeline.

---

## CLI Quick Reference

The Snow-White CLI is the integration point for both workflows.

**Minimal config file (`snow-white.json`):**

```json
{
  "url": "http://<snow-white-host>",
  "qualityGate": "basic-coverage",
  "apiInformation": [
    {
      "serviceName": "my-service",
      "apiName": "my-api",
      "apiVersion": "1.0.0"
    }
  ]
}
```

**Trigger a coverage calculation:**

```shell
node toolkit/cli/target/cli/index.js calculate --configFile snow-white.json
```

| Exit Code | Meaning                               |
| --------- | ------------------------------------- |
| `0`       | Quality gate passed                   |
| non-zero  | Quality gate failed or error occurred |

See [`toolkit/cli/README.md`](https://github.com/bbortt/snow-white/blob/main/toolkit/cli/README.md) for all exit codes and options.

---

## Setting Up the Quality Gate Step

Add the coverage check as a CI step after your integration tests.

**GitHub Actions example:**

```yaml
- name: Calculate API Coverage
  run: |
    node toolkit/cli/target/cli/index.js calculate \
      --configFile snow-white.json
  env:
    SNOW_WHITE_URL: ${{ secrets.SNOW_WHITE_URL }}
```

The step will fail the workflow if the quality gate is not met, preventing the PR from merging.
