package io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.OpenApiCoverage;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

class QualityGateConfigConverterTest {

  private static void verifyBasicAttributes(QualityGateConfiguration result) {
    assertThat(result)
      .isNotNull()
      .satisfies(
        r -> assertThat(r.getName()).isEqualTo("Test Quality Gate"),
        r -> assertThat(r.getDescription()).isEqualTo("Test Description")
      );
  }

  private QualityGateConfigConverter fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateConfigConverter();
  }

  @Test
  void isConverter() {
    assertThat(fixture).isInstanceOf(Converter.class);
  }

  @Nested
  class Convert {

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

      QualityGateConfiguration result = fixture.convert(qualityGateConfig);

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

      var result = fixture.convert(qualityGateConfig);

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
}
