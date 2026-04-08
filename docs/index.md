---
title: Home
nav_order: 1
description: 'Snow-White - API Coverage Analysis & Observability Platform'
permalink: /
---

<div class="hero">
  <h1>Snow-White 🍎</h1>
  <p class="hero-tagline">API Coverage Analysis &amp; Observability</p>
  <p class="hero-description">
    Connect your OpenAPI specifications with OpenTelemetry tracing data to gain
    actionable insights into API coverage, performance, and quality - without
    changing a line of your application code.
  </p>
  <div class="hero-actions">
    <a href="installation" class="btn btn-primary fs-5">Get Started</a>
    <a href="https://github.com/bbortt/snow-white" class="btn btn-outline fs-5" target="_blank">View on GitHub</a>
  </div>
</div>

---

## Why Snow-White?

Testing APIs thoroughly is hard. You write integration tests, but do you know which endpoints they _actually_ exercise? Are your quality gates catching real regressions? Do you know how your production API is really being used?

**Snow-White answers these questions automatically** by correlating your OpenAPI specifications with [OpenTelemetry](https://opentelemetry.io) tracing data - the same telemetry your applications already emit.

---

## Key Features

<div class="feature-grid">

<div class="feature-card">
  <h3>📊 Coverage Analysis</h3>
  <p>Automatically determine which API endpoints, parameters, and responses are exercised by your tests or in production traffic.</p>
</div>

<div class="feature-card">
  <h3>🔭 OpenTelemetry Native</h3>
  <p>No proprietary SDKs or agents required. Snow-White uses industry-standard <a href="https://opentelemetry.io">OpenTelemetry</a>, so it works with any OTEL-compatible application.</p>
</div>

<div class="feature-card">
  <h3>✅ Quality Gates</h3>
  <p>Define coverage thresholds and validate them automatically in your CI/CD pipeline. Catch regressions before they reach production.</p>
</div>

<div class="feature-card">
  <h3>☸️ Kubernetes Native</h3>
  <p>Deploy with a single <a href="https://helm.sh">Helm</a> command. Built for cloud-native environments with configurable scaling modes.</p>
</div>

<div class="feature-card">
  <h3>🚀 Production Ready</h3>
  <p>Works with black-box integration tests <em>and</em> live production traffic. Get insights from test suites and real user behavior alike.</p>
</div>

<div class="feature-card">
  <h3>🛠️ Developer Toolkit</h3>
  <p>Includes a CLI, Spring Boot autoconfiguration, and an OpenAPI Generator plugin to minimize integration effort in your services.</p>
</div>

</div>

---

## How It Works

<div class="steps">

**1. Synchronize API Specs**
Import OpenAPI specifications from your central API repository (e.g. JFrog Artifactory). Snow-White keeps them in sync automatically.

**2. Ingest Telemetry**
Your applications emit OpenTelemetry traces during tests or in production. Snow-White listens - no code changes needed beyond standard OTEL instrumentation.

**3. Correlate & Analyze**
Traces are matched to API operations using shared semantic attributes: service name, API name, and version. Coverage is computed automatically.

**4. Evaluate Quality Gates**
Results are validated against your configured thresholds. Fail fast when coverage drops below your standards.

**5. Visualize or Export**
View coverage reports in the built-in UI, or integrate results into CI/CD pipelines via the [CLI](https://github.com/bbortt/snow-white/tree/main/toolkit/cli).

</div>

---

## Quick Start

Add the Helm repository and deploy Snow-White to your Kubernetes cluster in seconds:

```shell
helm repo add snow-white https://bbortt.github.io/snow-white
helm repo update

helm install my-snow-white snow-white/snow-white \
  --set snowWhite.ingress.host=snow-white.example.com
```

For full setup instructions, see the [Installation Guide](installation).

---

## Annotating Your APIs

Snow-White links telemetry to specifications using a lightweight annotation model - three fields applied consistently to both the spec and the traces.

**In your OpenAPI specification:**

```yaml
openapi: 3.1.0
info:
  title: My Service API
  version: 1.0.0
  x-api-name: my-api # annotation key
  x-service-name: my-service # annotation key
```

**In your OpenTelemetry spans** (set automatically via the toolkit):

| Attribute      | Example      |
| -------------- | ------------ |
| `service.name` | `my-service` |
| `api.name`     | `my-api`     |
| `api.version`  | `1.0.0`      |

These three values form the **linking key** between specifications and runtime telemetry. The [Spring Boot autoconfiguration](https://github.com/bbortt/snow-white/tree/main/toolkit/spring-web-autoconfiguration) and [OpenAPI Generator plugin](https://github.com/bbortt/snow-white/tree/main/toolkit/openapi-generator) set them automatically.

---

## Architecture

Snow-White follows an event-driven microservices architecture built on Apache Kafka and OpenTelemetry.

![Architecture Overview](architecture.png)

See the [Architecture page](architecture) for a full breakdown of all microservices and their interactions.

---

## License

Snow-White is licensed under the [PolyForm Small Business License 1.0.0](license).

**Free** for companies with fewer than $1M USD in annual revenue. For commercial licensing, contact [timon.borter@gmx.ch](mailto:timon.borter@gmx.ch).
