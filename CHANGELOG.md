# Changelog

## [1.2.0](https://github.com/bbortt/snow-white/compare/v1.1.0...v1.2.0) (2026-06-24)


### Features

* **report-coordinator-api:** filter options on quality gate report list endpoint ([2a2ae93](https://github.com/bbortt/snow-white/commit/2a2ae935c0be2f93039e3ba03fea6736e4639aaf))


### Bug Fixes

* **api-gateway:** accessiblity on sorting buttons ([414206c](https://github.com/bbortt/snow-white/commit/414206cc5d5ca977629aa3b8649b33688926602f))
* **deps:** transition from react-router-dom to react-router v8 ([976b265](https://github.com/bbortt/snow-white/commit/976b265de76f6d8d4486ab71502aaa6c01639f2e))


### Documentation

* add links to badges ([db1f126](https://github.com/bbortt/snow-white/commit/db1f1260f201e167b67c80e4c4c9bdf455684a06))
* add some project badges ([4a6486f](https://github.com/bbortt/snow-white/commit/4a6486ffb27e952e92e07611e16f5fc45e459c86))

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
