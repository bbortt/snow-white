/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@ExtendWith({ MockitoExtension.class })
class ApiGatewayPropertiesTest {

  private static final Profiles PROD = Profiles.of("prod");

  @Mock
  private Environment environmentMock;

  private ApiGatewayProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiGatewayProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Test
  void isEnvironmentAware() {
    assertThat(fixture).isInstanceOf(EnvironmentAware.class);
  }

  @Nested
  class AfterPropertiesSet {

    @Test
    void doesNotThrowAnythingIfPropertiesSet() {
      doReturn(false).when(environmentMock).acceptsProfiles(PROD);
      fixture.setEnvironment(environmentMock);

      fixture.setQualityGateApiUrl("qualityGateApiUrl");
      fixture.setReportCoordinationServiceUrl("reportCoordinationServiceUrl");

      assertThatNoException().isThrownBy(() -> fixture.afterPropertiesSet());
    }

    @Test
    void throwsExceptionWithMissingQualityGateApiUrl() {
      fixture.setQualityGateApiUrl("qualityGateApiUrl");

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.api.gateway.report-coordination-service-url]."
        );
    }

    @Test
    void throwsExceptionWithMissingReportCoordinationServiceUrl() {
      fixture.setReportCoordinationServiceUrl("reportCoordinationServiceUrl");

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.api.gateway.quality-gate-api-url]."
        );
    }

    public static Stream<
      String
    > throwsExceptionIfInProdProfileAndUndefinedPublicUrl() {
      return Stream.of("", null, " ");
    }

    @MethodSource
    @ParameterizedTest
    void throwsExceptionIfInProdProfileAndUndefinedPublicUrl(String publicUrl) {
      doReturn(true).when(environmentMock).acceptsProfiles(PROD);
      fixture.setEnvironment(environmentMock);

      fixture.setQualityGateApiUrl("qualityGateApiUrl");
      fixture.setReportCoordinationServiceUrl("reportCoordinationServiceUrl");

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.api.gateway.public-url]."
        );
    }

    @Test
    void doesNotThrowAnythingIfInProdProfileAndPublicUrlIsSet() {
      doReturn(true).when(environmentMock).acceptsProfiles(PROD);
      fixture.setEnvironment(environmentMock);

      fixture.setQualityGateApiUrl("qualityGateApiUrl");
      fixture.setReportCoordinationServiceUrl("reportCoordinationServiceUrl");

      fixture.setPublicUrl("publicUrl");

      assertThatNoException().isThrownBy(() -> fixture.afterPropertiesSet());
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
