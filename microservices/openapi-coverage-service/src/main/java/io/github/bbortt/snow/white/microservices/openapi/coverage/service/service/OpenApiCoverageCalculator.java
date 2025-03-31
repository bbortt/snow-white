package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_HEADER;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_HEADER;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenApiCoverage;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class OpenApiCoverageCalculator {

  private final Map<String, Operation> operationsMap;
  private final Map<String, List<OpenTelemetryData>> pathToTelemetryMap;

  private static BigDecimal calculatePercentage(
    AtomicLong covered,
    AtomicLong required
  ) {
    if (covered.get() == 0 && required.get() == 0) {
      return ONE.setScale(2, HALF_UP);
    } else if (covered.get() == 0) {
      return ZERO.setScale(2, HALF_UP);
    }

    return new BigDecimal(covered.get()).divide(
      new BigDecimal(required.get()),
      2,
      HALF_UP
    );
  }

  OpenApiCoverage calculate() {
    var pathCoverage = calculatePathCoverage(operationsMap, pathToTelemetryMap);
    var responseCodeCoverage = calculateResponseCodeCoverage(
      operationsMap,
      pathToTelemetryMap
    );
    var requiredParameterCoverage = calculateRequiredParameterCoverage(
      operationsMap,
      pathToTelemetryMap
    );
    var queryParameterCoverage = calculateQueryParameterCoverage(
      operationsMap,
      pathToTelemetryMap
    );
    var headerParameterCoverage = calculateHeaderParameterCoverage(
      operationsMap,
      pathToTelemetryMap
    );
    var requestBodySchemaCoverage = calculateRequestBodySchemaCoverage(
      operationsMap,
      pathToTelemetryMap
    );
    var errorResponseCoverage = calculateErrorResponseCoverage(
      operationsMap,
      pathToTelemetryMap
    );
    var contentTypeCoverage = calculateContentTypeCoverage(
      operationsMap,
      pathToTelemetryMap
    );

    return new OpenApiCoverage(
      pathCoverage,
      responseCodeCoverage,
      requiredParameterCoverage,
      queryParameterCoverage,
      headerParameterCoverage,
      requestBodySchemaCoverage,
      errorResponseCoverage.atLeastOneTestExists(),
      errorResponseCoverage.coverage,
      contentTypeCoverage
    );
  }

  /**
   * Coverage criteria: Every endpoint in an OpenAPI specification has at least on test.
   *
   * @param operationsMap
   * @param pathToTelemetryMap
   * @return
   */
  private BigDecimal calculatePathCoverage(
    Map<String, Operation> operationsMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var pathsCovered = new AtomicLong(0);

    for (String operationKey : operationsMap.keySet()) {
      if (pathToTelemetryMap.containsKey(operationKey)) {
        logger.trace("Path covered: {}", operationKey);
        pathsCovered.incrementAndGet();
      } else if (logger.isTraceEnabled()) {
        logger.trace("Path not covered: {}", operationKey);
      }
    }

    return calculatePercentage(
      pathsCovered,
      new AtomicLong(pathToTelemetryMap.size())
    );
  }

  /**
   * Coverage criteria: Each documented response code for each endpoint is tested at least once.
   */
  private BigDecimal calculateResponseCodeCoverage(
    Map<String, Operation> operationsMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var totalResponseCodes = new AtomicLong(0);
    var responseCodesCovered = new AtomicLong(0);

    for (Map.Entry<String, Operation> entry : operationsMap.entrySet()) {
      var operationKey = entry.getKey();
      var operation = entry.getValue();

      if (isNull(operation.getResponses())) {
        logger.trace("Operation '{}' has no defined responses", operationKey);
        continue;
      }

      Set<String> expectedResponseCodes = operation.getResponses().keySet();
      Set<String> observedResponseCodes = new HashSet<>();

      if (pathToTelemetryMap.containsKey(operationKey)) {
        for (OpenTelemetryData data : pathToTelemetryMap.get(operationKey)) {
          if (data.attributes().has("http.response.status_code")) {
            observedResponseCodes.add(
              data.attributes().get("http.response.status_code").asText()
            );
          }
        }
      }

      var responseCodesInOperation = expectedResponseCodes.size();
      var responseCodesCoveredInOperation = observedResponseCodes
        .stream()
        .filter(expectedResponseCodes::contains)
        .count();

      logger.trace(
        "Expected {} response codes to be covered for operation '{}', {} were",
        responseCodesInOperation,
        operationKey,
        responseCodesCoveredInOperation
      );

      totalResponseCodes.addAndGet(responseCodesInOperation);
      responseCodesCovered.addAndGet(responseCodesCoveredInOperation);
    }

    return calculatePercentage(responseCodesCovered, totalResponseCodes);
  }

  /**
   * Coverage criteria: All <i>required</i> parameters for each endpoint are included in at least one test.
   */
  private BigDecimal calculateRequiredParameterCoverage(
    Map<String, Operation> operationsMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var totalRequiredParameters = new AtomicLong(0);
    var requiredParametersCovered = new AtomicLong(0);

    for (Map.Entry<String, Operation> entry : operationsMap.entrySet()) {
      var operationKey = entry.getKey();
      var operation = entry.getValue();

      if (isEmpty(operation.getParameters())) {
        logger.trace("Operation '{}' has no defined parameters", operationKey);
        continue;
      }

      var requiredParameters = operation
        .getParameters()
        .stream()
        .filter(param -> TRUE.equals(param.getRequired()))
        .toList();

      if (requiredParameters.isEmpty()) {
        logger.trace(
          "Operation '{}' has no defined required parameters",
          operationKey
        );
        continue;
      }

      totalRequiredParameters.addAndGet(requiredParameters.size());

      if (
        !pathToTelemetryMap.containsKey(operationKey) ||
        pathToTelemetryMap.get(operationKey).isEmpty()
      ) {
        logger.trace(
          "No telemetry data for operation with required parameters: {}",
          operationKey
        );
        continue;
      }

      for (Parameter requiredParam : requiredParameters) {
        var paramName = requiredParam.getName();
        var paramIn = requiredParam.getIn();

        boolean parameterFound = pathToTelemetryMap
          .get(operationKey)
          .stream()
          .anyMatch(openTelemetryData ->
            isParameterPresent(openTelemetryData, paramName, paramIn)
          );

        if (parameterFound) {
          logger.trace(
            "Required parameter '{}' found in operation '{}'",
            paramName,
            operationKey
          );

          requiredParametersCovered.incrementAndGet();
        } else {
          logger.trace(
            "Required parameter '{}' not found in operation '{}'",
            paramName,
            operationKey
          );
        }
      }
    }

    return calculatePercentage(
      requiredParametersCovered,
      totalRequiredParameters
    );
  }

  private boolean isParameterPresent(
    OpenTelemetryData data,
    String paramName,
    String paramIn
  ) {
    // TODO: Implement calculation
    return false;
  }

  /**
   * Coverage criteria: Each <i>optional</i> query parameter is tested in at least one request.
   */
  private BigDecimal calculateQueryParameterCoverage(
    Map<String, Operation> operationsMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var totalQueryParameters = new AtomicLong(0);
    var queryParametersCovered = new AtomicLong(0);

    for (Map.Entry<String, Operation> entry : operationsMap.entrySet()) {
      var operationKey = entry.getKey();
      var operation = entry.getValue();

      if (isEmpty(operation.getParameters())) {
        logger.trace("Operation '{}' has no defined parameters", operationKey);
        continue;
      }

      var queryParameters = operation
        .getParameters()
        .stream()
        .filter(
          param ->
            QUERY.toString().equals(param.getIn()) &&
            !TRUE.equals(param.getRequired())
        )
        .toList();

      if (queryParameters.isEmpty()) {
        logger.trace(
          "Operation '{}' has no defined (optional) query parameters",
          operationKey
        );
        continue;
      }

      totalQueryParameters.addAndGet(queryParameters.size());

      if (
        !pathToTelemetryMap.containsKey(operationKey) ||
        pathToTelemetryMap.get(operationKey).isEmpty()
      ) {
        logger.trace(
          "No telemetry data for operation with query parameters: {}",
          operationKey
        );
        continue;
      }

      for (Parameter queryParam : queryParameters) {
        var paramName = queryParam.getName();

        boolean parameterFound = false;

        for (OpenTelemetryData data : pathToTelemetryMap.get(operationKey)) {
          if (data.attributes().has(URL_QUERY.getKey())) {
            String queryString = data
              .attributes()
              .get(URL_QUERY.getKey())
              .asText();
            if (queryString.contains(paramName + "=")) {
              parameterFound = true;
              break;
            }
          }
        }

        if (!parameterFound) {
          logger.trace(
            "Query parameter '{}' not found in request '{}'",
            paramName,
            operationKey
          );
          queryParametersCovered.incrementAndGet();
        } else {
          logger.trace(
            "Query parameter '{}' not found in operation '{}'",
            paramName,
            operationKey
          );
        }
      }
    }

    return calculatePercentage(queryParametersCovered, totalQueryParameters);
  }

  /**
   * Coverage criteria: Each <i>documented</i> header parameter is included in at least one test.
   */
  private BigDecimal calculateHeaderParameterCoverage(
    Map<String, Operation> operationsMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var documentedHeaderParametersTotal = new AtomicLong(0);
    var documentedHeaderParametersCovered = new AtomicLong(0);

    for (Map.Entry<String, Operation> entry : operationsMap.entrySet()) {
      String operationKey = entry.getKey();
      Operation operation = entry.getValue();

      if (isEmpty(operation.getParameters())) {
        logger.trace("Operation '{}' has no defined parameters", operationKey);
        continue;
      }

      var headerParameters = operation
        .getParameters()
        .stream()
        .filter(param -> "header".equals(param.getIn()))
        .toList();

      if (headerParameters.isEmpty()) {
        logger.trace(
          "Operation '{}' has no defined header parameters",
          operationKey
        );
        continue;
      }

      documentedHeaderParametersTotal.addAndGet(headerParameters.size());

      if (
        !pathToTelemetryMap.containsKey(operationKey) ||
        pathToTelemetryMap.get(operationKey).isEmpty()
      ) {
        logger.trace(
          "No telemetry data for operation with header parameters: {}",
          operationKey
        );
        continue;
      }

      for (Parameter headerParam : headerParameters) {
        var paramName = headerParam.getName();

        boolean parameterFound = false;

        for (OpenTelemetryData data : pathToTelemetryMap.get(operationKey)) {
          // Note: OpenTelemetry may not capture all headers, so this check might be incomplete
          if (
            data
              .attributes()
              .has(HTTP_REQUEST_HEADER + "." + paramName.toLowerCase())
          ) {
            parameterFound = true;
            break;
          }
        }

        if (parameterFound) {
          logger.trace(
            "Header parameter '{}' found in operation '{}'",
            paramName,
            operationKey
          );

          documentedHeaderParametersCovered.incrementAndGet();
        } else {
          logger.trace(
            "Header parameter '{}' not found in operation '{}'",
            paramName,
            operationKey
          );
        }
      }
    }

    return calculatePercentage(
      documentedHeaderParametersCovered,
      documentedHeaderParametersTotal
    );
  }

  /**
   * Coverage criteria: For endpoints accepting request bodies, at least one test covers each required property.
   */
  private BigDecimal calculateRequestBodySchemaCoverage(
    Map<String, Operation> operationsMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var requestBodySchemasTotal = new AtomicLong(0);
    var requestBodySchemasCovered = new AtomicLong(0);

    for (Map.Entry<String, Operation> entry : operationsMap.entrySet()) {
      var operationKey = entry.getKey();
      var operation = entry.getValue();

      if (isNull(operation.getRequestBody())) {
        logger.trace(
          "Operation '{}' has no defined request body schema",
          operationKey
        );
        continue;
      }

      var requestBody = operation.getRequestBody();
      if (isEmpty(requestBody.getContent())) {
        logger.trace(
          "Operation '{}' has empty defined request body content",
          operationKey
        );
        continue;
      }

      // Get required properties from request body schema
      Set<String> requiredProperties = new HashSet<>();
      for (MediaType mediaType : requestBody.getContent().values()) {
        if (
          nonNull(mediaType.getSchema()) &&
          nonNull(mediaType.getSchema().getRequired())
        ) {
          requiredProperties.addAll(mediaType.getSchema().getRequired());
        }
      }

      if (requiredProperties.isEmpty()) {
        logger.trace(
          "Operation '{}' has no defined required properties",
          operationKey
        );
        continue;
      }

      requestBodySchemasTotal.addAndGet(requiredProperties.size());

      if (
        !pathToTelemetryMap.containsKey(operationKey) ||
        pathToTelemetryMap.get(operationKey).isEmpty()
      ) {
        logger.trace(
          "No telemetry data for operation with request body: {}",
          operationKey
        );
        continue;
      }

      // TODO: Check if required properties are present in telemetry data
      // Note: This is a best-effort check as OpenTelemetry may not capture request body content
      // In a real implementation, you might need to rely on more sophisticated tracing or testing data
      logger.warn(
        "Request body schema coverage check is limited by available telemetry data for operation: {}",
        operationKey
      );
      // Consider this as not covered due to limitations in standard OpenTelemetry data
    }

    return calculatePercentage(
      requestBodySchemasCovered,
      requestBodySchemasTotal
    );
  }

  /**
   * Coverage criteria: At least one negative test exists for each endpoint that verifies error handling.
   */
  private ErrorResponseCoverage calculateErrorResponseCoverage(
    Map<String, Operation> operationsMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var errorResponsesTotal = new AtomicLong(0);
    var errorResponsesCovered = new AtomicLong(0);
    var atLeastOneTestExists = new AtomicBoolean(false);

    for (Map.Entry<String, Operation> entry : operationsMap.entrySet()) {
      String operationKey = entry.getKey();
      Operation operation = entry.getValue();

      if (isNull(operation.getResponses())) {
        continue;
      }

      var errorResponseCodes = operation
        .getResponses()
        .keySet()
        .stream()
        .filter(code -> code.startsWith("4") || code.startsWith("5"))
        .collect(toSet());

      if (errorResponseCodes.isEmpty()) {
        logger.trace(
          "Operation '{}' has no defined error responses",
          operationKey
        );
        continue;
      }

      errorResponsesTotal.addAndGet(errorResponseCodes.size());

      Set<String> observedErrorResponses = new HashSet<>();

      if (pathToTelemetryMap.containsKey(operationKey)) {
        for (OpenTelemetryData data : pathToTelemetryMap.get(operationKey)) {
          if (data.attributes().has(HTTP_RESPONSE_STATUS_CODE.getKey())) {
            String statusCode = data
              .attributes()
              .get(HTTP_RESPONSE_STATUS_CODE.getKey())
              .asText();
            if (statusCode.startsWith("4") || statusCode.startsWith("5")) {
              logger.trace(
                "Status code {} covered for operation '{}'",
                statusCode,
                operationKey
              );
              observedErrorResponses.add(statusCode);
            }
          }
        }
      }

      if (!observedErrorResponses.isEmpty()) {
        logger.trace(
          "Observed at least one status code for operation '{}'",
          operationKey
        );
        atLeastOneTestExists.set(true);
      }

      long coveredForOperation = observedErrorResponses
        .stream()
        .filter(errorResponseCodes::contains)
        .count();

      errorResponsesCovered.addAndGet(coveredForOperation);
    }

    return new ErrorResponseCoverage(
      calculatePercentage(errorResponsesCovered, errorResponsesTotal),
      atLeastOneTestExists.get()
    );
  }

  /**
   * Coverage criteria: Each supported content type is tested.
   */
  private BigDecimal calculateContentTypeCoverage(
    Map<String, Operation> operationsMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var contentTypesTotal = new AtomicLong(0);
    var contentTypesCovered = new AtomicLong(0);

    for (Map.Entry<String, Operation> entry : operationsMap.entrySet()) {
      String operationKey = entry.getKey();
      Operation operation = entry.getValue();

      Set<String> expectedRequestContentTypes = new HashSet<>();
      if (
        nonNull(operation.getRequestBody()) &&
        nonNull(operation.getRequestBody().getContent())
      ) {
        expectedRequestContentTypes.addAll(
          operation.getRequestBody().getContent().keySet()
        );
      }

      Set<String> expectedResponseContentTypes = new HashSet<>();
      if (nonNull(operation.getResponses())) {
        for (ApiResponse response : operation.getResponses().values()) {
          if (nonNull(response.getContent())) {
            expectedResponseContentTypes.addAll(response.getContent().keySet());
          }
        }
      }

      int operationContentTypesExpected =
        expectedRequestContentTypes.size() +
        expectedResponseContentTypes.size();
      contentTypesTotal.addAndGet(operationContentTypesExpected);

      if (operationContentTypesExpected == 0) {
        logger.trace(
          "Operation '{}' has no defined content types",
          operationKey
        );
        continue;
      }

      Set<String> observedRequestContentTypes = new HashSet<>();
      Set<String> observedResponseContentTypes = new HashSet<>();

      if (pathToTelemetryMap.containsKey(operationKey)) {
        for (OpenTelemetryData data : pathToTelemetryMap.get(operationKey)) {
          var requestContentTypeAttribute =
            HTTP_REQUEST_HEADER + "content-type";
          if (data.attributes().has(requestContentTypeAttribute)) {
            observedRequestContentTypes.add(
              data.attributes().get(requestContentTypeAttribute).asText()
            );
          }

          // Check response content type
          var responseContentTypeAttribute =
            HTTP_RESPONSE_HEADER + "content-type";
          if (data.attributes().has(responseContentTypeAttribute)) {
            observedResponseContentTypes.add(
              data.attributes().get(responseContentTypeAttribute).asText()
            );
          }
        }
      }

      int requestContentTypesCovered = 0;
      for (String contentType : expectedRequestContentTypes) {
        if (
          observedRequestContentTypes
            .stream()
            .anyMatch(observed -> observed.startsWith(contentType))
        ) {
          requestContentTypesCovered++;
        } else {
          logger.trace(
            "Request content type '{}' not covered in operation '{}'",
            contentType,
            operationKey
          );
        }
      }

      int responseContentTypesCovered = 0;
      for (String contentType : expectedResponseContentTypes) {
        if (
          observedResponseContentTypes
            .stream()
            .anyMatch(observed -> observed.startsWith(contentType))
        ) {
          responseContentTypesCovered++;
        } else {
          logger.trace(
            "Response content type '{}' not covered in operation '{}'",
            contentType,
            operationKey
          );
        }
      }

      contentTypesCovered.addAndGet(requestContentTypesCovered);
      contentTypesCovered.addAndGet(responseContentTypesCovered);
    }

    return calculatePercentage(contentTypesCovered, contentTypesTotal);
  }

  private record ErrorResponseCoverage(
    BigDecimal coverage,
    boolean atLeastOneTestExists
  ) {}
}
