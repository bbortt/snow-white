/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.bbortt.snow.white.microservices.mcp.api.config.McpApiProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = DoNotIncludeTests.class
)
class TechnicalStructureUnitTest {

  // prettier-ignore
  @ArchTest
  static final ArchRule respectsTechnicalArchitectureLayers = layeredArchitecture()
    .consideringAllDependencies()
    .layer("Client").definedBy("..api.client..")
    .layer("Config").definedBy("..config..")
    .layer("Tool").definedBy("..tool..")
    .whereLayer("Client").mayOnlyBeAccessedByLayers("Config", "Tool")
    .whereLayer("Config").mayNotBeAccessedByAnyLayer()
    .whereLayer("Tool").mayNotBeAccessedByAnyLayer()
    .ignoreDependency(belongToAnyOf(Main.class), alwaysTrue())
    .ignoreDependency(alwaysTrue(), belongToAnyOf(McpApiProperties.class));

  /**
   * The CLI's agentic transformer is not ported here, and reaching for the module it lives
   * in is how a port would start. Stated over the POM rather than over classes because
   * {@code toolkit/cli} is a TypeScript build behind a Maven module, so a dependency on it
   * is something only the POM can express.
   */
  @VerifiesArch(ArchTraceables.ARCH_021_MCP_SURFACE_IS_A_FACADE)
  @Test
  void theModuleDoesNotDependOnTheCliModule() throws IOException {
    assertThat(Files.readString(Path.of("pom.xml"))).doesNotContain(
      "snow-white.toolkit"
    );
  }
}
