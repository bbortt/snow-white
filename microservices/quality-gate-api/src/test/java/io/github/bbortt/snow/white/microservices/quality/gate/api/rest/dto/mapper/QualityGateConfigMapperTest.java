package io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfigCriteria;
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
      var criteria = QualityGateConfigCriteria.builder()
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
        .criteria(criteria)
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
      var criteria = QualityGateConfigCriteria.builder()
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
        .criteria(criteria)
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

      assertNotNull(result.getCriteria());
      assertTrue(result.getCriteria().getPathCoverage());
      assertTrue(result.getCriteria().getResponseCodeCoverage());
      assertTrue(result.getCriteria().getRequiredParameterCoverage());
      assertTrue(result.getCriteria().getQueryParameterCoverage());
      assertTrue(result.getCriteria().getHeaderParameterCoverage());
      assertTrue(result.getCriteria().getRequestBodySchemaCoverage());
      assertTrue(result.getCriteria().getErrorResponseCoverage());
      assertTrue(result.getCriteria().getContentTypeCoverage());
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

      assertNotNull(result.getCriteria());
      assertFalse(result.getCriteria().getPathCoverage());
      assertFalse(result.getCriteria().getResponseCodeCoverage());
      assertFalse(result.getCriteria().getRequiredParameterCoverage());
      assertFalse(result.getCriteria().getQueryParameterCoverage());
      assertFalse(result.getCriteria().getHeaderParameterCoverage());
      assertFalse(result.getCriteria().getRequestBodySchemaCoverage());
      assertFalse(result.getCriteria().getErrorResponseCoverage());
      assertFalse(result.getCriteria().getContentTypeCoverage());
    }
  }
}
