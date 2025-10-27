/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils.scanPackageForClassesRecursively;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

@ExtendWith({ MockitoExtension.class })
class NativeRuntimeHintsConfigurationTest {

  private static final List<String> CLASSES_TO_IGNORE = List.of(
    "DurationFormatter",
    "JUnitReportCreationException",
    "JUnitReporter",
    "JUnitReportResource",
    "Properties"
  );

  @Mock
  private RuntimeHints runtimeHintsMock;

  @Mock
  private ReflectionHints reflectionHintsMock;

  @Mock
  private ClassLoader classLoaderMock;

  private NativeRuntimeHintsConfiguration.QualityGateApiDtoRuntimeHints qualityGateApiDtoRuntimeHints;
  private NativeRuntimeHintsConfiguration.RestApiDtoHints restApiDtoHints;

  @BeforeEach
  void beforeEachSetup() {
    qualityGateApiDtoRuntimeHints =
      new NativeRuntimeHintsConfiguration.QualityGateApiDtoRuntimeHints();
    restApiDtoHints = new NativeRuntimeHintsConfiguration.RestApiDtoHints();
  }

  @Test
  void shouldContainAllJUnitClasses() {
    var registerReflectionForBindings =
      NativeRuntimeHintsConfiguration.class.getDeclaredAnnotationsByType(
        RegisterReflectionForBinding.class
      );

    var junitClasses = scanPackageForClassesRecursively(
      "io.github.bbortt.snow.white.microservices.report.coordinator.api.junit",
      new ClassPathScanningUtils.FilterConfiguration(false, false)
    )
      .stream()
      .filter(clazz -> !clazz.getName().contains("BeanDefinitions"))
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

  @Test
  void shouldRegisterQualityGateApiDtoRuntimeHints() {
    doReturn(reflectionHintsMock).when(runtimeHintsMock).reflection();
    doReturn(reflectionHintsMock)
      .when(reflectionHintsMock)
      .registerType(any(Class.class));

    qualityGateApiDtoRuntimeHints.registerHints(
      runtimeHintsMock,
      classLoaderMock
    );

    scanPackageForClassesRecursively(
      "io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.dto"
    ).forEach(clazz -> verify(reflectionHintsMock).registerType(clazz));

    verifyNoInteractions(classLoaderMock);
  }

  @Test
  void shouldRegisterRestApiDtoRuntimeHints() {
    doReturn(reflectionHintsMock).when(runtimeHintsMock).reflection();
    doReturn(reflectionHintsMock)
      .when(reflectionHintsMock)
      .registerType(any(Class.class), eq(INVOKE_PUBLIC_METHODS));

    restApiDtoHints.registerHints(runtimeHintsMock, classLoaderMock);

    scanPackageForClassesRecursively(
      "io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.restapi.dto"
    ).forEach(clazz ->
      verify(reflectionHintsMock).registerType(clazz, INVOKE_PUBLIC_METHODS)
    );

    verifyNoInteractions(classLoaderMock);
  }
}
