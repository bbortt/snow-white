/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.TestResultForUnknownApiException;
import java.util.Set;
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
        .apiTests(Set.of(apiTest))
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
        .apiTests(Set.of(apiTest))
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
        .apiTests(
          Set.of(
            ApiTest.builder()
              .serviceName("TestService")
              .apiName("TestApi")
              .apiVersion("v1")
              .build()
          )
        )
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
      var qualityGateReport = QualityGateReport.builder().build();

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
