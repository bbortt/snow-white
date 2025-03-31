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
  }

  @Nested
  class SetEnvironment {

    private static final Profiles PROD = Profiles.of("prod");

    @Mock
    private Environment environmentMock;

    public static Stream<
      String
    > shouldThrowIfInProdProfileAndUndefinedPublicUrl() {
      return Stream.of("", null, " ");
    }

    @Test
    void shouldNotThrowIfNotInProdProfile() {
      doReturn(false).when(environmentMock).acceptsProfiles(PROD);

      assertThatNoException()
        .isThrownBy(() -> fixture.setEnvironment(environmentMock));
    }

    @MethodSource
    @ParameterizedTest
    void shouldThrowIfInProdProfileAndUndefinedPublicUrl(String publicUrl) {
      doReturn(true).when(environmentMock).acceptsProfiles(PROD);

      assertThatThrownBy(() -> fixture.setEnvironment(environmentMock))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.api.gateway.public-url]."
        );
    }

    @Test
    void shouldNotThrowIfInProdProfileAndPublicUrlIsSet() {
      doReturn(true).when(environmentMock).acceptsProfiles(PROD);
      fixture.setPublicUrl("publicUrl");

      assertThatNoException()
        .isThrownBy(() -> fixture.setEnvironment(environmentMock));
    }
  }
}
