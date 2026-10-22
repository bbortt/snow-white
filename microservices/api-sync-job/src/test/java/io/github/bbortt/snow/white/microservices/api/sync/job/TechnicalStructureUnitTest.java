/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = DoNotIncludeTests.class
)
class TechnicalStructureUnitTest {

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

  /**
   * The cadence belongs to whatever starts the process - the chart's
   * {@code CronJob} - so the job holds no clock of its own. An in-process
   * schedule would put a second, invisible cadence next to the operator's.
   */
  @ArchTest
  @VerifiesArch(ArchTraceables.ARCH_014_SYNC_CADENCE_OWNED_BY_THE_SCHEDULER)
  static final ArchRule doesNotEnableSchedulingItself = noClasses()
    .should()
    .beAnnotatedWith(EnableScheduling.class)
    .because(
      "the sync cadence is owned by whatever schedules the process, not by the job itself"
    );

  @ArchTest
  @VerifiesArch(ArchTraceables.ARCH_014_SYNC_CADENCE_OWNED_BY_THE_SCHEDULER)
  static final ArchRule doesNotDeclareAnInProcessSchedule = noMethods()
    .should()
    .beAnnotatedWith(Scheduled.class)
    .because(
      "the sync cadence is owned by whatever schedules the process, not by the job itself"
    );
}
