# Changelog

## [1.3.1](https://github.com/bbortt/snow-white/compare/v1.3.0...v1.3.1) (2026-07-02)

### Bug Fixes

- migrate otel filter processor to new format ([5254524](https://github.com/bbortt/snow-white/commit/5254524a5f828a791ad8e0fff4e842de91de9364))
- **openapi-coverage-stream:** immediately shutdown stream if influxdb connection fails ([43ef925](https://github.com/bbortt/snow-white/commit/43ef925672c176bceaa05a91671eefd6168fa754))

## [1.3.0](https://github.com/bbortt/snow-white/compare/v1.2.0...v1.3.0) (2026-06-29)

### Features

- @SnowWhiteInformation is now a class level annotation ([3385e09](https://github.com/bbortt/snow-white/commit/3385e09dc0a151e0bcbc6e0768f6bdc4d2cc9f73))

### Bug Fixes

- **openapi-coverage-stream:** match operation.id attribute first ([f99fb93](https://github.com/bbortt/snow-white/commit/f99fb93945551272f79b6e1e2444343b1d29e36f))
- **prettier:** reformat codebase ([426425e](https://github.com/bbortt/snow-white/commit/426425e825dc7b98b7191b96cfd6f8b4033f45a1))

## [1.2.0](https://github.com/bbortt/snow-white/compare/v1.1.0...v1.2.0) (2026-06-26)

### Features

- **#1285:** result filtering frontend implementation ([48a7d04](https://github.com/bbortt/snow-white/commit/48a7d04184cd186d7629b9babc05fc61143d3064))
- **report-coordinator-api:** filter options on quality gate report list endpoint ([eafb09d](https://github.com/bbortt/snow-white/commit/eafb09de93b0487948d7835d7a0e297b2cca5e0c))

### Bug Fixes

- **api-gateway:** accessiblity on sorting buttons ([79fcef6](https://github.com/bbortt/snow-white/commit/79fcef61505ccae06a7332827945860de6bceda6))
- **deps:** transition from react-router-dom to react-router v8 ([8342847](https://github.com/bbortt/snow-white/commit/8342847bb565773c490d7187a763a76f877a4248))

### Documentation

- add links to badges ([7c718da](https://github.com/bbortt/snow-white/commit/7c718da064351fc34a93c8533d25fc5d80a6f9ec))
- add some project badges ([a2f345f](https://github.com/bbortt/snow-white/commit/a2f345f0f66b03e8f9ac3a32837e8be113158174))
- **examples:** add spring-boot example app ([e58a690](https://github.com/bbortt/snow-white/commit/e58a69081775dee7a36909c5cae2bff8d0b76a46))
- onboarding without openapi generator maven plugin ([6e412aa](https://github.com/bbortt/snow-white/commit/6e412aa81bc4a5d6d4a8c3919093482c3a8402d6))

## [1.1.0](https://github.com/bbortt/snow-white/compare/v1.0.0...v1.1.0) (2026-06-19)

### Features

- **api-gateway:** auto-complete feature for api index filtering ([115d7a7](https://github.com/bbortt/snow-white/commit/115d7a7f547313efee5363c840a9534f9344ffad))
- **api-gateway:** filtering for api index page ([e780941](https://github.com/bbortt/snow-white/commit/e780941c88569bdb0d8e02d4f06b86630fcd0a3a))
- **api-gateway:** pagination and sorting for api index ([14babd0](https://github.com/bbortt/snow-white/commit/14babd02c9147c9af5c8fb6469adc622b051a88c))
- **microservices:** improve resilience with retries ([401c437](https://github.com/bbortt/snow-white/commit/401c4371f8b271d0b35f5f8d262da3ab0ea9037d))
- **openapi-coverage-stream:** extract and publish otel tracing context for e2e tracing in streams ([bb90934](https://github.com/bbortt/snow-white/commit/bb90934484ef4b16a12471792107aa88ed1ae5f5))

### Bug Fixes

- **api-gateway:** sorting header row layout ([5710acf](https://github.com/bbortt/snow-white/commit/5710acfdc08ea00dbce9cdd9be4bde7bb3789134))
- **api-gateway:** spa web filter for api-index routing ([53bea41](https://github.com/bbortt/snow-white/commit/53bea411ffcc7b958790983ea3c8d076ca8546e8))
- **api-index-api:** api index table filtering with "contains" is more intuitive than "equals" ([154f214](https://github.com/bbortt/snow-white/commit/154f21485df23f40e279f073f6f20915d090ac3b))
- **api-index-api:** consistent 'starts with' filtering for autocompletion ([1a9157d](https://github.com/bbortt/snow-white/commit/1a9157dcf1bea510f06e51fb43ab4d2ebd3537bc))
- **api-index-api:** meta information api uri ([62cc41b](https://github.com/bbortt/snow-white/commit/62cc41b09f2d430b0875562290b89393d09d933a))
- **api-sync-job:** image build with custom api index client ([e0940b4](https://github.com/bbortt/snow-white/commit/e0940b4b0b359abcda9bb2027fd1b75051ee56d6))
- **openapi-coverage-stream:** image build with custom jvm ([81e4ad3](https://github.com/bbortt/snow-white/commit/81e4ad37d8d128948898d68051f261b50efdb0be))
- **report-coordinator-api:** race condition on calculation request ([bf1193f](https://github.com/bbortt/snow-white/commit/bf1193f9d6676e10473bebb2ddf9dca191f25848))
- trace mdc pattern ([95133ba](https://github.com/bbortt/snow-white/commit/95133ba238c2ca3dff8a5d7c5c97870f588b5da4))
