/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.math.BigDecimal.ONE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OpenApiCriterionResultTest {

  @Test
  void allArgsConstructor() {
    var duration = Duration.ofSeconds(1);
    var additionalInformation = "additionalInformation";

    assertThat(
      new OpenApiCriterionResult(
        PATH_COVERAGE,
        ONE,
        duration,
        additionalInformation
      )
    ).satisfies(
      r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
      r -> assertThat(r.coverage()).isEqualTo(ONE),
      r -> assertThat(r.duration()).isEqualTo(duration),
      r ->
        assertThat(r.additionalInformation()).isEqualTo(additionalInformation)
    );
  }

  @Test
  void requiredArgsConstructor() {
    var duration = Duration.ofSeconds(1);

    assertThat(
      new OpenApiCriterionResult(PATH_COVERAGE, ONE, duration)
    ).satisfies(
      r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
      r -> assertThat(r.coverage()).isEqualTo(ONE),
      r -> assertThat(r.duration()).isEqualTo(duration),
      r -> assertThat(r.additionalInformation()).isNull()
    );
  }
}
