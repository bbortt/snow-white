/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.tool;

import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.dto.GetReportByCalculationId200ResponseInterfacesInnerTestResultsInner;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.dto.ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.mcp.annotation.McpTool;

/** What the tools answer with is the REST surface's own shape. */
@VerifiesArch(ArchTraceables.ARCH_021_MCP_SURFACE_IS_A_FACADE)
class McpResponseShapeUnitTest {

  private static final String GENERATED_DTO_PACKAGE_SUFFIX = ".api.client";

  /** The shape a ported verdict derivation would have to produce. */
  private static final Pattern VERDICT_FIELD = Pattern.compile(
    "(?i)(?:pass|fail|verdict)"
  );

  private static List<JavaMethod> declaredTools() {
    return new ClassFileImporter()
      .withImportOption(new DoNotIncludeTests())
      .importPackages("io.github.bbortt.snow.white.microservices.mcp.api")
      .stream()
      .flatMap(javaClass -> javaClass.getMethods().stream())
      .filter(javaMethod -> javaMethod.isAnnotatedWith(McpTool.class))
      .toList();
  }

  /**
   * Every tool answers with a type the REST surface already defines. A response type of
   * this module's own making is where a derived value would appear, and deriving is what
   * stays in the services.
   */
  @Test
  void answersOnlyWithGeneratedClientTypes() {
    var responseTypes = declaredTools()
      .stream()
      .map(javaMethod -> unwrap(javaMethod.reflect().getGenericReturnType()))
      .map(Type::getTypeName)
      .toList();

    assertThat(responseTypes)
      .isNotEmpty()
      .allSatisfy(typeName ->
        assertThat(typeName).contains(GENERATED_DTO_PACKAGE_SUFFIX)
      );
  }

  /**
   * A per-criterion result carries its two numbers and no judgement of them. A client
   * holding both can ask the follow-up question; the report's own status is the only
   * verdict this surface serves.
   */
  @ParameterizedTest
  @ValueSource(
    classes = {
      ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner.class,
      GetReportByCalculationId200ResponseInterfacesInnerTestResultsInner.class,
    }
  )
  void noPerCriterionResultCarriesAVerdictField(Class<?> perCriterionResult) {
    assertThat(perCriterionResult.getDeclaredFields())
      .extracting(Field::getName)
      .isNotEmpty()
      .noneMatch(name -> VERDICT_FIELD.matcher(name).find());
  }

  private static Type unwrap(Type type) {
    return type instanceof ParameterizedType parameterized &&
      parameterized.getRawType().equals(List.class)
      ? parameterized.getActualTypeArguments()[0]
      : type;
  }
}
