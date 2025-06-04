/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TestSuitesTest {

  @Nested
  class AddTestSuite {

    @Test
    void shouldAddTestSuiteToTestSuites() {
      TestSuites testSuites = new TestSuites();
      TestSuite testSuite = new TestSuite("added-suite");

      testSuites.addTestSuite(testSuite);

      assertThat(testSuites.getContainedSuites())
        .isNotNull()
        .hasSize(1)
        .containsExactly(testSuite);
    }
  }

  @Nested
  class Builder {

    @Test
    void shouldUseDefaultValues_whenNotSpecified() {
      TestSuites testSuites = TestSuites.builder().build();

      assertThat(testSuites)
        .isNotNull()
        .satisfies(
          s -> assertThat(s.getErrors()).isZero(),
          s -> assertThat(s.getSkipped()).isZero(),
          s -> assertThat(s.getTime()).isEqualTo("0"),
          s -> assertThat(s.getContainedSuites()).isNotNull().isEmpty()
        );
    }
  }
}
