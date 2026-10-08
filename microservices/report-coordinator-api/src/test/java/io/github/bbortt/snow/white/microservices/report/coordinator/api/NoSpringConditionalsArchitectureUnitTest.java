/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;

import com.tngtech.archunit.junit.AnalyzeClasses;
import io.github.bbortt.snow.white.archunit.rules.AbstractNoSpringConditionalsArchitectureTest;

@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = DoNotIncludeTests.class
)
class NoSpringConditionalsArchitectureUnitTest
  extends AbstractNoSpringConditionalsArchitectureTest {}
