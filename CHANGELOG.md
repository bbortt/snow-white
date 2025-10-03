# Changelog

## 1.0.0 (2025-10-03)


### Features

* **#145:** include filter attributes in flux query ([09ac8bd](https://github.com/bbortt/snow-white/commit/09ac8bd19913c6d822da15afa04f537051b7aa95))
* **#145:** publish filter attributes to coverage request events ([e07adfb](https://github.com/bbortt/snow-white/commit/e07adfbb6373f0b64bbef1d75239211551825301))
* **#447:** return all quality-gate results to ui ([e5d06be](https://github.com/bbortt/snow-white/commit/e5d06be167dd55c867f4edeee66f97bef6584669))
* **#453:** include pie charts indicating test result status ([d24a1d4](https://github.com/bbortt/snow-white/commit/d24a1d4195907b33ef2abac6bc30ad0cc7d4cd40))
* add image oci labels ([d5b70bd](https://github.com/bbortt/snow-white/commit/d5b70bd9b7568495f6c029bf797354ca14c5e0c0))
* **api-gateway:** better status badges ([fac1e41](https://github.com/bbortt/snow-white/commit/fac1e411f6049b3bc93b5363d37d53066f7b1dc2))
* **api-gateway:** improve test result card ([59197f0](https://github.com/bbortt/snow-white/commit/59197f07ad9a2f2abe2fabac5823163e27904ce4))
* **api-gateway:** quality gate details ([3875fe4](https://github.com/bbortt/snow-white/commit/3875fe4db680f53b9a8ba1a49a1022fcf3ffa176))
* **api-sync-job:** add backstage entity sync ([fe46d55](https://github.com/bbortt/snow-white/commit/fe46d552c2f4f042361df23edbcafe005cfe09b3))
* **cli:** adapt to multiple api option api ([61aa9c2](https://github.com/bbortt/snow-white/commit/61aa9c2352b9db7bce3dbc137555ba79fa429bcb))
* **cli:** with this it's possible to trigger quality-gate calculation from command line ([bdc12a5](https://github.com/bbortt/snow-white/commit/bdc12a5395496cbb8ffc1ecb83dabe452c5ef84d))
* **docker-compose:** add resource limits ([9f36b78](https://github.com/bbortt/snow-white/commit/9f36b7838bedbcddb0058c31642aac5049bb1a39))
* **helm:** add api-gateway ([de0908b](https://github.com/bbortt/snow-white/commit/de0908b66f6d4f2f889674e31a7711ad717b07d4))
* **openapi-coverage-service:** response code coverage ([8664945](https://github.com/bbortt/snow-white/commit/866494547f93f6771004fc0cc090d19b266f90c1))
* **persistency-layer:** use postgresql for all relational data storages ([7d28c63](https://github.com/bbortt/snow-white/commit/7d28c631438481790561fd9fa040833a03859300))
* **report-coordination-service:** accept multiple openapi specs per calculation request ([5d3c076](https://github.com/bbortt/snow-white/commit/5d3c0765cf6dbf0aaa31223486c20cd167cc65f1))
* **report-coordination-service:** include quality-gate importance in results ([1c753dd](https://github.com/bbortt/snow-white/commit/1c753dd88bc38c5ac61fdb4cbb61964e35188432))
* **sonar:** remove code smells and vulnerabilities ([0fb6023](https://github.com/bbortt/snow-white/commit/0fb60238c66c29d353f7c70af4f069db2fc02c11))


### Bug Fixes

* **#453:** more robust error handling in chart component ([8ee49f9](https://github.com/bbortt/snow-white/commit/8ee49f902e21f15cc52b748f1995d1fc8573f45e))
* adapt to openapi generator changes ([aa47306](https://github.com/bbortt/snow-white/commit/aa47306925d9cdb71a1120de3d3d781e4ae51cbd))
* **api-gateway:** detail charts were switched ([c8fef6d](https://github.com/bbortt/snow-white/commit/c8fef6d7f00e9a995a3765807e409c4c7a3326a3))
* **api-gateway:** frontend build ([ca96766](https://github.com/bbortt/snow-white/commit/ca96766f1ef932ffdb2e3d50b75bee08227e0415))
* **api-gateway:** republished eslint-config-prettier ([adb5fba](https://github.com/bbortt/snow-white/commit/adb5fba2e78f2caa81e23f3542dda91517ec6944))
* **api-gateway:** swagger ui icon color ([862e937](https://github.com/bbortt/snow-white/commit/862e93729522272620ae53f7179b4fe8db262c38))
* **api-gateway:** switch to module resolution bundler ([6e1092d](https://github.com/bbortt/snow-white/commit/6e1092d42a5d75d4d1031fff345d7c329434a2d9))
* **api-gateway:** translate openapi criteria ([ac6e882](https://github.com/bbortt/snow-white/commit/ac6e88294fa7ea7f27dd74b6e5d4763d7ee2e000))
* **api-gateway:** ux fixes ([5e0382f](https://github.com/bbortt/snow-white/commit/5e0382f909bff05b975ca2fd826e4db37f61448b))
* **api-sync-job:** include api type in index ([000d68b](https://github.com/bbortt/snow-white/commit/000d68b90719cbac4ac68e84b230aaa901f8f060))
* **cli:** display correct error message returned from servers ([9ac67cc](https://github.com/bbortt/snow-white/commit/9ac67cc224309756b578e4859622b38303aca54d))
* **cli:** eslint dependencies mismatch ([66bc630](https://github.com/bbortt/snow-white/commit/66bc6309ea8a91f637b88bdeb27865f3be30a951))
* **cli:** exit code on error ([c96523b](https://github.com/bbortt/snow-white/commit/c96523b87e6dd36ed8bd0c621e85e29cf11a58a2))
* **cli:** improved error message when config file not found ([201da8f](https://github.com/bbortt/snow-white/commit/201da8f132395d98327b161e7c3255fa7dd40cd4))
* **cli:** refactored linting and made code thus more secure ([7d599df](https://github.com/bbortt/snow-white/commit/7d599df80721daaa1e0bacdf5eec6f27a8e5f7ea))
* **cli:** republished eslint-config-prettier ([d1ec2d8](https://github.com/bbortt/snow-white/commit/d1ec2d8d74bc046746388ba93d2ed7dd487c0b31))
* **cli:** test setup ([5af5118](https://github.com/bbortt/snow-white/commit/5af51180bbbfc39c880590992561fb6c15f167cc))
* **deps:** prettier ([f3eb10e](https://github.com/bbortt/snow-white/commit/f3eb10eee7de32fd97132310dcc69b5463ab4df0))
* **deps:** update google protobuf to v4 ([2b4a638](https://github.com/bbortt/snow-white/commit/2b4a638a6920aca0ee1c4b3bc7d5a55f0faf49ab))
* **kafka-event-filter:** native runtime hints for otel classes ([e6178a1](https://github.com/bbortt/snow-white/commit/e6178a105b539f8caa3ff1e01f3c6ee9c4918dc1))
* **kafka:** topic management for native compiled sources ([9032833](https://github.com/bbortt/snow-white/commit/9032833afbe822c01968418401897906bdce1f5e))
* native image builds ([ec4626c](https://github.com/bbortt/snow-white/commit/ec4626c631342bd85963d2e6a80d982abc1a2220))
* **openapi-coverage-service:** calculate coverage with api information ([fcad3ec](https://github.com/bbortt/snow-white/commit/fcad3eca03ec5bef5301b2a53b1044ec46c84f38))
* **openapi-coverage-service:** return additional information as code ([326f000](https://github.com/bbortt/snow-white/commit/326f000ecf391663ef053436e9ede06202c5ebc5))
* **openapi-coverage-service:** security-hardened images ([4ec5182](https://github.com/bbortt/snow-white/commit/4ec518250b886a2f979462dfe4d163619dea6608))
* **postgres:** users and microservice connection ([41d1898](https://github.com/bbortt/snow-white/commit/41d18989d3d8366bddb5968eeaf794be7d57adf5))
* **quality-gate-api:** native compilation of rest dtos ([0f993da](https://github.com/bbortt/snow-white/commit/0f993da0e1d603c33dd67b70b06ecf951c2711a6))
* refactored properties ([4ee8608](https://github.com/bbortt/snow-white/commit/4ee86080a0ea08eff0ac0edc0cc5fc0e131d33a7))
* **report-coordination-service:** do not persist openapi criteria twice ([86b573f](https://github.com/bbortt/snow-white/commit/86b573fdfc8c7adc5a732d33d5eca11c7f430558))
* **report-coordination-service:** download junit xml report ([714e215](https://github.com/bbortt/snow-white/commit/714e215b24963b6a13ae01f915d7a1b86f03bf6b))
* **report-coordination-service:** native compilation ([fc9aa90](https://github.com/bbortt/snow-white/commit/fc9aa9049c9be9336ddde12cc757e04820fd248a))
* **report-coordination-service:** persistence of test results ([0956dcb](https://github.com/bbortt/snow-white/commit/0956dcb02fa69e5cdb0feac4f112d40abe12be39))
* **report-coordination-service:** switch to jackson xml writer ([902f298](https://github.com/bbortt/snow-white/commit/902f2981d78d6dfc388aeb0079aec2e7d7a0ffd9))
* return location header when quality-gate calculation has been triggered ([9a7930e](https://github.com/bbortt/snow-white/commit/9a7930e520fb9dd7e18478c625d2d6ebff4a50a6))
* **root:** republished eslint-config-prettier ([a8ed13f](https://github.com/bbortt/snow-white/commit/a8ed13f147d5c22205413326970c0e012dfe7e82))


### Documentation

* updated dev docs ([daa5be3](https://github.com/bbortt/snow-white/commit/daa5be32cbb40413bff6098e609425a5b31a6ad4))
* updated readme and development setup ([0d89397](https://github.com/bbortt/snow-white/commit/0d89397a1fbce270bdc11f916374970900abeead))
