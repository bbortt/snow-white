package io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.OpenApiCoverage;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QualityGateConfigMapperTest {

  private static void verifyBasicAttributes(QualityGateConfiguration result) {
    assertThat(result)
      .isNotNull()
      .satisfies(
        r -> assertThat(r.getName()).isEqualTo("Test Quality Gate"),
        r -> assertThat(r.getDescription()).isEqualTo("Test Description")
      );
  }

  @Nested
  class ToEntity {

    @Test
    void allCriteriaEnabled() {
      var openApiCoverage = OpenApiCoverage.builder()
        .pathCoverage(true)
        .responseCodeCoverage(true)
        .requiredParameterCoverage(true)
        .queryParameterCoverage(true)
        .headerParameterCoverage(true)
        .requestBodySchemaCoverage(true)
        .errorResponseCoverage(true)
        .contentTypeCoverage(true)
        .build();

      var qualityGateConfig = QualityGateConfig.builder()
        .name("Test Quality Gate")
        .description("Test Description")
        .openApiCoverage(openApiCoverage)
        .build();

      QualityGateConfiguration result = QualityGateConfigMapper.toEntity(
        qualityGateConfig
      );

      verifyBasicAttributes(result);

      assertTrue(result.getIncludePathCoverage());
      assertTrue(result.getIncludeResponseCodeCoverage());
      assertTrue(result.getIncludeRequiredParameterCoverage());
      assertTrue(result.getIncludeQueryParameterCoverage());
      assertTrue(result.getIncludeHeaderParameterCoverage());
      assertTrue(result.getIncludeRequestBodySchemaCoverage());
      assertTrue(result.getIncludeErrorResponseCoverage());
      assertTrue(result.getIncludeContentTypeCoverage());
    }

    @Test
    void allCriteriaDisabled() {
      var openApiCoverage = OpenApiCoverage.builder()
        .pathCoverage(false)
        .responseCodeCoverage(false)
        .requiredParameterCoverage(false)
        .queryParameterCoverage(false)
        .headerParameterCoverage(false)
        .requestBodySchemaCoverage(false)
        .errorResponseCoverage(false)
        .contentTypeCoverage(false)
        .build();

      var qualityGateConfig = QualityGateConfig.builder()
        .name("Test Quality Gate")
        .description("Test Description")
        .openApiCoverage(openApiCoverage)
        .build();

      var result = QualityGateConfigMapper.toEntity(qualityGateConfig);

      verifyBasicAttributes(result);

      assertFalse(result.getIncludePathCoverage());
      assertFalse(result.getIncludeResponseCodeCoverage());
      assertFalse(result.getIncludeRequiredParameterCoverage());
      assertFalse(result.getIncludeQueryParameterCoverage());
      assertFalse(result.getIncludeHeaderParameterCoverage());
      assertFalse(result.getIncludeRequestBodySchemaCoverage());
      assertFalse(result.getIncludeErrorResponseCoverage());
      assertFalse(result.getIncludeContentTypeCoverage());
    }
  }

  @Nested
  class ToDto {

    @Test
    void allCriteriaEnabled() {
      var qualityGateConfiguration = QualityGateConfiguration.builder()
        .name("Test Quality Gate")
        .description("Test Description")
        .includePathCoverage(true)
        .includeResponseCodeCoverage(true)
        .includeRequiredParameterCoverage(true)
        .includeQueryParameterCoverage(true)
        .includeHeaderParameterCoverage(true)
        .includeRequestBodySchemaCoverage(true)
        .includeErrorResponseCoverage(true)
        .includeContentTypeCoverage(true)
        .build();

      var result = QualityGateConfigMapper.toDto(qualityGateConfiguration);

      assertThat(result)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getName()).isEqualTo("Test Quality Gate"),
          r -> assertThat(r.getDescription()).isEqualTo("Test Description")
        );

      assertThat(result.getOpenApiCoverage())
        .isNotNull()
        .satisfies(
          c -> assertThat(c.getPathCoverage()).isTrue(),
          c -> assertThat(c.getResponseCodeCoverage()).isTrue(),
          c -> assertThat(c.getRequiredParameterCoverage()).isTrue(),
          c -> assertThat(c.getQueryParameterCoverage()).isTrue(),
          c -> assertThat(c.getHeaderParameterCoverage()).isTrue(),
          c -> assertThat(c.getRequestBodySchemaCoverage()).isTrue(),
          c -> assertThat(c.getErrorResponseCoverage()).isTrue(),
          c -> assertThat(c.getContentTypeCoverage()).isTrue()
        );
    }

    @Test
    void allCriteriaDisabled() {
      var qualityGateConfiguration = QualityGateConfiguration.builder()
        .name("Test Quality Gate")
        .description("Test Description")
        .includePathCoverage(false)
        .includeResponseCodeCoverage(false)
        .includeRequiredParameterCoverage(false)
        .includeQueryParameterCoverage(false)
        .includeHeaderParameterCoverage(false)
        .includeRequestBodySchemaCoverage(false)
        .includeErrorResponseCoverage(false)
        .includeContentTypeCoverage(false)
        .build();

      var result = QualityGateConfigMapper.toDto(qualityGateConfiguration);

      assertThat(result)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getName()).isEqualTo("Test Quality Gate"),
          r -> assertThat(r.getDescription()).isEqualTo("Test Description")
        );

      assertThat(result.getOpenApiCoverage())
        .isNotNull()
        .satisfies(
          c -> assertThat(c.getPathCoverage()).isFalse(),
          c -> assertThat(c.getResponseCodeCoverage()).isFalse(),
          c -> assertThat(c.getRequiredParameterCoverage()).isFalse(),
          c -> assertThat(c.getQueryParameterCoverage()).isFalse(),
          c -> assertThat(c.getHeaderParameterCoverage()).isFalse(),
          c -> assertThat(c.getRequestBodySchemaCoverage()).isFalse(),
          c -> assertThat(c.getErrorResponseCoverage()).isFalse(),
          c -> assertThat(c.getContentTypeCoverage()).isFalse()
        );
    }
  }
}
