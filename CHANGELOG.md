# Changelog

## 1.0.0 (2025-12-05)


### Features

* **#145:** include filter attributes in flux query ([09ac8bd](https://github.com/bbortt/snow-white/commit/09ac8bd19913c6d822da15afa04f537051b7aa95))
* **#145:** publish filter attributes to coverage request events ([e07adfb](https://github.com/bbortt/snow-white/commit/e07adfbb6373f0b64bbef1d75239211551825301))
* **#447:** return all quality-gate results to ui ([e5d06be](https://github.com/bbortt/snow-white/commit/e5d06be167dd55c867f4edeee66f97bef6584669))
* **#453:** include pie charts indicating test result status ([d24a1d4](https://github.com/bbortt/snow-white/commit/d24a1d4195907b33ef2abac6bc30ad0cc7d4cd40))
* **api-gateway:** better status badges ([fac1e41](https://github.com/bbortt/snow-white/commit/fac1e411f6049b3bc93b5363d37d53066f7b1dc2))
* **api-gateway:** deploy api-gateway microservice with snow-white helm chart ([d4faf22](https://github.com/bbortt/snow-white/commit/d4faf22f39cc452b1219639a4b76fb756314369a))
* **api-gateway:** improve test result card ([59197f0](https://github.com/bbortt/snow-white/commit/59197f07ad9a2f2abe2fabac5823163e27904ce4))
* **cli:** adapt to multiple api option api ([61aa9c2](https://github.com/bbortt/snow-white/commit/61aa9c2352b9db7bce3dbc137555ba79fa429bcb))
* compile on jdk 25 ([1e65168](https://github.com/bbortt/snow-white/commit/1e65168eea36cbcb604d63c6f6b3720c08c064ae))
* **helm:** add api-gateway ([de0908b](https://github.com/bbortt/snow-white/commit/de0908b66f6d4f2f889674e31a7711ad717b07d4))
* **openapi-coverage-service:** response code coverage ([8664945](https://github.com/bbortt/snow-white/commit/866494547f93f6771004fc0cc090d19b266f90c1))
* **openapi-coverage-stream:** rename kafka stream from openapi-coverage-service ([571c1b9](https://github.com/bbortt/snow-white/commit/571c1b9bb31fc65ffc7ad181dfff565f12ae7058))
* **otel-event-filter-stream:** rename kafka stream filter from kafka-event-filter ([16e70da](https://github.com/bbortt/snow-white/commit/16e70da3db5197d8ee0e31f54aa0ad54ee8ab6f8))
* **report-coordination-service:** accept multiple openapi specs per calculation request ([5d3c076](https://github.com/bbortt/snow-white/commit/5d3c0765cf6dbf0aaa31223486c20cd167cc65f1))
* **report-coordination-service:** include quality-gate importance in results ([1c753dd](https://github.com/bbortt/snow-white/commit/1c753dd88bc38c5ac61fdb4cbb61964e35188432))
* **report-coordinator-api:** rename http microservice from report-coordination-service ([c9f0c01](https://github.com/bbortt/snow-white/commit/c9f0c0188eef1e3d31706cb91ce456a7f3032426))
* **sonar:** remove code smells and vulnerabilities ([0fb6023](https://github.com/bbortt/snow-white/commit/0fb60238c66c29d353f7c70af4f069db2fc02c11))


### Bug Fixes

* **#453:** more robust error handling in chart component ([8ee49f9](https://github.com/bbortt/snow-white/commit/8ee49f902e21f15cc52b748f1995d1fc8573f45e))
* **api-gateway:** detail charts were switched ([c8fef6d](https://github.com/bbortt/snow-white/commit/c8fef6d7f00e9a995a3765807e409c4c7a3326a3))
* **api-gateway:** frontend build ([ca96766](https://github.com/bbortt/snow-white/commit/ca96766f1ef932ffdb2e3d50b75bee08227e0415))
* **api-gateway:** swagger ui icon color ([862e937](https://github.com/bbortt/snow-white/commit/862e93729522272620ae53f7179b4fe8db262c38))
* **api-gateway:** switch to module resolution bundler ([6e1092d](https://github.com/bbortt/snow-white/commit/6e1092d42a5d75d4d1031fff345d7c329434a2d9))
* **api-sync-job:** include api type in index ([000d68b](https://github.com/bbortt/snow-white/commit/000d68b90719cbac4ac68e84b230aaa901f8f060))
* **cli:** display correct error message returned from servers ([9ac67cc](https://github.com/bbortt/snow-white/commit/9ac67cc224309756b578e4859622b38303aca54d))
* **cli:** eslint dependencies mismatch ([66bc630](https://github.com/bbortt/snow-white/commit/66bc6309ea8a91f637b88bdeb27865f3be30a951))
* **cli:** exit code on error ([c96523b](https://github.com/bbortt/snow-white/commit/c96523b87e6dd36ed8bd0c621e85e29cf11a58a2))
* **cli:** improved error message when config file not found ([201da8f](https://github.com/bbortt/snow-white/commit/201da8f132395d98327b161e7c3255fa7dd40cd4))
* **deps:** include okhttp-jvm instead of okhttp main dependency ([2a10d4c](https://github.com/bbortt/snow-white/commit/2a10d4ca9ac8a47bdd084292351b5c4e3e823a24))
* **deps:** pin flyway-core to same version as flyway-database-postgresql ([6d0c1cb](https://github.com/bbortt/snow-white/commit/6d0c1cbe78e41166543c0e6e3266824c121fd3bd))
* **deps:** remove avro dependency in kafka-event-filter ([eb351cc](https://github.com/bbortt/snow-white/commit/eb351cc1900c2c4e539f39c0a008c20fb236b1ab))
* **deps:** remove remnants of org.fasterxml.jackson package usages ([c0bf567](https://github.com/bbortt/snow-white/commit/c0bf567f4951e18eb143ffe6e950ac253ffda425))
* **deps:** update postgres to 18.0-alpine across the board ([b8d7fa3](https://github.com/bbortt/snow-white/commit/b8d7fa325f29516ac4aaf2752ca8aaf1e907bf28))
* **deps:** update spring-boot to 4.0.0-RC1 ([06c533f](https://github.com/bbortt/snow-white/commit/06c533fca67e8809229523059333527690f6068d))
* **helm:** deploy to local k8s cluster ([134149e](https://github.com/bbortt/snow-white/commit/134149e576037107abcba3c66443aa6f0702a3dc))
* **helm:** init postgresql databases for microservices ([b2f398a](https://github.com/bbortt/snow-white/commit/b2f398a2fcedff2b5fa325269ead3fe8c9a4c30b))
* **helm:** installation of quality-gate-api ([c768e0a](https://github.com/bbortt/snow-white/commit/c768e0ab951548a1782b024183f34485b0b3fff7))
* **openapi-coverage-service:** calculate coverage with api information ([fcad3ec](https://github.com/bbortt/snow-white/commit/fcad3eca03ec5bef5301b2a53b1044ec46c84f38))
* **openapi-coverage-service:** return additional information as code ([326f000](https://github.com/bbortt/snow-white/commit/326f000ecf391663ef053436e9ede06202c5ebc5))
* **openapi-coverage-service:** security-hardened images ([4ec5182](https://github.com/bbortt/snow-white/commit/4ec518250b886a2f979462dfe4d163619dea6608))
* **quality-gate-api:** missing parameter names in aot processing ([950c5e9](https://github.com/bbortt/snow-white/commit/950c5e91f0d0b07dd7fb72abae2c1a8d2ec574bd))
* **quality-gate-api:** native compilation of rest dtos ([0f993da](https://github.com/bbortt/snow-white/commit/0f993da0e1d603c33dd67b70b06ecf951c2711a6))
* **report-coordination-service:** native compilation ([fc9aa90](https://github.com/bbortt/snow-white/commit/fc9aa9049c9be9336ddde12cc757e04820fd248a))
* **report-coordinator-api:** main class compatibility with graalvm ([e98bcf8](https://github.com/bbortt/snow-white/commit/e98bcf8415814e305e60a2f58516b12128c67adb))
* **report-corodinator-api:** native image build ([872cf97](https://github.com/bbortt/snow-white/commit/872cf97063351c2ffa45af3d4d10c407ad577f95))
* return location header when quality-gate calculation has been triggered ([9a7930e](https://github.com/bbortt/snow-white/commit/9a7930e520fb9dd7e18478c625d2d6ebff4a50a6))


### Documentation

* general concept of snow-white ([099a6a6](https://github.com/bbortt/snow-white/commit/099a6a6260900dda04617b2e66e4e3736d8a8d09))
* updated dev docs ([daa5be3](https://github.com/bbortt/snow-white/commit/daa5be32cbb40413bff6098e609425a5b31a6ad4))
