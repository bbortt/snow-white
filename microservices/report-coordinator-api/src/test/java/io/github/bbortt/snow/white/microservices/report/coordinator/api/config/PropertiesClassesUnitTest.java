/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import static io.github.bbortt.snow.white.archunit.rules.PropertiesRules.configurationPropertiesClassesMustHaveConfigurationAnnotation;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.bbortt.snow.white.archunit.rules.PropertiesClassesRules;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.Main;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties;

@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = DoNotIncludeTests.class
)
public class PropertiesClassesUnitTest {

  @ArchTest
  static final ArchRule configurationPropertiesClassesMustHaveConfigurationAnnotation =
    configurationPropertiesClassesMustHaveConfigurationAnnotation();

  @ArchTest
  static final ArchRule propertiesClassesMustNotProxyBeanMethods =
    PropertiesClassesRules.propertiesClassesMustNotProxyBeanMethods(
      Properties.class
    );
}
