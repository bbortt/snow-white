# OpenAPI

## OpenAPI Attributes

This group describes attributes used for correlation of [Open Telemetry](https://github.com/open-telemetry) data with [OpenAPI](https://www.openapis.org) specifications.

| Attribute                                                                            | Type   | Description                                                                                 | Examples                         | Stability                                                      |
| ------------------------------------------------------------------------------------ | ------ | ------------------------------------------------------------------------------------------- | -------------------------------- | -------------------------------------------------------------- |
| <a id="openapi-name" href="#openapi-name">`api.name`</a>                             | String | Name of the attribute correlating an OTEL span with the name of the OpenAPI it presents.    | `Swagger Petstore - OpenAPI 3.0` | ![Development](https://img.shields.io/badge/-development-blue) |
| <a id="openapi-version" href="#openapi-version">`api.version`</a>                    | String | Name of the attribute correlating an OTEL span with the version of the OpenAPI it presents. | `1.2.3`                          | ![Development](https://img.shields.io/badge/-development-blue) |
| <a id="openapi-operation-id" href="#openapi-operation-id">`openapi.operation.id`</a> | String | Name of the attribute correlating an OTEL span with specific operation of an OpenAPI.       | `doPing`                         | ![Development](https://img.shields.io/badge/-development-blue) |
