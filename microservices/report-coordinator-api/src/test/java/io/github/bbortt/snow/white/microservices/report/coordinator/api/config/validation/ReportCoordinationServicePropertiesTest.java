/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config.validation;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportCoordinationServicePropertiesTest {

  private ReportCoordinationServiceProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ReportCoordinationServiceProperties();
  }

  @Nested
  class AfterPropertiesSet {

    @Test
    void doesNotThrowAnythingIfPropertiesSet() {
      fixture.setCalculationRequestTopic("calculationRequestTopic");
      fixture
        .getOpenapiCalculationResponse()
        .setTopic("openapiCalculationResponseTopic");
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");
      fixture.setQualityGateApiUrl("qualityGateApiUrl");

      assertThatNoException().isThrownBy(() ->
        new ReportCoordinationServicePropertiesValidator(fixture)
      );
    }

    @Test
    void throwsExceptionWithMissingCalculationRequestTopic() {
      fixture
        .getOpenapiCalculationResponse()
        .setTopic("openapiCalculationResponseTopic");
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");
      fixture.setQualityGateApiUrl("qualityGateApiUrl");

      assertThatThrownBy(() ->
        new ReportCoordinationServicePropertiesValidator(fixture)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [snow.white.report.coordinator.api.calculation-request-topic]."
        );
    }

    @Test
    void throwsExceptionWithMissingOpenapiCalculationResponseTopic() {
      fixture.setCalculationRequestTopic("calculationRequestTopic");
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");
      fixture.setQualityGateApiUrl("qualityGateApiUrl");

      assertThatThrownBy(() ->
        new ReportCoordinationServicePropertiesValidator(fixture)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [snow.white.report.coordinator.api.openapi-calculation-response.topic]."
        );
    }

    @Test
    void throwsExceptionWithMissingPublicApiGatewayUrl() {
      fixture.setCalculationRequestTopic("calculationRequestTopic");
      fixture
        .getOpenapiCalculationResponse()
        .setTopic("openapiCalculationResponseTopic");
      fixture.setQualityGateApiUrl("qualityGateApiUrl");

      assertThatThrownBy(() ->
        new ReportCoordinationServicePropertiesValidator(fixture)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [snow.white.report.coordinator.api.public-api-gateway-url]."
        );
    }

    @Test
    void throwsExceptionWithMissingQualityGateApiUrl() {
      fixture.setCalculationRequestTopic("calculationRequestTopic");
      fixture
        .getOpenapiCalculationResponse()
        .setTopic("openapiCalculationResponseTopic");
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");

      assertThatThrownBy(() ->
        new ReportCoordinationServicePropertiesValidator(fixture)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [snow.white.report.coordinator.api.quality-gate-api-url]."
        );
    }
  }
}
