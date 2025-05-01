/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TestSuiteTest {

  @Test
  void shouldCreateTestSuite_withNameConstructor() {
    var testSuiteName = "sample-suite";
    var testSuite = new TestSuite(testSuiteName);

    assertThat(testSuite)
      .isNotNull()
      .satisfies(
        s -> assertThat(s.getName()).isEqualTo(testSuiteName),
        s -> assertThat(s.getTime()).isEqualTo("0"),
        s -> assertThat(s.getTestCases()).isNotNull().isEmpty()
      );
  }

  @Nested
  class AddTestCase {

    @Test
    void shouldAddTestCaseToTestSuite() {
      var testSuite = new TestSuite("sample-suite");
      var testCase = TestCase.builder()
        .name("test-case")
        .classname("TestClass")
        .build();

      testSuite.addTestCase(testCase);

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
          s -> assertThat(s.getTime()).isEqualTo("0"),
          s -> assertThat(s.getTestCases()).isNotNull().isEmpty()
        );
    }
  }
}
