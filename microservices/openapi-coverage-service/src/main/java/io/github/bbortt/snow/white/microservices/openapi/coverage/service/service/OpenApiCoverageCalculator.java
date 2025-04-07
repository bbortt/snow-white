package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import java.util.Map;

public interface OpenApiCoverageCalculator {
  boolean accepts(OpenApiCriteria openApiCriteria);

  OpenApiCriterionResult calculate(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  );
}
