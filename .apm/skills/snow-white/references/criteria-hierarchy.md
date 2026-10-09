# Quality Gate Criteria Reference

Every `testcase` in a Snow-White JUnit XML report corresponds to one criterion below, evaluated
for a single operation or endpoint.
Criterion names appear in the report exactly as the `UPPER_SNAKE_CASE` identifiers listed here.

Snow-White currently evaluates these against **OpenAPI** specifications (v3.x).
The criteria model is format-agnostic by design, so additional specification formats slot in
without changing how quality gates are defined.

## Coverage criteria

| Criterion                    | Label                      | Passes when                                                                                                                      |
| ---------------------------- | -------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| `PATH_COVERAGE`              | Path Coverage              | Every path defined in the specification has been called.                                                                         |
| `HTTP_METHOD_COVERAGE`       | HTTP Method Coverage       | Each HTTP method (`GET`, `POST`, `PUT`, `DELETE`, etc.) for each path has been tested.                                           |
| `OPERATION_SUCCESS_COVERAGE` | Operation Success Coverage | Each operation (unique path + HTTP method combination) has produced at least one successful (2xx) response, not merely a call.   |
| `CONTENT_TYPE_COVERAGE`      | Content Type Coverage      | Each documented request body content type (e.g. `application/json`, `multipart/form-data`) for each endpoint has been exercised. |

`OPERATION_SUCCESS_COVERAGE` is a stricter check than `HTTP_METHOD_COVERAGE`: both judge the same
operations, but method coverage only asks whether one was called at all, not whether it ever
succeeded.

## Response code criteria

| Criterion                         | Label                           | Passes when                                                                                     |
| --------------------------------- | ------------------------------- | ----------------------------------------------------------------------------------------------- |
| `RESPONSE_CODE_COVERAGE`          | Response Code Coverage          | Each documented response code for each endpoint is tested.                                      |
| `POSITIVE_RESPONSE_CODE_COVERAGE` | Positive Response Code Coverage | Each documented positive (non-error) response code (1xx, 2xx, 3xx) for each endpoint is tested. |
| `ERROR_RESPONSE_CODE_COVERAGE`    | Error Response Code Coverage    | Each documented error response code for each endpoint is tested.                                |

## Parameter criteria

| Criterion                     | Label                       | Passes when                                                                                |
| ----------------------------- | --------------------------- | ------------------------------------------------------------------------------------------ |
| `PARAMETER_COVERAGE`          | Parameter Coverage          | Each parameter (in path, query) has been tested with valid values.                         |
| `REQUIRED_PARAMETER_COVERAGE` | Required Parameter Coverage | Each required parameter (in path, query) has been tested with valid values.                |
| `OPTIONAL_PARAMETER_COVERAGE` | Optional Parameter Coverage | Each optional (non-required) parameter (in path, query) has been tested with valid values. |

## Specification-completeness criteria

These invert the usual direction: they fail when the runtime did something the specification
never documented.
The fix is normally in the **specification**, not the tests.

| Criterion                                 | Label                                              | Passes when                                                                                                 |
| ----------------------------------------- | -------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `NO_UNDOCUMENTED_RESPONSE_CODES`          | All Response Codes must be Specified               | All response codes (including errors) that occurred are documented in the specification.                    |
| `NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES` | All Non-Erroneous Response Codes must be Specified | All response codes that occurred and are not considered errors (0–399) are documented in the specification. |
| `NO_UNDOCUMENTED_ERROR_RESPONSE_CODES`    | All Error Response Codes must be Specified         | All error response codes that occurred are documented in the specification.                                 |

## Other criteria

| Criterion                        | Label                          | Passes when                                                                   |
| -------------------------------- | ------------------------------ | ----------------------------------------------------------------------------- |
| `REQUIRED_ERROR_FIELDS_COVERAGE` | Required Error Fields Coverage | Error responses include all required fields as declared in the specification. |

## The hierarchy

Several criteria are narrowings of others, in one of two ways:

```plaintext
HTTP_METHOD_COVERAGE
└── OPERATION_SUCCESS_COVERAGE

RESPONSE_CODE_COVERAGE
├── POSITIVE_RESPONSE_CODE_COVERAGE
└── ERROR_RESPONSE_CODE_COVERAGE

PARAMETER_COVERAGE
├── REQUIRED_PARAMETER_COVERAGE
└── OPTIONAL_PARAMETER_COVERAGE

NO_UNDOCUMENTED_RESPONSE_CODES
├── NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES
└── NO_UNDOCUMENTED_ERROR_RESPONSE_CODES
```

The last three groups are **subsets**: the child applies the same check to fewer targets, so a
parent at 100% puts every child at 100% too.
The first is a **stricter check**: `OPERATION_SUCCESS_COVERAGE` judges exactly the operations
`HTTP_METHOD_COVERAGE` judges, and demands more of each.
There the implication runs the other way — full operation-success coverage is full method coverage,
while full method coverage says only that every operation was called.

`PATH_COVERAGE`, `CONTENT_TYPE_COVERAGE` and `REQUIRED_ERROR_FIELDS_COVERAGE` stand alone — they
have neither parent nor children, so a failure there is always fixed directly.

`PATH_COVERAGE` and `HTTP_METHOD_COVERAGE` are not a parent/child pair, although they are often
read as one: a path item and one operation within it are different targets, so neither criterion's
checks are a subset of the other's.
Calling an operation does cover its path, so raising method coverage raises path coverage with it —
but path coverage can fail while method coverage passes on the paths it did reach, and a quality
gate requiring one does not check the other.

## Using the hierarchy to prioritize

Within a subset group, when several criteria fail for the same API, fix the highest-level failing
parent first.
A fix that satisfies `RESPONSE_CODE_COVERAGE` also satisfies `POSITIVE_RESPONSE_CODE_COVERAGE` and
`ERROR_RESPONSE_CODE_COVERAGE`, so addressing a child separately duplicates work.

Read a subset group in the other direction too: if a parent passes while a child fails, the parent
cannot help you — fix the child directly rather than broadening the change.

The stricter-check group inverts that advice.
When both `HTTP_METHOD_COVERAGE` and `OPERATION_SUCCESS_COVERAGE` fail, fix the **child**: making
every operation succeed makes every operation called.
Fixing the parent alone leaves the child exactly where it was.

## Online version

<https://timon-borter.ch/snow-white/quality-gate-criteria/>
