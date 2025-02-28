package io.github.bbortt.snow.white.microservices.quality.gate.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QualityGateConfigurationTest {

  @Test
  void builderWithDefaultValues() {
    assertThat(QualityGateConfiguration.builder().build()).satisfies(
      c -> assertThat(c.getIncludePathCoverage()).isTrue(),
      c -> assertThat(c.getIncludeResponseCodeCoverage()).isTrue(),
      c -> assertThat(c.getIncludeRequiredParameterCoverage()).isTrue(),
      c -> assertThat(c.getIncludeQueryParameterCoverage()).isFalse(),
      c -> assertThat(c.getIncludeHeaderParameterCoverage()).isFalse(),
      c -> assertThat(c.getIncludeRequestBodySchemaCoverage()).isTrue(),
      c -> assertThat(c.getIncludeErrorResponseCoverage()).isTrue(),
      c -> assertThat(c.getIncludeContentTypeCoverage()).isFalse()
    );
  }
}
