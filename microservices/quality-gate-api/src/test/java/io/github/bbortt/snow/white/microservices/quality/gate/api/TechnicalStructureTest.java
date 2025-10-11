/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties;

@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = DoNotIncludeTests.class
)
class TechnicalStructureTest {

  // prettier-ignore
  @ArchTest
  static final ArchRule respectsTechnicalArchitectureLayers = layeredArchitecture()
    .consideringAllDependencies()
    .layer("Config").definedBy("..config..")
    .layer("Init").definedBy("..init..")
    .layer("Web").definedBy("..api.rest..")
    .layer("Service").definedBy("..service..")
    .layer("Persistence").definedBy("..repository..")
    .layer("Domain").definedBy("..domain..")

    .whereLayer("Config").mayNotBeAccessedByAnyLayer()
    .whereLayer("Web").mayOnlyBeAccessedByLayers("Config")
    .whereLayer("Service").mayOnlyBeAccessedByLayers("Web", "Config","Init")
    .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service",  "Web", "Config")
    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Persistence", "Service",  "Web", "Config")

    .ignoreDependency(belongToAnyOf(Main.class), alwaysTrue())
    .ignoreDependency(alwaysTrue(),belongToAnyOf(QualityGateApiProperties.class))
      .ignoreDependency(simpleNameEndingWith("__BeanFactoryRegistrations"), alwaysTrue())
      .ignoreDependency(alwaysTrue(), simpleNameEndingWith("__BeanDefinitions"));
}
