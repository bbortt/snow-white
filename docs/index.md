---
layout: splash
permalink: /
title: 'Snow-White'
header:
  overlay_color: '#400101'
  overlay_filter: 0.6
  actions:
    - label: '<i class="fas fa-rocket"></i>&nbsp; Get Started'
      url: '/deployment/'
    - label: '<i class="fab fa-github"></i>&nbsp; View on GitHub'
      url: 'https://github.com/bbortt/snow-white'
excerpt: >
  Connect your OpenAPI specifications with OpenTelemetry traces to gain
  actionable insights into API coverage, performance, and quality —
  without changing a single line of your application code.

intro:
  - excerpt: >
      **Snow-White answers the question every engineering team asks:**
      *"Which API endpoints are our tests actually exercising?"*
      It correlates the OpenAPI specs you already have with the OpenTelemetry
      traces your applications already emit — no proprietary agents, no code changes.

feature_row:
  - image_path: /assets/images/screenshots/coverage-dashboard.png
    alt: 'API Coverage Dashboard'
    title: 'Coverage at a Glance'
    excerpt: >
      See exactly which endpoints, parameters, and responses are exercised
      by your test suite or production traffic — endpoint by endpoint.
    url: '/onboarding/'
    btn_label: 'Start Measuring'
    btn_class: 'btn--primary'
  - image_path: /assets/images/screenshots/quality-gate.png
    alt: 'Quality Gate Results'
    title: 'Automated Quality Gates'
    excerpt: >
      Define coverage thresholds and validate them in CI/CD.
      The CLI exits non-zero when coverage drops — catch regressions before they ship.
    url: '/workflows/'
    btn_label: 'See Pipeline Workflows'
    btn_class: 'btn--primary'
  - image_path: /assets/images/screenshots/architecture-overview.png
    alt: 'Architecture Overview'
    title: 'Cloud-Native by Design'
    excerpt: >
      Deploy with a single Helm command. Event-driven microservices on Kafka,
      built for Kubernetes with configurable scaling.
    url: '/architecture/'
    btn_label: 'Explore Architecture'
    btn_class: 'btn--primary'

feature_row2:
  - image_path: /assets/images/screenshots/otel-integration.png
    alt: 'OpenTelemetry Integration'
    title: 'OpenTelemetry Native'
    excerpt: >
      No proprietary SDKs. Snow-White piggybacks on the OTEL traces your
      services already emit. The Spring Boot autoconfiguration and OpenAPI
      Generator plugin wire the three linking attributes automatically.

        ```yaml
      openapi: 3.1.0
      info:
        title: My Service API
        version: 1.0.0
        x-api-name: my-api
        x-service-name: my-service
        ```
    url: '/onboarding/#step-2---instrument-your-application'
    btn_label: 'Instrument Your Service'
    btn_class: 'btn--primary'
---

{% include feature_row id="intro" type="center" %}

{% include feature_row %}

---

## How It Works

<div class="feature__wrapper" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:1rem;margin:2rem 0;">

<div style="text-align:center;padding:1.5rem;background:#fff;border-radius:6px;border:1px solid #e0e0e0;">
  <div style="font-size:2rem;margin-bottom:.5rem;">📥</div>
  <strong>1.
Sync Specs</strong>
  <p style="font-size:.9rem;color:#555;margin:.5rem 0 0;">Import OpenAPI specs from Artifactory.
Snow-White keeps them in sync automatically.</p>
</div>

<div style="text-align:center;padding:1.5rem;background:#fff;border-radius:6px;border:1px solid #e0e0e0;">
  <div style="font-size:2rem;margin-bottom:.5rem;">📡</div>
  <strong>2.
Ingest Traces</strong>
  <p style="font-size:.9rem;color:#555;margin:.5rem 0 0;">Applications emit OTEL traces during tests or in production.
No code changes needed.</p>
</div>

<div style="text-align:center;padding:1.5rem;background:#fff;border-radius:6px;border:1px solid #e0e0e0;">
  <div style="font-size:2rem;margin-bottom:.5rem;">🔗</div>
  <strong>3.
Correlate</strong>
  <p style="font-size:.9rem;color:#555;margin:.5rem 0 0;">Traces match specs via service name, API name, and version.
Coverage computed automatically.</p>
</div>

<div style="text-align:center;padding:1.5rem;background:#fff;border-radius:6px;border:1px solid #e0e0e0;">
  <div style="font-size:2rem;margin-bottom:.5rem;">🚦</div>
  <strong>4.
Gate</strong>
  <p style="font-size:.9rem;color:#555;margin:.5rem 0 0;">Evaluate results against quality thresholds.
Fail fast when coverage regresses.</p>
</div>

<div style="text-align:center;padding:1.5rem;background:#fff;border-radius:6px;border:1px solid #e0e0e0;">
  <div style="font-size:2rem;margin-bottom:.5rem;">📊</div>
  <strong>5.
Visualize</strong>
  <p style="font-size:.9rem;color:#555;margin:.5rem 0 0;">Review reports in the UI or export results to CI/CD via the CLI.</p>
</div>

</div>

{% include feature_row id="feature_row2" type="left" %}

---

## Quick Start

```shell
helm repo add snow-white https://bbortt.github.io/snow-white
helm repo update

helm install my-snow-white snow-white/snow-white \
  --set snowWhite.host=snow-white.example.com
```

For full configuration options see the [Deployment Guide](/deployment/).
