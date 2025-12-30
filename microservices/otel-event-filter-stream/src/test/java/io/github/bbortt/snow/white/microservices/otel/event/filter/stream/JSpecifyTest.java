/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream;

import static io.github.bbortt.snow.white.archunit.rules.JSpecifyRules.onlyUseJSpecifyNonNull;
import static io.github.bbortt.snow.white.archunit.rules.JSpecifyRules.onlyUseJSpecifyNullable;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;

@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = DoNotIncludeTests.class
)
class JSpecifyTest {

  private static final Set<String> GENERATED_PACKAGES = Set.of(
    "io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.apiindexapi",
    "io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.apiindexapi.api",
    "io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.apiindexapi.dto"
  );

  @ArchTest
  static final ArchRule onlyUseJSpecifyNullable = onlyUseJSpecifyNullable()
    .ignoringPackages(GENERATED_PACKAGES)
    .build();

  @ArchTest
  static final ArchRule onlyUseJSpecifyNonNull = onlyUseJSpecifyNonNull()
    .ignoringPackages(GENERATED_PACKAGES)
    .build();
}
