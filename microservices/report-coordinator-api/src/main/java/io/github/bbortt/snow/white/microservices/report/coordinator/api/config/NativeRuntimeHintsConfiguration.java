/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static io.github.bbortt.snow.white.commons.testing.ClassPathScanningUtils.scanPackageForClassesRecursively;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Failure;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Property;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Skipped;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.TestCase;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.TestSuite;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.TestSuites;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Slf4j
@Configuration
@RegisterReflectionForBinding(
  {
    // For Coverage Requests
    QualityGateCalculationRequestEvent.class,
    // For Coverage Responses
    OpenApiCoverageResponseEvent.class,
    OpenApiTestResult.class,
    // For JUnit Report Generation
    Failure.class,
    Property.class,
    Skipped.class,
    TestCase.class,
    TestSuite.class,
    TestSuites.class,
  }
)
@ImportRuntimeHints(
  {
    NativeRuntimeHintsConfiguration.QualityGateApiDtoRuntimeHints.class,
    NativeRuntimeHintsConfiguration.RestApiDtoHints.class,
  }
)
public class NativeRuntimeHintsConfiguration {

  static class QualityGateApiDtoRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      scanPackageForClassesRecursively(
        "io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.dto"
      ).forEach(clazz -> hints.reflection().registerType(clazz));
    }
  }

  static class RestApiDtoHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      scanPackageForClassesRecursively(
        "io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto"
      ).forEach(clazz ->
        hints.reflection().registerType(clazz, INVOKE_PUBLIC_METHODS)
      );
    }
  }
}
