/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.TestResultForUnknownApiException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ApiInformationFilterTest {

  private ApiInformationFilter fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiInformationFilter();
  }

  @Nested
  class FindApiTestMatchingApiInformationInQualityGateReport {

    @Test
    void shouldReturnApiTest_whenMatchingApiFound() {
      var apiTest = ApiTest.builder()
        .serviceName("TestService")
        .apiName("TestApi")
        .apiVersion("v1")
        .build();

      var qualityGateReport = QualityGateReport.builder()
        .calculationId(UUID.fromString("0874a4d7-1baf-461e-bd20-c0b5a02d5b3b"))
        .apiTests(Set.of(apiTest))
        .reportParameter(mock(ReportParameter.class))
        .build();

      var apiInformation = ApiInformation.builder()
        .serviceName(apiTest.getServiceName())
        .apiName(apiTest.getApiName())
        .apiVersion(apiTest.getApiVersion())
        .build();

      ApiTest result =
        fixture.findApiTestMatchingApiInformationInQualityGateReport(
          qualityGateReport,
          apiInformation
        );

      assertThat(result).isEqualTo(apiTest);
    }

    @Test
    void shouldReturnApiTest_whenApiInformationDoesNotIncludeVersion() {
      var apiTest = ApiTest.builder()
        .serviceName("TestService")
        .apiName("TestApi")
        .build();

      var qualityGateReport = QualityGateReport.builder()
        .calculationId(UUID.fromString("139718a3-e63f-40a8-a8f9-e0e62921c038"))
        .apiTests(Set.of(apiTest))
        .reportParameter(mock(ReportParameter.class))
        .build();

      var apiInformation = ApiInformation.builder()
        .serviceName(apiTest.getServiceName())
        .apiName(apiTest.getApiName())
        .apiVersion("v2") // Version is not included in ApiInformation
        .build();

      ApiTest result =
        fixture.findApiTestMatchingApiInformationInQualityGateReport(
          qualityGateReport,
          apiInformation
        );

      assertThat(result).isEqualTo(apiTest);
    }

    public static Stream<
      ApiInformation
    > shouldThrowException_whenApiInformationDoesNotMatch() {
      return Stream.of(
        ApiInformation.builder()
          .serviceName("UnknownService")
          .apiName("TestApi")
          .apiVersion("v1")
          .build(),
        ApiInformation.builder()
          .serviceName("TestService")
          .apiName("UnknownApi")
          .apiVersion("v1")
          .build(),
        ApiInformation.builder()
          .serviceName("TestService")
          .apiName("TestApi")
          .build()
      );
    }

    @MethodSource
    @ParameterizedTest
    void shouldThrowException_whenApiInformationDoesNotMatch(
      ApiInformation apiInformation
    ) {
      var qualityGateReport = QualityGateReport.builder()
        .calculationId(UUID.fromString("df80260f-6f17-42cb-95ba-bf972016d868"))
        .apiTests(
          Set.of(
            ApiTest.builder()
              .serviceName("TestService")
              .apiName("TestApi")
              .apiVersion("v1")
              .build()
          )
        )
        .reportParameter(mock(ReportParameter.class))
        .build();

      assertThatThrownBy(() ->
        fixture.findApiTestMatchingApiInformationInQualityGateReport(
          qualityGateReport,
          apiInformation
        )
      ).isInstanceOf(TestResultForUnknownApiException.class);
    }

    @Test
    void shouldThrowException_whenQualityGateReportIsEmpty() {
      var qualityGateReport = QualityGateReport.builder()
        .calculationId(UUID.fromString("09995502-fb95-447b-a6c9-b36bdb1c078b"))
        .reportParameter(mock(ReportParameter.class))
        .build();

      var apiInformation = ApiInformation.builder()
        .serviceName("UnknownService")
        .apiName("UnknownApi")
        .apiVersion("v2")
        .build();

      assertThatThrownBy(() ->
        fixture.findApiTestMatchingApiInformationInQualityGateReport(
          qualityGateReport,
          apiInformation
        )
      ).isInstanceOf(TestResultForUnknownApiException.class);
    }
  }
}
