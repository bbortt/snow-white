# Changelog

## [2.0.0](https://github.com/bbortt/snow-white/compare/v1.13.0...v2.0.0) (2026-10-10)

### ⚠ BREAKING CHANGES

- **commons:** `getContainedBy()` is joined by `getContainmentForm()`, and a declared container now always carries a form. A consumer that read containment as "is a subset of" must read the form before drawing a coverage implication from an edge.
- **report-coordinator-api:** of the four predefined gates only basic-coverage changes -- six criteria at 80% tolerated exactly one failure and now tolerates none. full-feature at 100% and minimal at one criterion already demanded every criterion they select, and dry-run selects none. An API test that passed with one included criterion below the bar now fails, and the reports that roll up from it (SW-015) fail with it. No gate configuration changes and no stored report is rewritten; the new rule applies to API tests scored from here on.
- **report-coordinator-api:** a custom quality gate whose bar is 82, 85, 86, 88, 89, 91, 92 or 93 may now fail an API test it previously passed, when the passing share lands exactly on the rounding boundary. The gate's configuration does not change and no report is rewritten; only newly scored API tests are affected.
- **quality-gate-api:** `basic-coverage` goes from five criteria to six, and `initPredefinedQualityGates` upserts predefined gates by name on every startup, so an upgraded instance gains the criterion without anyone asking for it. A pipeline that was green on exactly four of five criteria and has paths it never reaches now scores four of six, which is 67% against an 80% bar, and fails. `CON-003` forbids users mutating a predefined gate so that a pinned name keeps meaning one thing; changing one across a release owes them this warning in return.
- **api:** the report, quality-gate and API-index list reads answer `400` for a `sort` value they cannot honour, where they previously answered `500` or ignored it. `sort=createdAt,…` on the report read and `sort=otelServiceName,…` on the API-index read are no longer accepted; use `initiatedAt` and `serviceName`. Reports can no longer be sorted by `status`, which is published as a name but stored as a stable code whose order those names do not imply, so the column lost its sort affordance in the UI.

### Features

- **api-gateway:** count the traces that contributed to the coverage ([29e4be5](https://github.com/bbortt/snow-white/commit/29e4be5c92cbc718566b76d2a991320042ca87c6))
- **citrus-junit-jupiter-extension:** an extension for the test case otel semconv ([f72c955](https://github.com/bbortt/snow-white/commit/f72c9551b4fd14bb298381dd30c40ac12c71fddb))
- **commons:** containment admits a stricter check, not only a smaller target set ([70f391f](https://github.com/bbortt/snow-white/commit/70f391f9dbf6174df571fc9b1deaa9e98c4f800a))
- **commons:** declare criteria containment on the criteria enum ([cf98d91](https://github.com/bbortt/snow-white/commit/cf98d91a7f3b6678bb14a2c3dacc4ef8f97a0149))
- **helm:** kubernetes restarts a coverage stream whose topology died ([d43e0d9](https://github.com/bbortt/snow-white/commit/d43e0d9cbb5c74139fdaf02d16384d3fcaa39d40))
- **openapi-coverage-stream:** the stream reports its own health ([85b1f64](https://github.com/bbortt/snow-white/commit/85b1f648ec1995c7ac685be93448004d05050f0e))
- **quality-gate-api:** basic-coverage measures path coverage again ([682690e](https://github.com/bbortt/snow-white/commit/682690e18f6e5ed6c3dc33a03350af9223db3e34))
- **report-coordinator-api:** publish the threshold a report was scored against ([8813e97](https://github.com/bbortt/snow-white/commit/8813e97cb147c6889654a2ce90830029b62da1bf))

### Bug Fixes

- **api-gateway:** mark the coverage bars at the report's pinned threshold ([54b8e5f](https://github.com/bbortt/snow-white/commit/54b8e5f7f178debb785d014501bd770c4e705ec2))
- **api-gateway:** swallow only a missing gate, not every fault ([6878ae2](https://github.com/bbortt/snow-white/commit/6878ae2d5524d70cb61cf8a05a3954e252c45ddd))
- **api:** honour the documented sort contract on the three list reads ([b446a0a](https://github.com/bbortt/snow-white/commit/b446a0aaa02a9108934aef19364c9bd5cf02c301))
- **cli:** fail closed when a report arrives without its threshold ([efbe848](https://github.com/bbortt/snow-white/commit/efbe8482ad67ff6cf48c3bd4e9ed5c1505f6a0bb))
- **cli:** follow the generated error DTO rename ([0b76430](https://github.com/bbortt/snow-white/commit/0b76430224cd99532089dfb3b86b40ca4a9fd28b))
- **cli:** keep the fail-closed threshold check free of an inverted comparison ([cb16daa](https://github.com/bbortt/snow-white/commit/cb16daa43b411a199d47aa0e186deea3d9f727fc))
- **cli:** report only the criteria below the report's pinned bar ([278e38f](https://github.com/bbortt/snow-white/commit/278e38fc7146852695e860f47760eb4c75994721))
- **cli:** treat a null threshold as no bar at all ([1362809](https://github.com/bbortt/snow-white/commit/13628093faddd0d49e560c36eed99dfc73b625f6))
- **commons:** path coverage is not contained by HTTP method coverage ([0fb5b9a](https://github.com/bbortt/snow-white/commit/0fb5b9a1ad641d903df87f62fb252ebb84b11605))
- **report-coordinator-api:** compare the passing share exactly ([90beb74](https://github.com/bbortt/snow-white/commit/90beb74a4985ee07b9ca35f85aba76b4297e73e3))
- **report-coordinator-api:** every included criterion must clear the bar ([41c5776](https://github.com/bbortt/snow-white/commit/41c57763a3b9769decf96e71d22705457f36c5a0))
- **report-coordinator-api:** follow the generated error DTO rename ([91f1df9](https://github.com/bbortt/snow-white/commit/91f1df9f7d1356e793f7072d54bb4b49b6b4e8d1))

### Performance Improvements

- **report-coordinator-api:** batch lazy and eager collection loads ([9732406](https://github.com/bbortt/snow-white/commit/9732406314c6732a27db6ab4ede67fa852b57205))
- **report-coordinator-api:** index api test lookups ([ded4ba8](https://github.com/bbortt/snow-white/commit/ded4ba831bf22cb6871c1749351bc7571f59b1c1))

### Documentation

- **pages:** wrap up release 1.13.0 ([e73345e](https://github.com/bbortt/snow-white/commit/e73345e435ea92dfcdb8ec9786961aad81a5b70f))
- **STR-021:** promote the sort contract specs and anchor them ([6423823](https://github.com/bbortt/snow-white/commit/642382364bc988c5c4fee1e1d29dc89c255f8e3e))
- **STR-022:** describe the agentic failure set against the pinned bar ([3e24a05](https://github.com/bbortt/snow-white/commit/3e24a053cb595faf4ec0ec8bc2b8231b00c82654))
- **STR-022:** promote the pinned-threshold spec and anchor its relations ([098208c](https://github.com/bbortt/snow-white/commit/098208c61f13b39f774fe3d33ab6d3225bb50982))
- **STR-023:** promote the waiver specs and anchor their relations ([9350465](https://github.com/bbortt/snow-white/commit/9350465da54ab442999a24eea99a26548f9b00cd)), closes [#2009](https://github.com/bbortt/snow-white/issues/2009)

## [1.13.0](https://github.com/bbortt/snow-white/compare/v1.12.0...v1.13.0) (2026-10-01)

### Features

- **#1642:** drilldown UI ([3ae3e5e](https://github.com/bbortt/snow-white/commit/3ae3e5ec40126d241fa896c3425d3159a57b5c14))
- **api-gateway:** download the report read as a file ([c629eb0](https://github.com/bbortt/snow-white/commit/c629eb042a4182f0cc02e6677103ecb176ee79f5))
- **api-sync-job:** count every cycle outcome under the status it reached ([ec01eae](https://github.com/bbortt/snow-white/commit/ec01eae99de2b48f2820b9ffa64b7317e5d5ece8))
- **api-sync-job:** specify the cadence, identity and resilience contract ([d856608](https://github.com/bbortt/snow-white/commit/d856608c3742ee4b023e99809e8684ce1240fc9d))
- **cli:** persist the full JSON report alongside the JUnit one ([3658b04](https://github.com/bbortt/snow-white/commit/3658b042f8cec880a849d2e66a1797b344ec1dbf))
- **STR-020:** rest responses omit null properties ([c837137](https://github.com/bbortt/snow-white/commit/c837137a2c4d352a4dfbbd77500523282ac3aa54))

### Bug Fixes

- **api-gateway:** keep the fallback label the extraction dropped ([169b761](https://github.com/bbortt/snow-white/commit/169b761981d12453ab4f8cad31665f50970a8353))
- **api-gateway:** name the download buttons where their label is hidden ([50348a0](https://github.com/bbortt/snow-white/commit/50348a0e588f15d5b50a30ecc1f009a50fbef0ce))
- **api-sync-job:** name every reason a candidate file could not be indexed ([7fb59d2](https://github.com/bbortt/snow-white/commit/7fb59d21cd32bec11d157fbc502e1999eebd76b7))
- **cli:** keep both output flags silent in agentic mode ([1be841f](https://github.com/bbortt/snow-white/commit/1be841f34d73c2a0f79a79773fab3d431df69b2f))
- **cli:** settle the rejection assertions and split the artifact dispatch ([b3cc127](https://github.com/bbortt/snow-white/commit/b3cc127978a4bcda4eced07a143ae8b66ba560bc))
- **commons:** name the property when a positive bound binds as null ([949ab57](https://github.com/bbortt/snow-white/commit/949ab571d832677b0cf0737ae1c3a81a7fcd55a8))
- **helm:** render a zero fan-out bound instead of dropping it ([8194332](https://github.com/bbortt/snow-white/commit/819433240c1708c15785f47070fbc8d33bdc8e4d))

### Documentation

- **adr:** accept ADR-0003 ([613a12d](https://github.com/bbortt/snow-white/commit/613a12dc45e7fc6a1e580c899fc3f3a451579ff6))
- **adr:** decide how evidence leaves the system ([d5774f8](https://github.com/bbortt/snow-white/commit/d5774f81a616ddad649673b1f454bfdcab93fdeb))
- release 1.12.0 post ([f50bde2](https://github.com/bbortt/snow-white/commit/f50bde2d35e6bd718a836bf507ab644aceb2a87f))

## [1.12.0](https://github.com/bbortt/snow-white/compare/v1.11.1...v1.12.0) (2026-09-29)

### Features

- derive path coverage from findings ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([6ddca67](https://github.com/bbortt/snow-white/commit/6ddca67dc960debb3f85e797656cd319d7830eec))
- **example-snow-white-openapi-generator:** name the test behind a span with baggage ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([d3f1da1](https://github.com/bbortt/snow-white/commit/d3f1da18ad31a12d567c3ae6ed70468cf0bec6e0))
- **example-spring-boot:** name the test behind a span with baggage ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([64ac9b4](https://github.com/bbortt/snow-white/commit/64ac9b46f97f5cf9e7f49dafb0833eed1d4afbff))
- **openapi-coverage-stream:** name the test behind a finding ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([191a1b7](https://github.com/bbortt/snow-white/commit/191a1b7ba49e221d0a0a9f16b94409d2d7bcd11c))
- **report-coordinator-api:** persist findings beside the ratio they imply ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([299ef94](https://github.com/bbortt/snow-white/commit/299ef94e85cdd83a9ec3606156aff0a7b9b86cca))
- **report-coordinator-api:** serve a criterion's findings with the report ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([a6048d3](https://github.com/bbortt/snow-white/commit/a6048d37f557d41df70d7ff39181c0512ec3d9d8))

### Bug Fixes

- **api-index-api:** drop redundant unique index on api_reference's key columns ([35f3197](https://github.com/bbortt/snow-white/commit/35f31973622aeafe3a5a761069503cce810d41cd))
- **api-index-api:** narrow api_version column to match its actual constraints ([4364fcc](https://github.com/bbortt/snow-white/commit/4364fccc0fbc17e0479e1e70cd0ee838126b580d))
- **build:** keep the scoped PIT gate from aborting when nothing changed ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([a55e5e0](https://github.com/bbortt/snow-white/commit/a55e5e02967db3bcb99b29f2d35772541a9ad097))
- **build:** run WireMock on the multi-arch image ([a3eccb3](https://github.com/bbortt/snow-white/commit/a3eccb376616669de8846eda23fe7d391b5c03ea))
- **ci:** run zizmor on a runner that ships docker ([53a400d](https://github.com/bbortt/snow-white/commit/53a400d590f6bef6cc171700b634ebfb99499e73))
- **example-spring-boot:** compile parameter names so the example answers its own endpoints ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([e48b760](https://github.com/bbortt/snow-white/commit/e48b760086159312538c29e521f5561bd60cd48c))
- **example-spring-boot:** construct a PingRequest from the request body ([4239363](https://github.com/bbortt/snow-white/commit/4239363a302a92f8c052e44a5c7a299e440358cb))
- **examples:** serve the API docs under Spring Boot 4 ([172f8c9](https://github.com/bbortt/snow-white/commit/172f8c9514ea4e98bfe34e3d62b89d505a16c60f))
- **openapi-coverage-stream:** avoid re-deriving telemetry in ContentTypeCoverageCalculator ([7f076a2](https://github.com/bbortt/snow-white/commit/7f076a25d6863558e5510aac5ef1c2304f53061d))
- **openapi-coverage-stream:** bound a test name where it is read, not where it is stored ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([e5f5c57](https://github.com/bbortt/snow-white/commit/e5f5c57d9099b3e308d7ae5803688764d8743d9d))
- **openapi-coverage-stream:** delegate error-code checks consistently to HttpStatusCodeUtils ([725bb84](https://github.com/bbortt/snow-white/commit/725bb84c6621069dc9dfedecca2647f9be045a09))
- **openapi-coverage-stream:** quote literal path segments before compiling operation-key pattern ([122ab34](https://github.com/bbortt/snow-white/commit/122ab345f743320f2eb4b3d94f036ff87778b8eb))
- **openapi-coverage-stream:** report an unstorable test identity once per calculation ([b763552](https://github.com/bbortt/snow-white/commit/b7635520df8431fc8cc85bae98273d57d02a9cc7))
- **openapi-coverage-stream:** resolve required-error-fields telemetry by template ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([3ac6201](https://github.com/bbortt/snow-white/commit/3ac62019bb46118beb1bb97e5eaa492b8fdfb64c))
- **openapi-coverage-stream:** resolve telemetry once per operation, not per target ([581e9ec](https://github.com/bbortt/snow-white/commit/581e9ec6beed67f2adc56c20cd0e67a2ec5e4b9b))
- **quality-gate-api,report-coordinator-api:** enforce min coverage percentage bounds everywhere ([7c708dd](https://github.com/bbortt/snow-white/commit/7c708dd1b59e0ba7183c921b391e782e8a942022))
- **quality-gate-api:** drop redundant single-column index on quality_gate_configuration.name ([0012f46](https://github.com/bbortt/snow-white/commit/0012f46ee089a19e3c144d872c425a7feaa541e2))
- **report-coordinator-api:** declare api_test_criteria's column length explicitly ([3440d09](https://github.com/bbortt/snow-white/commit/3440d0925b4d0abcaf5589f31203d5169872f397))
- **report-coordinator-api:** dedupe finding evidence at the database level ([f4e11ab](https://github.com/bbortt/snow-white/commit/f4e11ab848bbc2681aa45eb78c0318ed8070c06c))
- **report-coordinator-api:** hold every test a trace named against one target ([d4f3efd](https://github.com/bbortt/snow-white/commit/d4f3efdfdc60bd7a287ae1befe19e4faa922d622))
- **report-coordinator-api:** name the criteria column the finding points at ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([8e773c9](https://github.com/bbortt/snow-white/commit/8e773c9282df37a439b62fae71afc19432cf8904))
- **report-coordinator-api:** read the finding status column as a Number ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([18151a9](https://github.com/bbortt/snow-white/commit/18151a9b904d82401de684b594b100424947d93b))
- **report-coordinator-api:** settle the report status on the body that is served ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([95745be](https://github.com/bbortt/snow-white/commit/95745be9708175cc48c87d5aab7dbe045877dce7))
- **report-coordinator-api:** supersede the findings migration instead of editing it ([b82e764](https://github.com/bbortt/snow-white/commit/b82e764e089dfc7eba5b3bed26b2487ee97ff2ce))
- resolve open SonarCloud findings on PR [#2106](https://github.com/bbortt/snow-white/issues/2106) ([dd7cb26](https://github.com/bbortt/snow-white/commit/dd7cb26f69c43e3a224ecb7feb30216e53cdef13))
- transform @remix-run/route-pattern for jest ([edb4a29](https://github.com/bbortt/snow-white/commit/edb4a291a52a28cc0a6af3d45befb96922809e65))

### Documentation

- correct the line-ending note to LF ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([209f1dc](https://github.com/bbortt/snow-white/commit/209f1dc9201a587fbc453308aacd11d3e334bb42))
- document the application-test sequence that actually works ([1c8f12f](https://github.com/bbortt/snow-white/commit/1c8f12f4b5494f6440731088ab61c78cb912b78f))
- document the baggage copy in the onboarding guide ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([70da778](https://github.com/bbortt/snow-white/commit/70da778d2a1ad623bd908c7291b83570e4aafe54))
- **example-spring-boot:** document how to actually run the application tests ([76e1f7a](https://github.com/bbortt/snow-white/commit/76e1f7aa2ded1ea58b935b6e4e6f3b45fa02132a))
- point SW-032 at the contract the bound actually lives on ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([d05378c](https://github.com/bbortt/snow-white/commit/d05378cb4902988070fc39ea19c22aca576c62bb))
- pull test identity into the findings model ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([33cb806](https://github.com/bbortt/snow-white/commit/33cb806d77d2c543b0e32a07a040c16f601427ac))
- spec api test findings as the primitive behind coverage ([#1642](https://github.com/bbortt/snow-white/issues/1642)) ([6fab330](https://github.com/bbortt/snow-white/commit/6fab3302d8366a2c653de3c4db875f629ce6ae90))
- spec the webapp's migration from jest to vitest ([4633d67](https://github.com/bbortt/snow-white/commit/4633d67bcf9e8e978f8a8f78c5042d046566b8fa))
- **spec:** strip the stray closing tag from two api-index-api specs and a story ([572a9f6](https://github.com/bbortt/snow-white/commit/572a9f667a31feb336ff72fcb0658debc926da6f))
- stop crediting the mutation label with a property both labels have ([53112a2](https://github.com/bbortt/snow-white/commit/53112a2c973ab7e902e054aab49601f732b86270))

## [1.11.1](https://github.com/bbortt/snow-white/compare/v1.11.0...v1.11.1) (2026-09-22)

### Bug Fixes

- release workflow without jacoco coverage ([dd88957](https://github.com/bbortt/snow-white/commit/dd889573178d5ea1a1f5893dccce050fb36da5a0))

## [1.11.0](https://github.com/bbortt/snow-white/compare/v1.10.1...v1.11.0) (2026-09-21)

### Features

- **#2010:** cleanup of parent-child criteria relationships ([8861a6c](https://github.com/bbortt/snow-white/commit/8861a6cc1e67cbb5b817e932776c001da2dffeeb))
- **cli:** cross-compile cli for windows arm targets ([db33a95](https://github.com/bbortt/snow-white/commit/db33a957a55ad31dfe5a246dab6d522bdb4f3fde))
- **helm:** customizable resources for all microservices ([a73927d](https://github.com/bbortt/snow-white/commit/a73927dfcce782e9fc4163b6199856845407754c))
- **openapi-coverage-stream:** bound telemetry fetch by required keys and spans ([79f2eeb](https://github.com/bbortt/snow-white/commit/79f2eebcf650cce2672009483fafc135aea69c60))
- **report-coordinator-api:** judge the JUnit export by the gate's own threshold ([74dd8e4](https://github.com/bbortt/snow-white/commit/74dd8e417a313d25471512d19d8f7e83dbc12d8e))

### Bug Fixes

- **api-gateway:** now requires api-index-api-url ([0ac9013](https://github.com/bbortt/snow-white/commit/0ac90130baf7cdfad68132a5e74734433bdb2961))
- **quality-gate-api:** reseed removes stale predefined-gate criteria ([b87f7a4](https://github.com/bbortt/snow-white/commit/b87f7a49fb6ef76913dbfaad1eab1c43380bd972))
- **report-coordinator-api:** redelivery of messages is an upsert operation ([305906a](https://github.com/bbortt/snow-white/commit/305906a07eebf71b914986a59e0ecbf8839f0e64))

### Documentation

- add premise-check skill to verify a request's factual premises ([873c5b3](https://github.com/bbortt/snow-white/commit/873c5b36a27e063c4de96e7496b83a781ce44406))
- draft improvements for the openapi-coverage-stream under load ([4ac6795](https://github.com/bbortt/snow-white/commit/4ac67959d8d1a0b8c7f8f2aaaaa97278ec434373))
- point agents at scoped local mutation testing ([9f4576b](https://github.com/bbortt/snow-white/commit/9f4576bc83945b8e49313143d1edd1efa10e64d4))
- **release:** add news for release version 1.11.0 ([6930a1e](https://github.com/bbortt/snow-white/commit/6930a1ef424e1709522d4b0b3e14a09e40109492))

## [1.10.1](https://github.com/bbortt/snow-white/compare/v1.10.0...v1.10.1) (2026-09-16)

### Bug Fixes

- **#2052:** bump bun to v1.4.2 which resolved cross compilation issues ([e1b2def](https://github.com/bbortt/snow-white/commit/e1b2def5eddbf933a0014e79578566506a86fafc))
- **helm:** remove otel collector influxdb exporter when disabled ([1b2e25b](https://github.com/bbortt/snow-white/commit/1b2e25b3dd4a024fb3d70f3210589f23a2bba92b))
- **openapi-coverage-stream:** grafana tempo scalar types ([401ecdb](https://github.com/bbortt/snow-white/commit/401ecdb63c85b809fa248b2fb483ad4e1697f06e))

### Documentation

- **claude:** add microservice architecture rules ([0a20c0b](https://github.com/bbortt/snow-white/commit/0a20c0b09c0e6c55b7a4f22ec9a00c16a292a3e7))
- **clew:** introduce base layer of stories and specifications ([ec7c332](https://github.com/bbortt/snow-white/commit/ec7c33297e5d405144ca5f93e7da2387cc8c5d88))
- **clew:** retrace requirements for the openapi-coverage-stream service ([a5ff628](https://github.com/bbortt/snow-white/commit/a5ff628487b1f1d446c68e36f08344e543d6aa17))
- move pages into own directory ([956afc1](https://github.com/bbortt/snow-white/commit/956afc1c5a38222c662713dc6a884225a3039200))
- remove aboslute links in documentation ([f629835](https://github.com/bbortt/snow-white/commit/f629835e06841e9a3817f6fc293067c5068b9808))
- updated navigation ([02ce448](https://github.com/bbortt/snow-white/commit/02ce448a8be117c7bfdf87f90cf45e48a7cf84ab))
- wrong casing of open-telemetry ([9167d17](https://github.com/bbortt/snow-white/commit/9167d17467e0c65f3f8482a8dc944b3ee70fe875))

## [1.10.0](https://github.com/bbortt/snow-white/compare/v1.9.0...v1.10.0) (2026-09-12)

### Features

- **#2012:** add agentic mode flag to cli ([72bbf94](https://github.com/bbortt/snow-white/commit/72bbf947ae618b8bf88e950b6f4805259a16c5e7))
- **skill:** publish snow-white skill as an APM package ([58b0104](https://github.com/bbortt/snow-white/commit/58b01040c78da2ae00f4ad01cb7963b294294f51))

### Bug Fixes

- **openapi-coverage-stream:** make both influxdb and grafana tempo requests more resilient ([a0769f3](https://github.com/bbortt/snow-white/commit/a0769f3e118a8e6ffb5dd17ce4a6723d3e2b4885))
- **openapi-coverage-stream:** migrate to grafana tempo traces v2 api ([fcb7818](https://github.com/bbortt/snow-white/commit/fcb7818a9cb78f260c962ddc465b28a2cbaf2807))
- **report-coordinator-api:** inconsistent entity validation ([decb109](https://github.com/bbortt/snow-white/commit/decb1093992549a4757ce5e2d5f33863c60de820))

### Documentation

- **#2012:** denote agentic mode in the snow-white skill ([b890b4a](https://github.com/bbortt/snow-white/commit/b890b4adde676ee10a5a4d3f4cfb16b4b25c59de))
- **#2012:** make agentic mode more prominent across repository ([61b1b74](https://github.com/bbortt/snow-white/commit/61b1b7484cb0fcb7d20ab467b47fd70ef668d1f1))
- add blog post and mention new agentic cli mode ([cd15b25](https://github.com/bbortt/snow-white/commit/cd15b255f5d51f41f895e99a636807e0dfebc280))
- add note that additional http header capturing configuration might be required ([dc0f0e4](https://github.com/bbortt/snow-white/commit/dc0f0e4ca3cc03b40e89306eb61e8b5cfea686c8))

## [1.9.0](https://github.com/bbortt/snow-white/compare/v1.8.0...v1.9.0) (2026-09-05)

### Features

- **#1697:** add otelCollector.disableIngestion to opt out of bundled OTel ingestion stack ([80c78ff](https://github.com/bbortt/snow-white/commit/80c78ff9c0ae24a79d0002e9b894c79e3ee5ee3b))
- **#1697:** helm chart now supports grafana tempo datasource configuration ([b19002d](https://github.com/bbortt/snow-white/commit/b19002dc547bd13886ac1a2500907e6d94a74722))

### Bug Fixes

- **helm:** both otel-collector and openapi-coverage-stream referenced non-existing influxdb ([2261a9e](https://github.com/bbortt/snow-white/commit/2261a9edcf5d6fb98337361f4ff2c7b1347f5f80))
- **openapi-coverage-stream:** attribute filter for grafana tempo datasource ([8d61507](https://github.com/bbortt/snow-white/commit/8d615074af28942eecff5d7318c2cbb13c66aa77))

### Documentation

- add project-specific Claude Code skills and root-level docs ([d23abd9](https://github.com/bbortt/snow-white/commit/d23abd9f4e2e2ed7e3c97ea32b48f124b2eb6146))
- **helm:** grafana tempo configuration from secret values ([41cdce4](https://github.com/bbortt/snow-white/commit/41cdce460bdcc036746b8078e0edda4688037fe3))

## [1.8.0](https://github.com/bbortt/snow-white/compare/v1.7.0...v1.8.0) (2026-08-18)

### Features

- **report-coordinator-api:** tag current span with report uuid ([9df127c](https://github.com/bbortt/snow-white/commit/9df127c880f5d222a3601c3adf778883d5b239f0))

### Bug Fixes

- end 2 end tracing of openapi coverage requests ([093261e](https://github.com/bbortt/snow-white/commit/093261e563b62f841befcc0ff2d1f03705b336c7))

### Documentation

- fix pages build ([5b42af3](https://github.com/bbortt/snow-white/commit/5b42af32157df17f85f3d1beb8f6716b47103f4e))

## [1.7.0](https://github.com/bbortt/snow-white/compare/v1.6.0...v1.7.0) (2026-08-11)

### Features

- **#1697:** support multi tenant grafana tempo instances ([ff0ab74](https://github.com/bbortt/snow-white/commit/ff0ab74d735af2c6220f62bdfe3d27f262587790))

### Bug Fixes

- **deps:** update opentelemetry-proto to v1.11.0 ([e4314c8](https://github.com/bbortt/snow-white/commit/e4314c87f0f6ebd832f91220e53a0d13edbbae49))
- **helm:** expose debug/basic sampling rate through values ([5d8f1ca](https://github.com/bbortt/snow-white/commit/5d8f1caf798c62f32366f318cfb09bcbcb893936))
- **openapi-coverage-stream:** non-reachable api not indexed branch ([99ef27a](https://github.com/bbortt/snow-white/commit/99ef27aa55e1ea9e23ec9359f4f8d8ac98b4b0ba))
- **report-coordinator-api:** wrong return value when downstream connection fails ([92c2c7a](https://github.com/bbortt/snow-white/commit/92c2c7a9004342df0471a8a8aa0e2378344d64fd))

### Documentation

- adjust requirements based on new application tests ([e110eff](https://github.com/bbortt/snow-white/commit/e110eff20c39225207d63a4fbebd069584711768))
- **cli:** document missing junit output file option ([14e0199](https://github.com/bbortt/snow-white/commit/14e01995a59a9c1872dfa422e251255545d55097))

## [1.6.0](https://github.com/bbortt/snow-white/compare/v1.5.1...v1.6.0) (2026-07-26)

### Features

- **helm:** expose kafka resource settings ([79f3c61](https://github.com/bbortt/snow-white/commit/79f3c6181869d7af95740e04092ffa6462b4949b))

### Bug Fixes

- **openapi-coverage-stream:** left padding of trace id ([0d6fe19](https://github.com/bbortt/snow-white/commit/0d6fe19401bcc9ea4451bbd1899c3b48bf89db6f))
- **report-coordinator-api:** major bug where api-index-api was called instead of quality-gate-api ([29f55b8](https://github.com/bbortt/snow-white/commit/29f55b80dc14d5eb7d2ca639ba0857907ec6bf82))
- **report-coordinator-api:** race condition within result linking service and parallel stream ([82b85ad](https://github.com/bbortt/snow-white/commit/82b85adbc61e6a663143d3580d0a03d68a7609c7))

## [1.5.1](https://github.com/bbortt/snow-white/compare/v1.5.0...v1.5.1) (2026-07-16)

### Bug Fixes

- **helm:** invalid definition of trace_conditions ([bf1ad46](https://github.com/bbortt/snow-white/commit/bf1ad460f68597e1056b54106116a1cb1dc2d6ff))

### Documentation

- remove claude binary skill and replace with markdown ([3f9573e](https://github.com/bbortt/snow-white/commit/3f9573e78becfd75419a99e96bc9a379433e8fea))

## [1.5.0](https://github.com/bbortt/snow-white/compare/v1.4.0...v1.5.0) (2026-07-15)

### Features

- **#1697:** add grafana tempo properties to openapi-coverage-stream ([797fd24](https://github.com/bbortt/snow-white/commit/797fd2486c00d385c340ab64a09d60c69fcb662a))
- **#1697:** implement tempo service for openapi-coverage-stream ([11d1a8f](https://github.com/bbortt/snow-white/commit/11d1a8fd66a242b0b928a8b8b432f83a8539b3bd))
- **#1697:** influxdb configuration in openapi-coverage-service is now optional ([78eb6db](https://github.com/bbortt/snow-white/commit/78eb6dbdae1ee66985f1fb4302b6679bcf733dd5))

### Bug Fixes

- **api-sync-job:** do not fetch references for meta information parsing ([0449b1d](https://github.com/bbortt/snow-white/commit/0449b1d9977a535267e1ec95ff9bf6e69b09924f))
- **api-sync-job:** double memory copy ([cff8d51](https://github.com/bbortt/snow-white/commit/cff8d511f427a5dc912347494e04936e232504b3))

## [1.4.0](https://github.com/bbortt/snow-white/compare/v1.3.0...v1.4.0) (2026-07-03)

### Features

- add claude skill ([268e654](https://github.com/bbortt/snow-white/commit/268e65425a9cc3ac32e00401fe765a39df15db72))

### Bug Fixes

- **helm:** adjust kafka memory limit to requested resource value ([6a76ff4](https://github.com/bbortt/snow-white/commit/6a76ff45fbb836914d0adbfe2271b28d7534500d))
- migrate otel filter processor to new format ([5254524](https://github.com/bbortt/snow-white/commit/5254524a5f828a791ad8e0fff4e842de91de9364))
- **openapi-coverage-stream:** immediately shutdown stream if influxdb connection fails ([43ef925](https://github.com/bbortt/snow-white/commit/43ef925672c176bceaa05a91671eefd6168fa754))

### Documentation

- header was invisible ([955a7ec](https://github.com/bbortt/snow-white/commit/955a7ecb9485d49e281c6b4202f5bb22598f0e0a))

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
