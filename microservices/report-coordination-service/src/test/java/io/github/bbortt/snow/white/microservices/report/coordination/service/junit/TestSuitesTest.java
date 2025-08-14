/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TestSuitesTest {

  @Nested
  class Constructor {

    @Test
    void shouldCreateTestSuites() {
      var testSuiteName = "sample-suite";
      var timestamp = "timestamp";
      var properties = new HashSet<Property>();

      var testSuites = new TestSuites(testSuiteName, timestamp, properties);

      assertThat(testSuites).satisfies(
        s -> assertThat(s.getName()).isEqualTo(testSuiteName),
        s -> assertThat(s.getErrors()).isZero(),
        s -> assertThat(s.getSkipped()).isZero(),
        s -> assertThat(s.getTime()).isEqualTo("0"),
        s -> assertThat(s.getTimestamp()).isEqualTo(timestamp),
        s -> assertThat(s.getProperties()).isEqualTo(properties),
        s -> assertThat(s.getContainedSuites()).isNotNull().isEmpty()
      );
    }
  }

  @Nested
  class AddAllTestSuites {

    @Test
    void shouldAddTestSuite() {
      var testSuite = TestSuite.builder().build();

      var testSuites = TestSuites.builder().build();
      testSuites.addAllTestSuite(Set.of(testSuite));

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
