/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config.validation;

import io.github.bbortt.snow.white.microservices.api.gateway.config.ApiGatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@ExtendWith({ MockitoExtension.class })
class ApiGatewayPropertiesValidatorTest {

  private static final Profiles PROD = Profiles.of("prod");

  @Mock
  private Environment environmentMock;

  private ApiGatewayProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiGatewayProperties();
  }

  @Nested
  class AfterPropertiesSet {

    @Test
    void doesNotThrowAnythingIfPropertiesSet() {
      doReturn(false).when(environmentMock).acceptsProfiles(PROD);
      fixture.setEnvironment(environmentMock);

      fixture.setQualityGateApiUrl("qualityGateApiUrl");
      fixture.setReportCoordinatorApiUrl("reportCoordinationServiceUrl");

      assertThatNoException().isThrownBy(() -> new ApiGatewayPropertiesValidator(fixture, environmentMock));
    }

    @Test
    void shouldThrowException_withMissingQualityGateApiUrl() {
      fixture.setQualityGateApiUrl("qualityGateApiUrl");

      assertThatThrownBy(() -> new ApiGatewayPropertiesValidator(fixture, environmentMock))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All properties must be configured - missing: [snow.white.api.gateway.report-coordinator-api-url].");
    }

    @Test
    void shouldThrowException_withMissingReportCoordinationServiceUrl() {
      fixture.setReportCoordinatorApiUrl("reportCoordinationServiceUrl");

      assertThatThrownBy(() -> new ApiGatewayPropertiesValidator(fixture, environmentMock))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All properties must be configured - missing: [snow.white.api.gateway.quality-gate-api-url].");
    }

    public static Stream<String> throwsExceptionIfInProdProfileAndUndefinedPublicUrl() {
      return Stream.of("", null, " ");
    }

    @MethodSource
    @ParameterizedTest
    void throwsExceptionIfInProdProfileAndUndefinedPublicUrl(String publicUrl) {
      doReturn(true).when(environmentMock).acceptsProfiles(PROD);
      fixture.setEnvironment(environmentMock);

      fixture.setQualityGateApiUrl("qualityGateApiUrl");
      fixture.setReportCoordinatorApiUrl("reportCoordinationServiceUrl");

      assertThatThrownBy(() -> new ApiGatewayPropertiesValidator(fixture, environmentMock))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All properties must be configured - missing: [snow.white.api.gateway.public-url].");
    }

    @Test
    void doesNotThrowAnythingIfInProdProfileAndPublicUrlIsSet() {
      doReturn(true).when(environmentMock).acceptsProfiles(PROD);
      fixture.setEnvironment(environmentMock);

      fixture.setQualityGateApiUrl("qualityGateApiUrl");
      fixture.setReportCoordinatorApiUrl("reportCoordinationServiceUrl");

      fixture.setPublicUrl("publicUrl");

      assertThatNoException().isThrownBy(() -> new ApiGatewayPropertiesValidator(fixture, environmentMock));
    }
  }

  @Nested
  class SetEnvironment {

    @Test
    void setsEnvironment() {
      fixture.setEnvironment(environmentMock);

      assertThat(fixture.getEnvironment()).isEqualTo(environmentMock);
    }
  }
}
