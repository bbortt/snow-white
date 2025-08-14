/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import static io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils.scanPackageForClassesRecursively;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

class NativeRuntimeHintsConfigurationTest {

  private static final List<String> CLASSES_TO_IGNORE = List.of(
    "DurationFormatter",
    "JUnitReportCreationException",
    "JUnitReporter",
    "JUnitReportResource",
    "Properties"
  );

  @Test
  void shouldContainAllJUnitClasses() {
    var registerReflectionForBindings =
      NativeRuntimeHintsConfiguration.class.getDeclaredAnnotationsByType(
        RegisterReflectionForBinding.class
      );

    var junitClasses = scanPackageForClassesRecursively(
      "io.github.bbortt.snow.white.microservices.report.coordination.service.junit",
      new ClassPathScanningUtils.FilterConfiguration(false, false)
    )
      .stream()
      .filter(clazz -> !clazz.getName().endsWith("Builder"))
      .filter(clazz -> !clazz.getName().endsWith("Factory"))
      .filter(clazz ->
        CLASSES_TO_IGNORE.stream().noneMatch(clazz.getSimpleName()::equals)
      )
      .collect(toSet());

    assertThat(registerReflectionForBindings)
      .hasSize(1)
      .hasOnlyOneElementSatisfying(annotation ->
        assertThat(annotation.value()).containsAll(junitClasses)
      );
  }
}
