/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.InfluxDBProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.OpenApiCoverageServiceProperties;

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
    .layer("Kafka").definedBy("..api.kafka..")
    .layer("Redis").definedBy("..api.redis..")
    .layer("Service").definedBy("..service..")

    .whereLayer("Config").mayNotBeAccessedByAnyLayer()
    .whereLayer("Kafka").mayOnlyBeAccessedByLayers("Config")
    .whereLayer("Redis").mayOnlyBeAccessedByLayers("Config", "Service")
    .whereLayer("Service").mayOnlyBeAccessedByLayers("Config", "Kafka")

    .ignoreDependency(belongToAnyOf(Main.class), alwaysTrue())
    .ignoreDependency(alwaysTrue(), belongToAnyOf(InfluxDBProperties.class, OpenApiCoverageServiceProperties.class))
    .ignoreDependency(simpleNameEndingWith("__BeanFactoryRegistrations"), alwaysTrue())
    .ignoreDependency(alwaysTrue(), simpleNameEndingWith("__BeanDefinitions"));
}
