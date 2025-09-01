/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClassPathScanningUtilsTest {

  @Nested
  class ScanPackageForClassesRecursively {

    @Test
    void shouldScanAllClasses() {
      var configClasses =
        ClassPathScanningUtils.scanPackageForClassesRecursively(
          "io.github.bbortt.snow.white.commons.testing",
          new ClassPathScanningUtils.FilterConfiguration(false, true)
        );

      assertThat(configClasses).containsExactlyInAnyOrder(
        io.github.bbortt.snow.white.commons.testing
          .ClassPathScanningUtilsTest.class,
        io.github.bbortt.snow.white.commons.testing
          .ClassPathScanningExceptionTest.class,
        io.github.bbortt.snow.white.commons.testing
          .ClassPathScanningUtils.class,
        io.github.bbortt.snow.white.commons.testing
          .ClassPathScanningException.class,
        io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils
          .FilterConfiguration.class
      );
    }

    @Test
    void shouldScanAllClasses_excludingTestSources() {
      var configClasses =
        ClassPathScanningUtils.scanPackageForClassesRecursively(
          "io.github.bbortt.snow.white.commons.testing",
          new ClassPathScanningUtils.FilterConfiguration(false, false)
        );

      assertThat(configClasses).containsExactlyInAnyOrder(
        io.github.bbortt.snow.white.commons.testing
          .ClassPathScanningException.class,
        io.github.bbortt.snow.white.commons.testing
          .ClassPathScanningUtils.class,
        io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils
          .FilterConfiguration.class
      );
    }

    @Test
    void shouldScanAllClasses_includingStaticNestedClasses() {
      var configClasses =
        ClassPathScanningUtils.scanPackageForClassesRecursively(
          "io.github.bbortt.snow.white.commons.testing"
        );

      assertThat(configClasses).containsExactlyInAnyOrder(
        io.github.bbortt.snow.white.commons.testing
          .ClassPathScanningException.class,
        io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtilsTest
          .ScanPackageForClassesRecursively.class,
        io.github.bbortt.snow.white.commons.testing
          .ClassPathScanningUtils.class,
        io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils
          .FilterConfiguration.class
      );
    }
  }
}
