/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.tool;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import io.github.bbortt.snow.white.microservices.mcp.api.Main;
import java.util.ArrayDeque;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpTool;

/**
 * The MCP surface is a façade over the generated read clients, and no write operation
 * sharing one of those documents is reachable through it.
 */
@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = DoNotIncludeTests.class
)
class ToolSurfaceUnitTest {

  private static final String HTTP_METHOD_FIELD_PREFIX =
    "org.springframework.http.HttpMethod.";

  @VerifiesArch(ArchTraceables.ARCH_021_MCP_SURFACE_IS_A_FACADE)
  @ArchTest
  static final ArchRule toolsReachNoSnowWhiteCodeBesidesTheGeneratedClients =
    noClasses()
      .that()
      .resideInAPackage("..mcp.api.tool..")
      .should()
      .dependOnClassesThat(
        resideInAPackage("io.github.bbortt.snow.white..")
          .and(not(resideInAPackage("..mcp.api.api.client..")))
          .and(not(resideInAPackage("..mcp.api.tool..")))
      )
      .because(
        "a tool is a call through a generated REST client, not a second place where Snow-White derives an answer (ARCH-021)"
      );

  @VerifiesCon(ConTraceables.CON_014_MCP_TOOL_SET_IS_READ_ONLY)
  @ArchTest
  static final ArchRule toolsReachNoWriteOperation = methods()
    .that()
    .areAnnotatedWith(McpTool.class)
    .should(onlyReachGetRequests())
    .because("the v1 tool set mutates nothing (CON-014)");

  /**
   * Reads the verb off the {@code HttpMethod} constant each generated request helper
   * hands to {@code ApiClient#invokeAPI}, rather than off the operation's name: a client
   * method's own signature says nothing about the verb behind it, and an allowlist of
   * names is exactly the check this rule exists to replace.
   */
  private static ArchCondition<JavaMethod> onlyReachGetRequests() {
    return new ArchCondition<>("only reach HttpMethod.GET requests") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        var visited = new HashSet<JavaCodeUnit>();
        var pending = new ArrayDeque<JavaCodeUnit>();
        pending.add(method);

        while (!pending.isEmpty()) {
          var current = pending.poll();
          if (!visited.add(current)) {
            continue;
          }

          current
            .getFieldAccesses()
            .stream()
            .map(access -> access.getTarget().getFullName())
            .filter(field -> field.startsWith(HTTP_METHOD_FIELD_PREFIX))
            .filter(field -> !field.equals(HTTP_METHOD_FIELD_PREFIX + "GET"))
            .forEach(field ->
              events.add(
                violated(
                  method,
                  "%s reaches %s, which issues %s".formatted(
                    method.getFullName(),
                    current.getFullName(),
                    field
                  )
                )
              )
            );

          current
            .getMethodCallsFromSelf()
            .forEach(call ->
              call.getTarget().resolveMember().ifPresent(pending::add)
            );
        }
      }
    };
  }

  /**
   * Keeps the rule above honest. Were every generated operation a {@code GET}, it would
   * pass without ever having something to catch, and would keep passing after a write
   * operation joined one of the three documents.
   */
  @Test
  void generatedClientsStillOfferAWriteOperationForTheRuleToCatch() {
    JavaClasses clients = new ClassFileImporter()
      .withImportOption(new DoNotIncludeTests())
      .importPackages(
        "io.github.bbortt.snow.white.microservices.mcp.api.api.client"
      );

    var writeVerbs = clients
      .stream()
      .flatMap(javaClass -> javaClass.getMethods().stream())
      .flatMap(javaMethod -> javaMethod.getFieldAccesses().stream())
      .map(access -> access.getTarget().getFullName())
      .filter(field -> field.startsWith(HTTP_METHOD_FIELD_PREFIX))
      .filter(field -> !field.equals(HTTP_METHOD_FIELD_PREFIX + "GET"))
      .toList();

    assertThat(writeVerbs).isNotEmpty();
  }
}
