# OpenAPI

## OpenAPI Attributes

This group describes attributes used for correlation of [Open Telemetry](https://github.com/open-telemetry) data with [OpenAPI](https://www.openapis.org) specifications.

| Attribute                                                                                                                      | Type    | Description                                                                                                                                                                  | Examples                         | Stability                                                      |
| ------------------------------------------------------------------------------------------------------------------------------ | ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------- | -------------------------------------------------------------- |
| <a id="openapi-name" href="#openapi-name">`api.name`</a>                                                                       | String  | Name of the attribute correlating an OTel span with the name of the OpenAPI it presents.                                                                                     | `Swagger Petstore - OpenAPI 3.0` | ![Development](https://img.shields.io/badge/-development-blue) |
| <a id="openapi-version" href="#openapi-version">`api.version`</a>                                                              | String  | Name of the attribute correlating an OTel span with the version of the OpenAPI it presents.                                                                                  | `1.2.3`                          | ![Development](https://img.shields.io/badge/-development-blue) |
| <a id="openapi-operation-id" href="#openapi-operation-id">`openapi.operation.id`</a>                                           | String  | Name of the attribute correlating an OTel span with specific operation of an OpenAPI.                                                                                        | `doPing`                         | ![Development](https://img.shields.io/badge/-development-blue) |
| <a id="openapi-response-schema-valid" href="#openapi-response-schema-valid">`openapi.response.schema.valid`</a>                | Boolean | Whether a validator judged this response to match the schema its OpenAPI specification documents. Presence means a verdict was reached; `true` means the response conformed. | `false`                          | ![Development](https://img.shields.io/badge/-development-blue) |
| <a id="openapi-response-schema-violations" href="#openapi-response-schema-violations">`openapi.response.schema.violations`</a> | String  | A JSON array of JSON Pointers, each relative to the response schema the document declares for this response. Present only where `openapi.response.schema.valid` is `false`.  | `["/properties/id","/required"]` | ![Development](https://img.shields.io/badge/-development-blue) |

The violation list is a serialised string rather than an OpenTelemetry array attribute because the analysed dataset cannot carry an array.
A string passes through both supported telemetry backends unchanged, and any producer can emit one by serialising a list.

## Response Schema Conformance: the Producer Contract

Snow-White does not validate response payloads.
Its `RESPONSE_SCHEMA_CONFORMANCE` criterion judges verdicts a test harness published, and it reads the span rather than who emitted it: any framework that emits the span below is judged exactly as the Citrus producer shipped in `citrus-junit-jupiter-extension` is.

A conformance verdict travels on a span carrying all of the following.

- **One validated response per span.** Never two responses' verdicts on one span.
- **`service.name` as a _resource_ attribute**, equal to the service name the specification declares (`x-service-name`), not as a span attribute.
  This is what makes a span the harness exported analysable at all: the correlation query is keyed on the resource's service name.
- **`api.name` and `api.version` as span attributes**, equal to the values the specification declares.
- **`openapi.operation.id` and `http.response.status_code` as span attributes.**
  Together they locate the (operation, response code) pair the verdict is about.
  A span missing either identifies no pair and contributes nothing, which is the same outcome as carrying no verdict at all.
- **`openapi.response.schema.valid`**, and where it is `false`, **`openapi.response.schema.violations`**.
- **Every _resource_ attribute a calculation filters on**, with the value the application's own spans carry.
  A calculation's attribute filters apply to a verdict span exactly as to the application's, so a verdict span lacking, say, the `deployment.environment` a calculation filters on is never read — and the criterion then reports no verdict at all rather than the verdicts that exist.
- **Export through any OTLP path** that reaches the configured telemetry backend.

A producer need not emit `http.request.method`.
The collector configurations Snow-White ships admit a span into the analysed dataset when it carries either `http.request.method` or `openapi.response.schema.valid`, alongside `api.name` and `api.version`.
An operator running their own collector filter has to admit the same.

Carrying `openapi.response.schema.valid` also marks the span as a harness span, so it contributes to `RESPONSE_SCHEMA_CONFORMANCE` and to no other criterion.
A producer need not — and must not — do anything further to keep its spans out of reach coverage.

### How Verdicts Are Read

Every ambiguous reading resolves to "did not conform", because the alternative lets a malformed attribute silence the check.

| On the span                                       | Read as                                                           |
| ------------------------------------------------- | ----------------------------------------------------------------- |
| `valid=true`, no violations                       | conformed                                                         |
| `valid=false`, with violations                    | did not conform, one finding per pointer                          |
| `valid=true` alongside a non-empty violation list | did not conform — the two disagree, and the failing reading wins  |
| violations present, `valid` absent                | did not conform                                                   |
| `valid` neither boolean nor `"true"`/`"false"`    | did not conform, logged at `WARN` without the value               |
| neither attribute present                         | no verdict — the response is not judged, and nothing fails for it |

Both attribute keys are operator-configurable on `openapi-coverage-stream` (`snow.white.openapi.coverage.stream.response-schema-valid-attribute` and `…-response-schema-violations-attribute`), defaulting to the names above.

### Bounds

A verdict carries at most **32** violation pointers, each at most **512 UTF-8 bytes**.
Both bounds are applied by the shipped producer when it captures a verdict and again by `openapi-coverage-stream` when it reads one, because the contract admits producers Snow-White cannot vouch for.
Beyond either bound the surplus pointers are dropped — never truncated — the `valid=false` verdict survives, and the drop is logged at `WARN`.
A dropped pointer costs a finding's detail, never the finding that the response did not conform.

Each pointer is a JSON Pointer intended to resolve into that response's schema.
A pointer that does not resolve is not discarded: the finding is reported at the nearest ancestor that does resolve, down to the response entry itself.
