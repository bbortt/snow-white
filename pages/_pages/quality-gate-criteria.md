---
title: 'Quality Gate Criteria'
permalink: /quality-gate-criteria/
toc: true
toc_sticky: true
---

Quality gate criteria are the individual checks Snow-White evaluates when running an analysis.
Each criterion compares what your API specification declares against what your runtime telemetry actually observed.

## Specification Format Support

Snow-White currently evaluates criteria against **OpenAPI specifications** (v3.x).
Support for additional specification formats — most notably **AsyncAPI** for event-driven APIs — is planned.
The criteria model itself is format-agnostic by design, so new formats slot in without changing how quality gates are defined or evaluated.

## Available Criteria

### Path Coverage

Every path defined in the specification has been called, by any HTTP method.
Judged per path, where [HTTP Method Coverage](#http-method-coverage) is judged per operation.

### HTTP Method Coverage

Each HTTP method (`GET`, `POST`, `PUT`, `DELETE`, etc.) for each path has been tested.

### Operation Success Coverage

Each operation (unique path + HTTP method combination) has produced at least one successful (2xx) response, not merely a call.
This is a stricter check than [HTTP Method Coverage](#http-method-coverage).

### Error Response Code Coverage

Each documented error response code for each endpoint is tested.
This is a subset of [Response Code Coverage](#response-code-coverage).

### Positive Response Code Coverage

Each documented positive (non-error) response code (1xx, 2xx, 3xx) for each endpoint is tested.
This is a subset of [Response Code Coverage](#response-code-coverage).

### Response Code Coverage

Each documented response code for each endpoint is tested.

### Required Parameter Coverage

Each required parameter (in path, query) has been tested with valid values.
This is a subset of [Parameter Coverage](#parameter-coverage).

### Optional Parameter Coverage

Each optional (non-required) parameter (in path, query) has been tested with valid values.
This is a subset of [Parameter Coverage](#parameter-coverage).

### Parameter Coverage

Each parameter (in path, query) has been tested with valid values.

### Content Type Coverage

Each documented request body content type (e.g. `application/json`, `multipart/form-data`) for each endpoint has been exercised.

### Required Error Fields Coverage

Error responses include all required fields as declared in the specification.

### All Response Codes must be Specified

All response codes (including errors) that occurred must be documented in the specification.
Catches undocumented behavior before it reaches production.

### All Error Response Codes must be Specified

All error response codes that occurred must be documented in the specification.
This is a subset of [All Response Codes must be Specified](#all-response-codes-must-be-specified).

### All Non-Erroneous Response Codes must be Specified

All response codes that occurred and are not being considered errors (0–399) must be documented in the specification.
This is a subset of [All Response Codes must be Specified](#all-response-codes-must-be-specified).

## Criteria Relationships

Several criteria form a hierarchy — a narrower criterion sits under the broader one it is a narrowing of:

<!-- This tree is a view of the containment declared on `OpenApiCoverageCriteria`, which is where
the relation is edited; `OpenApiCoverageCriteriaUnitTest` fails when the two disagree. -->

```plaintext
HTTP_METHOD_COVERAGE
└── OPERATION_SUCCESS_COVERAGE

RESPONSE_CODE_COVERAGE
├── POSITIVE_RESPONSE_CODE_COVERAGE
└── ERROR_RESPONSE_CODE_COVERAGE

NO_UNDOCUMENTED_RESPONSE_CODES
├── NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES
└── NO_UNDOCUMENTED_ERROR_RESPONSE_CODES

PARAMETER_COVERAGE
├── REQUIRED_PARAMETER_COVERAGE
└── OPTIONAL_PARAMETER_COVERAGE
```

A criterion narrows the one above it in one of two ways, and which way decides what the tree tells you about coverage.
Three of the four groups are **subsets**: the child applies the same check to fewer targets, so a parent at 100% means every child is at 100% too — full response-code coverage is full positive- and error-response-code coverage.
`OPERATION_SUCCESS_COVERAGE` under `HTTP_METHOD_COVERAGE` is a **stricter check** instead: both judge exactly the same operations, and success is the harder bar.
There the implication runs the other way — full operation-success coverage is full method coverage, while full method coverage only says every operation was called, not that any of them ever worked.

So when composing a custom quality gate, requiring a parent already covers its subsets, but requiring `HTTP_METHOD_COVERAGE` does not cover `OPERATION_SUCCESS_COVERAGE`.
Require the stricter criterion if that is what you mean.

`PATH_COVERAGE` is in neither group.
It and `HTTP_METHOD_COVERAGE` judge different things — a path item, and one operation within a path item — so neither one's targets are a subset of the other's and neither is a stricter check on the same targets.
Full method coverage does imply full path coverage, but never the reverse: a path whose `GET` was called and whose `POST` was not is fully covered for path coverage and half covered for method coverage.
Below 100% even that one direction breaks down, so requiring either criterion at a threshold tells you nothing about the other.
Require both if you want both reported.
