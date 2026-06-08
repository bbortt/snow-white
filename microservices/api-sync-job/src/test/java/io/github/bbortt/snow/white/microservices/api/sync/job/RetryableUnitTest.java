/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.microservices.api.sync.job;

import static io.github.bbortt.snow.white.archunit.rules.RetryableRules.servicesShouldNotHaveRetriableMethods;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = ImportOption.DoNotIncludeTests.class
)
class RetryableUnitTest {

  @ArchTest
  static final ArchRule servicesShouldNotHaveRetriableMethods =
    servicesShouldNotHaveRetriableMethods();
}
