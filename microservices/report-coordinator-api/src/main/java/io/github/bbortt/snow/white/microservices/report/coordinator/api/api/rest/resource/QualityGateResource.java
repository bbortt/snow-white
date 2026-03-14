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
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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

  @Override
  public ResponseEntity calculateQualityGate(
    String qualityGateConfigName,
    CalculateQualityGateRequest calculateQualityGateRequest
  ) {
    var apiTests = apiTestMapper.getApiTests(calculateQualityGateRequest);
    var reportParameter = reportParameterMapper.fromDto(
      calculateQualityGateRequest,
      randomUUID()
    );

    var validationResults = apiIndexService.fetchCompleteApiInformation(
      apiTests
    );

    if (
      validationResults
        .stream()
        .anyMatch(ApiIndexService.ValidationResult::isFailure)
    ) {
      return ResponseEntity.status(BAD_REQUEST).body(
        CalculateQualityGate400Response.builder()
          .code(BAD_REQUEST.getReasonPhrase())
          .message(
            validationResults
              .stream()
              .map(validationResult ->
                validationResult instanceof
                  ApiIndexService.ValidationResult.Failure(String errorMessage)
                  ? errorMessage
                  : null
              )
              .filter(Objects::nonNull)
              .collect(joining(lineSeparator()))
          )
          .build()
      );
    }

    try {
      return initializeQualityGateCalculation(
        qualityGateConfigName,
        validationResults
          .stream()
          .map(validationResult ->
            validationResult instanceof
              ApiIndexService.ValidationResult.Success(ApiTest apiTest)
              ? apiTest
              : null
          )
          .filter(Objects::nonNull)
          .collect(toSet()),
        reportParameter
      );
    } catch (QualityGateNotFoundException _) {
      return ResponseEntity.status(NOT_FOUND).body(
        CalculateQualityGate400Response.builder()
          .code(NOT_FOUND.getReasonPhrase())
          .message(
            format(
              "Quality-Gate configuration '%s' does not exist!",
              qualityGateConfigName
            )
          )
          .build()
      );
    }
  }

  private @NonNull ResponseEntity<
    CalculateQualityGate202Response
  > initializeQualityGateCalculation(
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
