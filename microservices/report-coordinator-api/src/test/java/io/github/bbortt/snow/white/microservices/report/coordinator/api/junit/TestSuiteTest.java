/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class TestSuiteTest {

  @Mock
  private DurationFormatter durationFormatterMock;

  @Nested
  class Constructor {

    @Test
    void shouldCreateTestSuite() {
      var testSuiteName = "sample-suite";
      var properties = new HashSet<Property>();

      var testSuite = new TestSuite(testSuiteName, properties);

      assertThat(testSuite)
        .isNotNull()
        .satisfies(
          s -> assertThat(s.getName()).isEqualTo(testSuiteName),
          s -> assertThat(s.getErrors()).isZero(),
          s -> assertThat(s.getSkipped()).isZero(),
          s -> assertThat(s.getTime()).isEqualTo("0"),
          s -> assertThat(s.getProperties()).isEqualTo(properties),
          s -> assertThat(s.getTestCases()).isNotNull().isEmpty()
        );
    }
  }

  @Nested
  class WithDuration {

    @Test
    void shouldSetTimeToFormattedDuration() {
      var duration = Duration.ofMillis(1234);
      var formattedTime = "1.234";

      doReturn(formattedTime)
        .when(durationFormatterMock)
        .toSecondsWithPrecision(duration);

      var testSuite = TestSuite.builder().build();
      testSuite = testSuite.withDuration(duration, durationFormatterMock);

      assertThat(testSuite).satisfies(
        t -> assertThat(t.getDuration()).isEqualTo(duration),
        t -> assertThat(t.getTime()).isEqualTo(formattedTime)
      );
    }
  }

  @Nested
  class AddAllTestCases {

    @Test
    void shouldAddTestCaseToTestSuite() {
      var testSuite = TestSuite.builder().build();
      var testCase = TestCase.builder()
        .name("test-case")
        .classname("TestClass")
        .build();

      testSuite.addAllTestCases(Set.of(testCase));

      assertThat(testSuite.getTestCases())
        .isNotNull()
        .hasSize(1)
        .containsExactly(testCase);
    }
  }

  @Nested
  class Builder {

    @Test
    void shouldUseDefaultValues_whenNotSpecified() {
      var testSuite = TestSuite.builder().build();

      assertThat(testSuite)
        .isNotNull()
        .satisfies(
          s -> assertThat(s.getErrors()).isZero(),
          s -> assertThat(s.getSkipped()).isZero(),
          s -> assertThat(s.getTime()).isEqualTo("0"),
          s -> assertThat(s.getTestCases()).isNotNull().isEmpty()
        );
    }
  }
}
