/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.junit.Property.property;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class TestCaseTest {

  @Mock
  private DurationFormatter durationFormatterMock;

  @Nested
  class Constructor {

    @Test
    void shouldCreateTestCase() {
      var testCaseName = "sample-case";
      var testCaseClassname = "sample.class";
      var properties = Set.of(property("key", "value"));

      var testCase = new TestCase(testCaseName, testCaseClassname, properties);

      assertThat(testCase)
        .isNotNull()
        .satisfies(
          t -> assertThat(t.getName()).isEqualTo(testCaseName),
          t -> assertThat(t.getClassname()).isEqualTo(testCaseClassname),
          t -> assertThat(t.getTime()).isEqualTo("0"),
          t -> assertThat(t.getProperties()).isEqualTo(properties)
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

      var testCase = TestCase.builder().build();
      testCase = testCase.withDuration(duration, durationFormatterMock);

      assertThat(testCase).satisfies(
        t -> assertThat(t.getDuration()).isEqualTo(duration),
        t -> assertThat(t.getTime()).isEqualTo(formattedTime)
      );
    }
  }

  @Nested
  class Builder {

    @Test
    void shouldUseDefaultValues_whenNotSpecified() {
      var testCase = TestCase.builder().build();

      assertThat(testCase)
        .isNotNull()
        .satisfies(s -> assertThat(s.getTime()).isEqualTo("0"));
    }
  }
}
