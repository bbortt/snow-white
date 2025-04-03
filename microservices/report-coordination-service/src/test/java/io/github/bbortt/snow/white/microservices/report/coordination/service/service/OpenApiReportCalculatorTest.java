package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiCoverageReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.OpenApiCoverageConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiReportCalculatorTest {

  @Mock
  private OpenApiCoverageResponseEvent mockResponseEvent;

  @Mock
  private OpenApiCoverageConfig openApiCoverageConfigMock;

  private OpenApiReportCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiReportCalculator(
      mockResponseEvent,
      openApiCoverageConfigMock
    );
  }

  @Nested
  class Calculate {

    @Test
    void shouldSetAllValuesFromResponseEvent() {
      var pathCoverage = new BigDecimal("0.8");
      var responseCodeCoverage = new BigDecimal("0.9");
      var errorResponseCoverage = new BigDecimal("0.7");
      var requiredParameterCoverage = new BigDecimal("0.85");
      var headerParameterCoverage = new BigDecimal("0.95");
      var queryParameterCoverage = new BigDecimal("0.75");
      var requestBodySchemaCoverage = new BigDecimal("0.65");
      var contentTypeCoverage = new BigDecimal("0.55");

      doReturn(pathCoverage).when(mockResponseEvent).getPathCoverage();
      doReturn(responseCodeCoverage)
        .when(mockResponseEvent)
        .getResponseCodeCoverage();
      doReturn(errorResponseCoverage)
        .when(mockResponseEvent)
        .getErrorResponseCoverage();
      doReturn(requiredParameterCoverage)
        .when(mockResponseEvent)
        .getRequiredParameterCoverage();
      doReturn(headerParameterCoverage)
        .when(mockResponseEvent)
        .getHeaderParameterCoverage();
      doReturn(queryParameterCoverage)
        .when(mockResponseEvent)
        .getQueryParameterCoverage();
      doReturn(requestBodySchemaCoverage)
        .when(mockResponseEvent)
        .getRequestBodySchemaCoverage();
      doReturn(contentTypeCoverage)
        .when(mockResponseEvent)
        .getContentTypeCoverage();

      doReturn(true).when(openApiCoverageConfigMock).getIncludesPathCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesResponseCodeCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesErrorResponseCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesRequiredParameterCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesHeaderParameterCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesQueryParameterCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesRequestBodySchemaCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesContentTypeCoverage();

      OpenApiCoverageReport report = fixture.calculate();

      assertThat(report.getPathCoverage()).isEqualTo(pathCoverage);
      assertThat(report.getResponseCodeCoverage()).isEqualTo(
        responseCodeCoverage
      );
      assertThat(report.getErrorResponseCoverage()).isEqualTo(
        errorResponseCoverage
      );
      assertThat(report.getRequiredParameterCoverage()).isEqualTo(
        requiredParameterCoverage
      );
      assertThat(report.getHeaderParameterCoverage()).isEqualTo(
        headerParameterCoverage
      );
      assertThat(report.getQueryParameterCoverage()).isEqualTo(
        queryParameterCoverage
      );
      assertThat(report.getRequestBodySchemaCoverage()).isEqualTo(
        requestBodySchemaCoverage
      );
      assertThat(report.getContentTypeCoverage()).isEqualTo(
        contentTypeCoverage
      );

      // All coverage metrics are below 1.0 and all are included in config
      assertThat(report.getPathCoverageMet()).isFalse();
      assertThat(report.getResponseCodeCoverageMet()).isFalse();
      assertThat(report.getErrorResponseCoverageMet()).isFalse();
      assertThat(report.getRequiredParameterCoverageMet()).isFalse();
      assertThat(report.getHeaderParameterCoverageMet()).isFalse();
      assertThat(report.getQueryParameterCoverageMet()).isFalse();
      assertThat(report.getRequestBodySchemaCoverageMet()).isFalse();
      assertThat(report.getContentTypeCoverageMet()).isFalse();
    }

    @Test
    void shouldMarkCoverageAsMetWhenConfigExcludesCoverage() {
      var partialCoverage = new BigDecimal("0.8");

      doReturn(partialCoverage).when(mockResponseEvent).getPathCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getResponseCodeCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getErrorResponseCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getRequiredParameterCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getHeaderParameterCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getQueryParameterCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getRequestBodySchemaCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getContentTypeCoverage();

      doReturn(false).when(openApiCoverageConfigMock).getIncludesPathCoverage();
      doReturn(false)
        .when(openApiCoverageConfigMock)
        .getIncludesResponseCodeCoverage();
      doReturn(false)
        .when(openApiCoverageConfigMock)
        .getIncludesErrorResponseCoverage();
      doReturn(false)
        .when(openApiCoverageConfigMock)
        .getIncludesRequiredParameterCoverage();
      doReturn(false)
        .when(openApiCoverageConfigMock)
        .getIncludesHeaderParameterCoverage();
      doReturn(false)
        .when(openApiCoverageConfigMock)
        .getIncludesQueryParameterCoverage();
      doReturn(false)
        .when(openApiCoverageConfigMock)
        .getIncludesRequestBodySchemaCoverage();
      doReturn(false)
        .when(openApiCoverageConfigMock)
        .getIncludesContentTypeCoverage();

      OpenApiCoverageReport report = fixture.calculate();

      // All coverage metrics should be marked as met because they are excluded in config
      assertThat(report.getPathCoverageMet()).isTrue();
      assertThat(report.getResponseCodeCoverageMet()).isTrue();
      assertThat(report.getErrorResponseCoverageMet()).isTrue();
      assertThat(report.getRequiredParameterCoverageMet()).isTrue();
      assertThat(report.getHeaderParameterCoverageMet()).isTrue();
      assertThat(report.getQueryParameterCoverageMet()).isTrue();
      assertThat(report.getRequestBodySchemaCoverageMet()).isTrue();
      assertThat(report.getContentTypeCoverageMet()).isTrue();
    }

    @Test
    void shouldMarkCoverageAsMetWhenCoverageIsComplete() {
      var fullCoverage = BigDecimal.ONE;

      doReturn(fullCoverage).when(mockResponseEvent).getPathCoverage();
      doReturn(fullCoverage).when(mockResponseEvent).getResponseCodeCoverage();
      doReturn(fullCoverage).when(mockResponseEvent).getErrorResponseCoverage();
      doReturn(fullCoverage)
        .when(mockResponseEvent)
        .getRequiredParameterCoverage();
      doReturn(fullCoverage)
        .when(mockResponseEvent)
        .getHeaderParameterCoverage();
      doReturn(fullCoverage)
        .when(mockResponseEvent)
        .getQueryParameterCoverage();
      doReturn(fullCoverage)
        .when(mockResponseEvent)
        .getRequestBodySchemaCoverage();
      doReturn(fullCoverage).when(mockResponseEvent).getContentTypeCoverage();

      doReturn(true).when(openApiCoverageConfigMock).getIncludesPathCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesResponseCodeCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesErrorResponseCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesRequiredParameterCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesHeaderParameterCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesQueryParameterCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesRequestBodySchemaCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesContentTypeCoverage();

      OpenApiCoverageReport report = fixture.calculate();

      // All coverage metrics should be marked as met because coverage is 100%
      assertThat(report.getPathCoverageMet()).isTrue();
      assertThat(report.getResponseCodeCoverageMet()).isTrue();
      assertThat(report.getErrorResponseCoverageMet()).isTrue();
      assertThat(report.getRequiredParameterCoverageMet()).isTrue();
      assertThat(report.getHeaderParameterCoverageMet()).isTrue();
      assertThat(report.getQueryParameterCoverageMet()).isTrue();
      assertThat(report.getRequestBodySchemaCoverageMet()).isTrue();
      assertThat(report.getContentTypeCoverageMet()).isTrue();
    }

    @Test
    void shouldHandleMixedCoverageStates() {
      var fullCoverage = BigDecimal.ONE;
      var partialCoverage = new BigDecimal("0.8");

      doReturn(fullCoverage).when(mockResponseEvent).getPathCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getResponseCodeCoverage();
      doReturn(fullCoverage).when(mockResponseEvent).getErrorResponseCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getRequiredParameterCoverage();
      doReturn(fullCoverage)
        .when(mockResponseEvent)
        .getHeaderParameterCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getQueryParameterCoverage();
      doReturn(fullCoverage)
        .when(mockResponseEvent)
        .getRequestBodySchemaCoverage();
      doReturn(partialCoverage)
        .when(mockResponseEvent)
        .getContentTypeCoverage();

      doReturn(true).when(openApiCoverageConfigMock).getIncludesPathCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesResponseCodeCoverage();
      doReturn(false)
        .when(openApiCoverageConfigMock)
        .getIncludesErrorResponseCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesRequiredParameterCoverage();
      doReturn(false)
        .when(openApiCoverageConfigMock)
        .getIncludesHeaderParameterCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesQueryParameterCoverage();
      doReturn(false)
        .when(openApiCoverageConfigMock)
        .getIncludesRequestBodySchemaCoverage();
      doReturn(true)
        .when(openApiCoverageConfigMock)
        .getIncludesContentTypeCoverage();

      OpenApiCoverageReport report = fixture.calculate();

      // Some are true, some are not
      assertThat(report.getPathCoverageMet()).isTrue(); // full coverage and included in config
      assertThat(report.getResponseCodeCoverageMet()).isFalse(); // partial coverage and included in config
      assertThat(report.getErrorResponseCoverageMet()).isTrue(); // full coverage, but would be true even if partial because excluded
      assertThat(report.getRequiredParameterCoverageMet()).isFalse(); // partial coverage and included in config
      assertThat(report.getHeaderParameterCoverageMet()).isTrue(); // full coverage, but would be true even if partial because excluded
      assertThat(report.getQueryParameterCoverageMet()).isFalse(); // partial coverage and included in config
      assertThat(report.getRequestBodySchemaCoverageMet()).isTrue(); // full coverage, but would be true even if partial because excluded
      assertThat(report.getContentTypeCoverageMet()).isFalse(); // partial coverage and included in config
    }
  }
}
