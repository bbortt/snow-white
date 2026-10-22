/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTestMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ReportParameterMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.QualityGateApi;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate400Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ApiIndexService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.WaiverValidator;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QualityGateResource implements QualityGateApi {

  private final ApiIndexService apiIndexService;
  private final ReportService reportService;

  private final ApiTestMapper apiTestMapper;
  private final ReportParameterMapper reportParameterMapper;
  private final QualityGateReportMapper qualityGateReportMapper;

  private final ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  private final WaiverValidator waiverValidator;

  /**
   * All named APIs are validated before anything is persisted, and every failure is reported at
   * once: a request with any unresolvable API creates no report and dispatches nothing.
   * <p>
   * The submitted waivers are judged on the far side of that resolution, because the question
   * "does this waiver name an API of this report" is asked of the versions the calculation will
   * judge, not of the ones the request wrote - a request may leave a version out and get the
   * indexed one. They are rejected the same way and to the same effect: one {@code 400} naming
   * every bad entry, and no report.
   */
  @Override
  @RealizesSw({
    SwTraceables.SW_013_CALCULATION_TRIGGER_IS_ALL_OR_NOTHING,
    SwTraceables.SW_040_WAIVER_ENTERS_ONLY_ON_THE_CALCULATION_REQUEST,
  })
  public ResponseEntity calculateQualityGate(
    String qualityGateConfigName,
    CalculateQualityGateRequest calculateQualityGateRequest
  ) {
    var apiTests = apiTestMapper.getApiTests(calculateQualityGateRequest);
    var reportParameter = reportParameterMapper.fromDto(
      calculateQualityGateRequest,
      randomUUID()
    );

    var apiInformationValidationResults =
      apiIndexService.fetchCompleteApiInformation(apiTests);

    if (
      apiInformationValidationResults
        .stream()
        .anyMatch(ApiIndexService.ValidationResult::isFailure)
    ) {
      return rejected(
        BAD_REQUEST,
        apiInformationValidationResults
          .stream()
          .map(validationResult ->
            validationResult instanceof
              ApiIndexService.ValidationResult.Failure(String errorMessage)
              ? errorMessage
              : null
          )
          .filter(Objects::nonNull)
          .collect(joining(lineSeparator()))
      );
    }

    var resolvedApiTests = apiInformationValidationResults
      .stream()
      .map(validationResult ->
        validationResult instanceof
          ApiIndexService.ValidationResult.Success(ApiTest apiTest)
          ? apiTest
          : null
      )
      .filter(Objects::nonNull)
      .collect(toSet());

    var waiverRejections = waiverValidator.validate(
      reportParameter.getWaivers(),
      resolvedApiTests
    );

    if (!waiverRejections.isEmpty()) {
      return rejected(
        BAD_REQUEST,
        String.join(lineSeparator(), waiverRejections)
      );
    }

    try {
      return initializeQualityGateCalculation(
        qualityGateConfigName,
        resolvedApiTests,
        reportParameter
      );
    } catch (QualityGateNotFoundException _) {
      return rejected(
        NOT_FOUND,
        format(
          "Quality-Gate configuration '%s' does not exist!",
          qualityGateConfigName
        )
      );
    }
  }

  /**
   * The one rejection body this endpoint answers with, whatever rejected the request: the generator
   * deduplicated the error shape, so the {@code 404} and both {@code 400}s share it.
   */
  private static ResponseEntity<CalculateQualityGate400Response> rejected(
    HttpStatus status,
    String message
  ) {
    return ResponseEntity.status(status).body(
      CalculateQualityGate400Response.builder()
        .code(status.getReasonPhrase())
        .message(message)
        .build()
    );
  }

  private @NonNull ResponseEntity<CalculateQualityGate202Response> initializeQualityGateCalculation(
    String qualityGateConfigName,
    Set<ApiTest> apiTests,
    ReportParameter reportParameter
  ) throws QualityGateNotFoundException {
    var report = reportService.initializeQualityGateCalculation(
      qualityGateConfigName,
      apiTests,
      reportParameter
    );

    logger.info(
      "Initialized quality-gate calculation: {}",
      report.getCalculationId()
    );

    return ResponseEntity.status(ACCEPTED)
      .header(
        LOCATION,
        format(
          "%s/quality-gate/%s",
          reportCoordinationServiceProperties.getPublicApiGatewayUrl(),
          report.getCalculationId()
        )
      )
      .body(qualityGateReportMapper.toDto(report));
  }
}
