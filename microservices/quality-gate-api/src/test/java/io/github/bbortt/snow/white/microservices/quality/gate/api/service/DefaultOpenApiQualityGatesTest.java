package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class DefaultOpenApiQualityGatesTest {

  @Mock
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepositoryMock;

  private DefaultOpenApiQualityGates fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new DefaultOpenApiQualityGates(
      openApiCoverageConfigurationRepositoryMock
    );
  }

  @Nested
  class GetDefaultOpenApiCoverageConfigurationsTest {

    @Test
    void shouldContainFourQualityGateDefinitions() {
      doAnswer(invocation ->
        Optional.of(
          OpenApiCoverageConfiguration.builder()
            .name(invocation.getArgument(0))
            .build()
        )
      )
        .when(openApiCoverageConfigurationRepositoryMock)
        .findByName(anyString());

      assertThat(fixture.getDefaultOpenApiCoverageConfigurations())
        .allSatisfy(qualityGateConfiguration ->
          assertThat(qualityGateConfiguration.getIsPredefined()).isTrue()
        )
        .satisfiesExactly(
          // basic-coverage
          qualityGateConfiguration ->
            assertThat(qualityGateConfiguration).satisfies(
              configuration ->
                assertThat(
                  configuration.getOpenApiCoverageConfigurations()
                ).hasSize(6),
              configuration ->
                assertThat(configuration.getMinCoveragePercentage()).isEqualTo(
                  80
                )
            ),
          // full-feature
          qualityGateConfiguration ->
            assertThat(qualityGateConfiguration).satisfies(
              configuration ->
                assertThat(
                  configuration.getOpenApiCoverageConfigurations()
                ).hasSize(OpenApiCriteria.values().length),
              configuration ->
                assertThat(configuration.getMinCoveragePercentage()).isEqualTo(
                  100
                )
            ),
          // minimal
          qualityGateConfiguration ->
            assertThat(qualityGateConfiguration).satisfies(
              configuration ->
                assertThat(
                  configuration.getOpenApiCoverageConfigurations()
                ).satisfiesExactly(openApiConfiguration ->
                  assertThat(
                    openApiConfiguration
                      .getOpenApiCoverageConfiguration()
                      .getName()
                  ).isEqualTo(PATH_COVERAGE.name())
                ),
              configuration ->
                assertThat(configuration.getMinCoveragePercentage()).isEqualTo(
                  80
                )
            ),
          // dry-run
          qualityGateConfiguration ->
            assertThat(
              qualityGateConfiguration.getOpenApiCoverageConfigurations()
            ).isEmpty()
        );
    }

    @Test
    void shouldResultInExceptionWhenOpenApiConfigurationsDoNotExist() {
      assertThatThrownBy(() ->
        fixture.getDefaultOpenApiCoverageConfigurations()
      ).isInstanceOf(IllegalStateException.class);

      verify(openApiCoverageConfigurationRepositoryMock).findByName(
        anyString()
      );
    }
  }
}
