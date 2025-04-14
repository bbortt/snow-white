package io.github.bbortt.snow.white.commons.event.dto;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.math.BigDecimal.ONE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiCriterionResultTest {

  @Test
  void allArgsConstructor() {
    var additionalInformation = "additionalInformation";
    assertThat(
      new OpenApiCriterionResult(PATH_COVERAGE, ONE, additionalInformation)
    ).satisfies(
      r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
      r -> assertThat(r.coverage()).isEqualTo(ONE),
      r ->
        assertThat(r.additionalInformation()).isEqualTo(additionalInformation)
    );
  }

  @Test
  void requiredArgsConstructor() {
    assertThat(new OpenApiCriterionResult(PATH_COVERAGE, ONE)).satisfies(
      r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
      r -> assertThat(r.coverage()).isEqualTo(ONE),
      r -> assertThat(r.additionalInformation()).isNull()
    );
  }
}
