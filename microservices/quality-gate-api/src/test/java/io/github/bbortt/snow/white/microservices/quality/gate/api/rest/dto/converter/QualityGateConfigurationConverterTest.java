package io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

class QualityGateConfigurationConverterTest {

  private static void verifyBasicAttributes(QualityGateConfiguration result) {
    assertThat(result)
      .isNotNull()
      .satisfies(
        r -> assertThat(r.getName()).isEqualTo("Test Quality Gate"),
        r -> assertThat(r.getDescription()).isEqualTo("Test Description")
      );
  }

  private QualityGateConfigurationConverter fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateConfigurationConverter();
  }

  @Test
  void isConverter() {
    assertThat(fixture).isInstanceOf(Converter.class);
  }

  @Nested
  class Convert {

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

      var result = fixture.convert(qualityGateConfiguration);

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

      var result = fixture.convert(qualityGateConfiguration);

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
