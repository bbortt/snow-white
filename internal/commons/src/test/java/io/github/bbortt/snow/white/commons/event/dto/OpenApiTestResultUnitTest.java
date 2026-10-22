/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static java.math.BigDecimal.ONE;
import static nl.jqno.equalsverifier.Warning.BIGDECIMAL_EQUALITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OpenApiTestResultUnitTest {

  private static final ApiTestFinding FINDING = ApiTestFinding.builder()
    .specPointer("/paths/~1api")
    .status(COVERED)
    .evidence(List.of(new FindingEvidence("traceId", null)))
    .build();

  @Test
  void equalsContract() {
    EqualsVerifier.forClass(OpenApiTestResult.class)
      .suppress(BIGDECIMAL_EQUALITY)
      .verify();
  }

  @Test
  void allArgsConstructor() {
    var duration = Duration.ofSeconds(1);
    var additionalInformation = "additionalInformation";

    assertThat(
      new OpenApiTestResult(
        PATH_COVERAGE,
        ONE,
        duration,
        additionalInformation,
        List.of(FINDING)
      )
    ).satisfies(
      r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
      r -> assertThat(r.coverage()).isEqualTo(ONE),
      r -> assertThat(r.duration()).isEqualTo(duration),
      r ->
        assertThat(r.additionalInformation()).isEqualTo(additionalInformation),
      r -> assertThat(r.findings()).containsExactly(FINDING)
    );
  }

  @Test
  void additionalInformationConstructor() {
    var duration = Duration.ofSeconds(1);
    var additionalInformation = "additionalInformation";

    assertThat(
      new OpenApiTestResult(PATH_COVERAGE, ONE, duration, additionalInformation)
    ).satisfies(
      r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
      r -> assertThat(r.coverage()).isEqualTo(ONE),
      r -> assertThat(r.duration()).isEqualTo(duration),
      r ->
        assertThat(r.additionalInformation()).isEqualTo(additionalInformation),
      r -> assertThat(r.findings()).isEmpty()
    );
  }

  @Test
  void requiredArgsConstructor() {
    var duration = Duration.ofSeconds(1);

    assertThat(new OpenApiTestResult(PATH_COVERAGE, ONE, duration)).satisfies(
      r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
      r -> assertThat(r.coverage()).isEqualTo(ONE),
      r -> assertThat(r.duration()).isEqualTo(duration),
      r -> assertThat(r.additionalInformation()).isNull(),
      r -> assertThat(r.findings()).isEmpty()
    );
  }

  @Nested
  class FindingsTest {

    @Test
    void shouldBeDecoupledFromTheListItWasBuiltFrom() {
      var mutableFindings = new ArrayList<ApiTestFinding>();
      mutableFindings.add(FINDING);

      var result = new OpenApiTestResult(
        PATH_COVERAGE,
        ONE,
        Duration.ofSeconds(1),
        null,
        mutableFindings
      );

      mutableFindings.clear();

      assertThat(result.findings()).containsExactly(FINDING);
    }

    @Test
    void shouldBeUnmodifiable() {
      var result = new OpenApiTestResult(
        PATH_COVERAGE,
        ONE,
        Duration.ofSeconds(1),
        null,
        List.of(FINDING)
      );
      var findings = result.findings();

      assertThatThrownBy(() -> findings.add(FINDING)).isInstanceOf(
        UnsupportedOperationException.class
      );
    }

    @Test
    void shouldRejectBeingNull() {
      var duration = Duration.ofSeconds(1);

      assertThatThrownBy(() ->
        new OpenApiTestResult(PATH_COVERAGE, ONE, duration, null, null)
      ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE
    )
    void shouldBeEmptyWhenTheSerializedResultPredatesThem() {
      var withoutFindings = """
      {
        "openApiCriteria": "PATH_COVERAGE",
        "coverage": 1,
        "duration": "PT1S",
        "additionalInformation": null
      }
      """;

      var result = JsonMapper.builder()
        .build()
        .readValue(withoutFindings, OpenApiTestResult.class);

      assertThat(result.findings()).isEmpty();
      assertThat(result.coverage()).isEqualByComparingTo(ONE);
    }

    @Test
    void shouldSurviveASerializationRoundTrip() {
      var jsonMapper = JsonMapper.builder().build();
      var result = new OpenApiTestResult(
        PATH_COVERAGE,
        ONE,
        Duration.ofSeconds(1),
        null,
        List.of(FINDING)
      );

      assertThat(
        jsonMapper.readValue(
          jsonMapper.writeValueAsString(result),
          OpenApiTestResult.class
        )
      ).isEqualTo(result);
    }
  }
}
