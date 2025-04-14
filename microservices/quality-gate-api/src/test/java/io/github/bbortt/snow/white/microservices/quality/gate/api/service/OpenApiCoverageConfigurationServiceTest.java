package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageConfigurationServiceTest {

  @Mock
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepositoryMock;

  private OpenApiCoverageConfigurationService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiCoverageConfigurationService(
      openApiCoverageConfigurationRepositoryMock
    );
  }

  @Nested
  class GetAllOpenapiCoverageConfigurations {

    @Test
    void shouldReturnOpenApiCoverageConfigurations() {
      var openApiCoverageConfiguration = new OpenApiCoverageConfiguration();
      doReturn(singletonList(openApiCoverageConfiguration))
        .when(openApiCoverageConfigurationRepositoryMock)
        .findAll();

      Set<OpenApiCoverageConfiguration> result =
        fixture.getAllOpenapiCoverageConfigurations();

      assertThat(result).containsExactly(openApiCoverageConfiguration);
    }
  }

  @Nested
  class InitOpenApiCriteria {

    @Test
    void shouldAddEachOpenApiCriteriaToDatabase() {
      fixture.initOpenApiCriteria();

      ArgumentCaptor<
        OpenApiCoverageConfiguration
      > openApiCoverageConfigurationArgumentCaptor = captor();
      verify(
        openApiCoverageConfigurationRepositoryMock,
        times(OpenApiCriteria.values().length)
      ).save(openApiCoverageConfigurationArgumentCaptor.capture());

      assertThat(openApiCoverageConfigurationArgumentCaptor.getAllValues())
        .isNotEmpty()
        .map(OpenApiCoverageConfiguration::getName)
        .containsExactlyInAnyOrder(
          stream(OpenApiCriteria.values())
            .map(OpenApiCriteria::name)
            .toArray(String[]::new)
        );
    }
  }
}
