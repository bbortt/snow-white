package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiCriterionDoesNotExistExceptionTest {

  @Test
  void constructorShouldCreateMessageFromLabel() {
    var fixture = new OpenApiCriterionDoesNotExistException("label");

    assertThat(fixture.getMessage()).isEqualTo(
      "OpenApi Criterion 'label' does not exist!"
    );
  }
}
