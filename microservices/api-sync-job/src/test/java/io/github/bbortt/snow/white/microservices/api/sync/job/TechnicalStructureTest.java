/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;

@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = DoNotIncludeTests.class
)
class TechnicalStructureTest {

  // prettier-ignore
  @ArchTest
  static final ArchRule respectsTechnicalArchitectureLayers = layeredArchitecture()
    .consideringAllDependencies()
    .layer("Api").definedBy("..api..")
    .layer("Config").definedBy("..config..")
    .layer("Service").definedBy("..service..")

    .whereLayer("Api").mayOnlyBeAccessedByLayers("Config")
    .whereLayer("Config").mayNotBeAccessedByAnyLayer()
    .whereLayer("Service").mayOnlyBeAccessedByLayers("Api", "Config")

    .ignoreDependency(belongToAnyOf(Main.class), alwaysTrue())
    .ignoreDependency(alwaysTrue(), belongToAnyOf(ApiSyncJobProperties.class));
}
