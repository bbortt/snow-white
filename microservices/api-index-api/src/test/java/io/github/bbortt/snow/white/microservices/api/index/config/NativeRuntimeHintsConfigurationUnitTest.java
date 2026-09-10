/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.config;

import static io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils.scanPackageForClassesRecursively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;

@ExtendWith({ MockitoExtension.class })
class NativeRuntimeHintsConfigurationUnitTest {

  @Mock
  private RuntimeHints runtimeHintsMock;

  @Mock
  private ReflectionHints reflectionHintsMock;

  @Mock
  private ClassLoader classLoaderMock;

  private NativeRuntimeHintsConfiguration.ApiIndexApiDtoRuntimeHints apiIndexApiDtoRuntimeHints;

  @BeforeEach
  void beforeEachSetup() {
    apiIndexApiDtoRuntimeHints =
      new NativeRuntimeHintsConfiguration.ApiIndexApiDtoRuntimeHints();
  }

  @Test
  void shouldRegisterApiIndexApiDtoRuntimeHints() {
    doReturn(reflectionHintsMock).when(runtimeHintsMock).reflection();
    doReturn(reflectionHintsMock)
      .when(reflectionHintsMock)
      .registerType(
        any(Class.class),
        eq(INVOKE_PUBLIC_CONSTRUCTORS),
        eq(INVOKE_PUBLIC_METHODS)
      );

    apiIndexApiDtoRuntimeHints.registerHints(runtimeHintsMock, classLoaderMock);

    scanPackageForClassesRecursively(
      "io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.apiindexapi.dto"
    ).forEach(clazz ->
      verify(reflectionHintsMock).registerType(
        clazz,
        INVOKE_PUBLIC_CONSTRUCTORS,
        INVOKE_PUBLIC_METHODS
      )
    );

    verifyNoInteractions(classLoaderMock);
  }
}
