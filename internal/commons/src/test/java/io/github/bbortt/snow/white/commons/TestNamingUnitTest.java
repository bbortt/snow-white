/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons;

import static io.github.bbortt.snow.white.archunit.rules.TestNamingRules.nestedTestClassesShouldEndInTest;
import static io.github.bbortt.snow.white.archunit.rules.TestNamingRules.testClassesShouldFollowNamingConvention;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packagesOf = PropertyUtils.class)
class TestNamingUnitTest {

  @ArchTest
  static final ArchRule testClassesShouldFollowNamingConvention =
    testClassesShouldFollowNamingConvention();

  @ArchTest
  static final ArchRule nestedTestClassesShouldEndInTest =
    nestedTestClassesShouldEndInTest();
}
