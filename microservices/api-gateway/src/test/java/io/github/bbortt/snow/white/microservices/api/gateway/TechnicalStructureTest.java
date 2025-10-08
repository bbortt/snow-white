/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packagesOf = Main.class, importOptions = DoNotIncludeTests.class)
class TechnicalStructureTest {

  // prettier-ignore
  @ArchTest
  static final ArchRule respectsTechnicalArchitectureLayers = layeredArchitecture()
    .consideringAllDependencies()
    .layer("Config").definedBy("..config..")
    .layer("Web").definedBy("..web..")
    .optionalLayer("Service").definedBy("..service..")
    .optionalLayer("Persistence").definedBy("..repository..")

    .whereLayer("Config").mayNotBeAccessedByAnyLayer()
    .whereLayer("Web").mayOnlyBeAccessedByLayers("Config")
    .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Config")
    .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service", "Web", "Config")

    .ignoreDependency(belongToAnyOf(Main.class), alwaysTrue());
}
