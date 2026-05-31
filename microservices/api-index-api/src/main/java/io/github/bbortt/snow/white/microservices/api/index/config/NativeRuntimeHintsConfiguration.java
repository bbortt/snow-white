/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.config;

import static io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils.scanPackageForClassesRecursively;
import static lombok.AccessLevel.PROTECTED;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Slf4j
@Configuration
@NoArgsConstructor(access = PROTECTED)
@ImportRuntimeHints(
  { NativeRuntimeHintsConfiguration.ApiIndexApiDtoRuntimeHints.class }
)
public class NativeRuntimeHintsConfiguration {

  @NullMarked
  static class ApiIndexApiDtoRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(
      RuntimeHints hints,
      @Nullable ClassLoader classLoader
    ) {
      scanPackageForClassesRecursively(
        "io.github.bbortt.snow.white.microservices.api.index.api.rest.dto"
      ).forEach(clazz ->
        hints
          .reflection()
          .registerType(
            clazz,
            INVOKE_PUBLIC_CONSTRUCTORS,
            INVOKE_PUBLIC_METHODS
          )
      );
    }
  }
}
