/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;

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
    .layer("Api").definedBy("..api.kafka..", "..api.rest..")
    .layer("Client").definedBy("..api.client..")
    .layer("Mapper").definedBy("..api.mapper..")
    .layer("Service").definedBy("..service..")
    .layer("Persistence").definedBy("..repository..")
    .layer("Domain").definedBy("..domain..")

    .whereLayer("Config").mayNotBeAccessedByAnyLayer()
    .whereLayer("Api").mayOnlyBeAccessedByLayers("Config","Mapper")
    .whereLayer("Mapper").mayOnlyBeAccessedByLayers("Api")
    .whereLayer("Service").mayOnlyBeAccessedByLayers("Api", "Config")
    .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service",  "Api", "Config")
    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Persistence", "Service",  "Api", "Config")

    .ignoreDependency(belongToAnyOf(Main.class), alwaysTrue())
    .ignoreDependency(alwaysTrue(),belongToAnyOf(ReportCoordinationServiceProperties.class));
}
