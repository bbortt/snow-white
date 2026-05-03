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

Every path defined in the specification has been called.
This is a subset of [HTTP Method Coverage](#http-method-coverage).

### HTTP Method Coverage

Each HTTP method (`GET`, `POST`, `PUT`, `DELETE`, etc.) for each path has been tested.

### Operation Success Coverage

Each operation (unique path + HTTP method combination) has produced at least one successful (2xx) response.
Complements [HTTP Method Coverage](#http-method-coverage), which only checks that an operation was called at all.

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

Several criteria form a hierarchy — satisfying a broader criterion implies narrower ones were also satisfied:

```plaintext
RESPONSE_CODE_COVERAGE
├── POSITIVE_RESPONSE_CODE_COVERAGE
└── ERROR_RESPONSE_CODE_COVERAGE

NO_UNDOCUMENTED_RESPONSE_CODES
├── NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES
└── NO_UNDOCUMENTED_ERROR_RESPONSE_CODES

PARAMETER_COVERAGE
├── REQUIRED_PARAMETER_COVERAGE
└── OPTIONAL_PARAMETER_COVERAGE

HTTP_METHOD_COVERAGE
└── PATH_COVERAGE
```

Understanding these relationships helps when composing custom quality gates: requiring a parent criterion already implies its subsets.
