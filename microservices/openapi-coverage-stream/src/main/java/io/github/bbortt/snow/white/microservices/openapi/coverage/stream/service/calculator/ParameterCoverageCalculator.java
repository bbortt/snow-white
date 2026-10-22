/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getTelemetryForTemplate;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.toEvidence;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toMethod;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toPath;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toParameterPointer;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.event.dto.FindingStatus;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Each parameter (in path, query) has been tested with valid values.
 *
 * @see OpenApiCoverageCriteria#PARAMETER_COVERAGE
 */
@Slf4j
@Component
public class ParameterCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return PARAMETER_COVERAGE;
  }

  /**
   * One finding per parameter the operation declares — including the parameters this criterion
   * does not judge, which are recorded {@code NOT_APPLICABLE} rather than dropped.
   */
  @RealizesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
  @Override
  protected @NonNull List<ApiTestFinding> calculateFindings(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    // Resolved once per operation, not once per parameter — the pattern match behind
    // getTelemetryForTemplate is the same for every parameter of a given operation.
    var telemetryByOperationKey = new HashMap<
      String,
      List<OpenTelemetryData>
    >();

    return toParameterTargets(pathToOpenAPIOperationMap)
      .stream()
      .map(target ->
        toFinding(
          target,
          telemetryByOperationKey.computeIfAbsent(
            target.operationKey(),
            operationKey ->
              getTelemetryForTemplate(pathToTelemetryMap, operationKey)
          )
        )
      )
      .toList();
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  ) {
    return getAdditionalInformationOrNull(
      "The following parameters are uncovered: `%s`",
      calculation
    );
  }

  /**
   * The parameter's location belongs in the message but is not one of the finding's
   * discriminators, so the label is built by re-walking the same enumeration the findings came
   * from and matching each target on the pointer it produced — never by parsing a pointer back
   * apart.
   */
  protected @Nullable String getAdditionalInformationOrNull(
    String infoMessagePattern,
    @NonNull Calculation calculation
  ) {
    var uncoveredPointers = calculation
      .findings()
      .stream()
      .filter(finding -> UNCOVERED.equals(finding.status()))
      .map(ApiTestFinding::specPointer)
      .collect(toSet());

    if (uncoveredPointers.isEmpty()) {
      return null;
    }

    var uncoveredParameters = toParameterTargets(
      calculation.pathToOpenAPIOperationMap()
    )
      .stream()
      .filter(target -> uncoveredPointers.contains(target.specPointer()))
      .map(ParameterCoverageCalculator::toUncoveredParameterLabel)
      .distinct()
      .sorted()
      .toList();

    return format(infoMessagePattern, join("`, `", uncoveredParameters));
  }

  /**
   * Every parameter of every operation, in document order, each carrying the operation it belongs
   * to and the position that names it. OpenAPI keys parameters by position rather than by name,
   * so the index is what the pointer is built from.
   */
  private static @NonNull List<ParameterTarget> toParameterTargets(
    @NonNull Map<String, Operation> pathToOpenAPIOperationMap
  ) {
    var targets = new ArrayList<ParameterTarget>();

    pathToOpenAPIOperationMap
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .forEach(entry -> {
        var parameters = entry.getValue().getParameters();

        if (isEmpty(parameters)) {
          logger.trace(
            "Operation '{}' has no defined parameters",
            entry.getKey()
          );
          return;
        }

        for (var index = 0; index < parameters.size(); index++) {
          targets.add(
            new ParameterTarget(entry.getKey(), index, parameters.get(index))
          );
        }
      });

    return targets;
  }

  private @NonNull ApiTestFinding toFinding(
    @NonNull ParameterTarget target,
    @NonNull List<OpenTelemetryData> telemetryDataList
  ) {
    var parameter = target.parameter();
    var isJudged = judgesParameter(parameter);

    List<FindingEvidence> evidence = isJudged
      ? toEvidence(
          getSatisfyingTelemetry(
            telemetryDataList,
            parameter,
            target.operationKey()
          )
        )
      : emptyList();

    FindingStatus status;
    if (!isJudged) {
      status = NOT_APPLICABLE;
    } else if (evidence.isEmpty()) {
      status = UNCOVERED;
    } else {
      status = COVERED;
    }

    logger.trace(
      "Parameter '{}' ({}) is {} in operation '{}'",
      parameter.getName(),
      parameter.getIn(),
      status,
      target.operationKey()
    );

    return ApiTestFinding.builder()
      .specPointer(target.specPointer())
      .status(status)
      .httpPath(toPath(target.operationKey()))
      .httpMethod(toMethod(target.operationKey()))
      .parameterName(parameter.getName())
      .evidence(evidence)
      .build();
  }

  /**
   * Whether this criterion has anything to say about the given parameter.
   * This criterion judges every declared parameter; the narrower subsets are drawn by the
   * subclasses, which is why the parameter stays part of the signature even though this base
   * implementation ignores it.
   */
  @SuppressWarnings("java:S1172")
  protected boolean judgesParameter(@NonNull Parameter parameter) {
    return true;
  }

  /**
   * The spans that exercised this parameter, rather than the first of them reduced to a boolean.
   */
  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  private @NonNull List<OpenTelemetryData> getSatisfyingTelemetry(
    @NonNull List<OpenTelemetryData> telemetryDataList,
    @NonNull Parameter parameter,
    @NonNull String operationKey
  ) {
    return telemetryDataList
      .stream()
      .filter(telemetryData ->
        isParameterPresent(
          telemetryData,
          parameter.getName(),
          parameter.getIn(),
          operationKey
        )
      )
      .toList();
  }

  private boolean isParameterPresent(
    OpenTelemetryData data,
    String paramName,
    @Nullable String paramIn,
    String operationKey
  ) {
    if (isNull(data.attributes()) || isNull(paramIn)) {
      return false;
    }

    return switch (paramIn) {
      case "query" -> isQueryParameterPresent(data, paramName);
      case "path" -> isPathParameterPresent(operationKey, paramName);
      case "header" -> isHeaderParameterPresent(data, paramName);
      default -> {
        logger.trace(
          "Unsupported parameter location '{}' for parameter '{}'",
          paramIn,
          paramName
        );
        yield false;
      }
    };
  }

  @RealizesSw(
    SwTraceables.SW_004_PARAMETER_COVERAGE_MATCHES_BY_TOKEN_NOT_SUBSTRING
  )
  private boolean isQueryParameterPresent(
    OpenTelemetryData data,
    String paramName
  ) {
    if (!data.attributes().has(URL_QUERY.getKey())) {
      return false;
    }

    String queryString = data.attributes().get(URL_QUERY.getKey()).asString();
    if (isNull(queryString) || queryString.isEmpty()) {
      return false;
    }

    return stream(queryString.split("&")).anyMatch(token ->
      queryTokenName(token).equals(paramName)
    );
  }

  /**
   * A query token is either a bare flag or a {@code name=value} pair; the parameter's name is
   * only ever the part before the first {@code =}, never a substring of a different token's name
   * or value.
   */
  private static String queryTokenName(String token) {
    var separatorIndex = token.indexOf('=');
    return separatorIndex < 0 ? token : token.substring(0, separatorIndex);
  }

  /**
   * Path parameters are implicitly covered if the telemetry data exists for the operation,
   * as the path template was matched.
   */
  private boolean isPathParameterPresent(
    String operationKey,
    String paramName
  ) {
    String path = OperationKeyCalculator.toPath(operationKey);
    return path.contains("{" + paramName + "}");
  }

  private boolean isHeaderParameterPresent(
    OpenTelemetryData data,
    String paramName
  ) {
    String headerKey = "http.request.header." + paramName.toLowerCase(ROOT);
    return data.attributes().has(headerKey);
  }

  private static String toUncoveredParameterLabel(ParameterTarget target) {
    return format(
      "%s [%s: %s]",
      target.operationKey(),
      target.parameter().getIn(),
      target.parameter().getName()
    );
  }

  /**
   * A declared parameter together with what names it: the operation it belongs to and its
   * position in that operation's parameter array.
   */
  private record ParameterTarget(
    String operationKey,
    int index,
    Parameter parameter
  ) {
    String specPointer() {
      return toParameterPointer(operationKey, index);
    }
  }
}
