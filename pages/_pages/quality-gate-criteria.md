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

## When a Target Cannot Be Covered

Sometimes one target holds back a criterion that works everywhere else.
A typical case is a documented `500` on one operation that only a failing downstream can provoke.
Removing `ERROR_RESPONSE_CODE_COVERAGE` from the gate would make that one target pass, but it would also stop the gate from checking every other error response.
Before that, try the two answers below, in this order.

### Cover It With a Real Test First

Snow-White only needs a span from the instrumented application, showing the request and the response it produced.
It does not care how the test got the application to produce that response.
A component test that mocks the _downstream_ dependency, so that your service answers `500`, is real evidence, provided that the test calls the service over HTTP with the OpenTelemetry agent attached.
Only mocking the handler layer itself hides the call from Snow-White.

So check first whether such a test exists or could be written, and make sure its traces reach Snow-White within the calculation's lookback window.
If the span also carries `test.case.name` (see [Onboarding](./onboarding.md)), the finding names the test that covered the target.
Then, every later run checks that the behaviour still exists.

### Waive It When Nothing Can Exercise It

A **waiver** declares that one criterion does not apply to one target of one API, with a reason, until a given date.
All other targets stay fully enforced, including other targets of the same criterion.
A waiver is an input to a single calculation.
It travels in the calculation request and is echoed back on the report, and Snow-White stores no waivers of its own.
With the CLI, waivers come from the [config file](./cli.md#waivers) only, never from a command-line flag, so adding one is always a change someone can review.

```json
{
  "serviceName": "order-service",
  "apiName": "orders-api",
  "apiVersion": "*",
  "path": "/orders/{orderId}",
  "method": "GET",
  "responseCode": "500",
  "criterion": "ERROR_RESPONSE_CODE_COVERAGE",
  "reason": "Only reachable by failing the database mid-request",
  "expires": "2027-04-30"
}
```

| Field                                            | Required | Meaning                                                                                                                 |
| ------------------------------------------------ | -------- | ----------------------------------------------------------------------------------------------------------------------- |
| `serviceName`, `apiName`                         | yes      | The API the waiver applies to, exactly as it is named in the calculation                                                |
| `apiVersion`                                     | yes      | An exact version, or `'*'` for every version of that API in the calculation. `'*'` is allowed in this field only        |
| `path`, `method`                                 | yes      | The target operation, with the path written exactly as in the specification (`/orders/{orderId}`, not `/orders/42`)     |
| `responseCode` / `parameterName` / `contentType` | no       | At most one of these. Narrows the target from the operation to one response code, parameter or request content type     |
| `criterion`                                      | no       | Limits the waiver to one criterion, as described in [Which Criteria a Waiver Reaches](#which-criteria-a-waiver-reaches) |
| `reason`                                         | yes      | A non-blank justification, shown wherever the waived target appears                                                     |
| `expires`                                        | yes      | The last day the waiver is in force, as a date (`YYYY-MM-DD`)                                                           |

The calculation is rejected with a `400`, and no report is created, if a waiver:

- is missing `reason` or `expires`;
- sets more than one of `responseCode`, `parameterName` and `contentType`;
- names a `criterion` that does not exist;
- names an API that is not part of the calculation.

### Which Criteria a Waiver Reaches

One target can be judged by several criteria.
For example, a documented `500` is judged by `RESPONSE_CODE_COVERAGE`, `ERROR_RESPONSE_CODE_COVERAGE` and `REQUIRED_ERROR_FIELDS_COVERAGE`.
These are different claims about the same response.

- **Without `criterion`**, the target is waived for every criterion that judged it.
  This is the strong claim: nothing can exercise this target.
- **With `criterion`**, the target is waived for that criterion and for every criterion [containing it](#criteria-relationships).
  `criterion: ERROR_RESPONSE_CODE_COVERAGE` also waives the target for `RESPONSE_CODE_COVERAGE`, so a waived child never leaves its parent failing on the same target.
  `REQUIRED_ERROR_FIELDS_COVERAGE` still judges that response.

A `criterion` that the chosen quality gate does not include is accepted.
A waiver list written once for a repository can then outlive changes to the gate.

### Expiry

`expires` is compared with the moment the calculation was triggered, which is the report's `initiatedAt`, not with the time the report is read.
Recalculating an old report therefore gives the same waiver verdicts.

An expired waiver is not silently dropped.
Its target is judged normally, as covered or uncovered according to the evidence, and the criterion's result names the waiver and the date it expired.
A build that turns red because of an expired waiver therefore says why.

### A Waiver That Matches Nothing Fails the Calculation

Every waiver has to match at least one target that a criterion actually judged.
Otherwise, that API's calculation fails, and the error names the waiver.
There are only two possible causes, and the author needs to know about both:

- the waiver was written wrong, for example `/orders/42` instead of `/orders/{orderId}`;
- the target no longer exists in the specification.

A waiver whose `apiVersion` is `'*'` has to match in every version it reaches.
It is the only kind of waiver whose target can disappear without anyone editing it.

### How a Waived Pass Is Reported

A waived target counts on neither side of the coverage ratio.
A criterion with ten targets, two of them waived, reports its coverage over the remaining eight.
If every target of a criterion is waived, it still reports `1.00`, but that is not a pass on coverage.
Every report shape lets you tell the two apart:

- **JSON report and `--agentic` output**: every criterion result carries `judgedTargetCount` (targets in the ratio) and `waivedTargetCount` (targets a waiver removed from it).
  `judgedTargetCount: 0` together with a `waivedTargetCount` above zero means the criterion was emptied by waivers, not fully covered.
  Waived findings have the status `WAIVED` and carry `waiverReason` and `waiverExpiresOn`.
- **JUnit export**: a criterion with waived targets is a passing `testcase` with `judgedTargetCount`, `waivedTargetCount` and `waivedTargets` properties.
  Its `system-out` lists each waived target with its expiry and reason.
  The `testsuites` root carries the document's total `waivedTargetCount`.
  `skipped` still means only that the gate excluded the criterion.
- **Web UI**: waived findings appear in the drilldown with their reason and expiry, and a criterion emptied by waivers is marked as fully waived.

A fully waived criterion neither raises nor lowers its API's verdict.
An API whose included criteria are all fully waived is `PASSED`, and only the counts show that this pass comes from waivers.
`coverage == 1.00` and a `PASSED` status are therefore never proof on their own that a criterion was satisfied.
