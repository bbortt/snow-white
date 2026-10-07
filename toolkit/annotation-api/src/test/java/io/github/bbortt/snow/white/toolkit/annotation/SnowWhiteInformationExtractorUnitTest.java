/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.annotation;

import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveApiName;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveApiVersion;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveOperationId;
import static io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformationExtractor.resolveServiceName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SnowWhiteInformationExtractorUnitTest {

  private static SnowWhiteInformation annotationWith(
    String serviceName,
    String apiName,
    String apiVersion,
    String operationId
  ) {
    SnowWhiteInformation annotation = mock(SnowWhiteInformation.class);
    when(annotation.serviceName()).thenReturn(serviceName);
    when(annotation.apiName()).thenReturn(apiName);
    when(annotation.apiVersion()).thenReturn(apiVersion);
    when(annotation.operationId()).thenReturn(operationId);
    return annotation;
  }

  @Nested
  class ResolveServiceNameTest {

    @Test
    void returnsMethodLevelValueWhenPresent() {
      SnowWhiteInformation method = annotationWith(
        "method-service",
        "",
        "",
        ""
      );
      SnowWhiteInformation clazz = annotationWith("class-service", "", "", "");

      assertThat(resolveServiceName(method, clazz)).isEqualTo("method-service");
    }

    @Test
    void fallsBackToClassLevelWhenMethodValueIsBlank() {
      SnowWhiteInformation method = annotationWith("", "", "", "");
      SnowWhiteInformation clazz = annotationWith("class-service", "", "", "");

      assertThat(resolveServiceName(method, clazz)).isEqualTo("class-service");
    }

    @Test
    void fallsBackToClassLevelWhenMethodAnnotationIsNull() {
      SnowWhiteInformation clazz = annotationWith("class-service", "", "", "");

      assertThat(resolveServiceName(null, clazz)).isEqualTo("class-service");
    }

    @Test
    void returnsEmptyStringWhenBothAnnotationsAreNull() {
      assertThat(resolveServiceName(null, null)).isEmpty();
    }

    @Test
    void returnsEmptyStringWhenOnlyMethodAnnotationIsNullAndClassValueIsBlank() {
      SnowWhiteInformation clazz = annotationWith("", "", "", "");

      assertThat(resolveServiceName(null, clazz)).isEmpty();
    }
  }

  @Nested
  class ResolveApiNameTest {

    @Test
    void returnsMethodLevelValueWhenPresent() {
      SnowWhiteInformation method = annotationWith("", "method-api", "", "");
      SnowWhiteInformation clazz = annotationWith("", "class-api", "", "");

      assertThat(resolveApiName(method, clazz)).isEqualTo("method-api");
    }

    @Test
    void fallsBackToClassLevelWhenMethodValueIsBlank() {
      SnowWhiteInformation method = annotationWith("", "", "", "");
      SnowWhiteInformation clazz = annotationWith("", "class-api", "", "");

      assertThat(resolveApiName(method, clazz)).isEqualTo("class-api");
    }

    @Test
    void fallsBackToClassLevelWhenMethodAnnotationIsNull() {
      SnowWhiteInformation clazz = annotationWith("", "class-api", "", "");

      assertThat(resolveApiName(null, clazz)).isEqualTo("class-api");
    }

    @Test
    void returnsEmptyStringWhenBothAnnotationsAreNull() {
      assertThat(resolveApiName(null, null)).isEmpty();
    }

    @Test
    void returnsEmptyStringWhenOnlyMethodAnnotationIsNullAndClassValueIsBlank() {
      SnowWhiteInformation clazz = annotationWith("", "", "", "");

      assertThat(resolveApiName(null, clazz)).isEmpty();
    }
  }

  @Nested
  class ResolveApiVersionTest {

    @Test
    void returnsMethodLevelValueWhenPresent() {
      SnowWhiteInformation method = annotationWith("", "", "v2", "");
      SnowWhiteInformation clazz = annotationWith("", "", "v1", "");

      assertThat(resolveApiVersion(method, clazz)).isEqualTo("v2");
    }

    @Test
    void fallsBackToClassLevelWhenMethodValueIsBlank() {
      SnowWhiteInformation method = annotationWith("", "", "", "");
      SnowWhiteInformation clazz = annotationWith("", "", "v1", "");

      assertThat(resolveApiVersion(method, clazz)).isEqualTo("v1");
    }

    @Test
    void fallsBackToClassLevelWhenMethodAnnotationIsNull() {
      SnowWhiteInformation clazz = annotationWith("", "", "v1", "");

      assertThat(resolveApiVersion(null, clazz)).isEqualTo("v1");
    }

    @Test
    void returnsEmptyStringWhenBothAnnotationsAreNull() {
      assertThat(resolveApiVersion(null, null)).isEmpty();
    }

    @Test
    void returnsEmptyStringWhenOnlyMethodAnnotationIsNullAndClassValueIsBlank() {
      SnowWhiteInformation clazz = annotationWith("", "", "", "");

      assertThat(resolveApiVersion(null, clazz)).isEmpty();
    }
  }

  @Nested
  class ResolveOperationIdTest {

    public static Stream<Arguments> resolvedOperationId() {
      return Stream.of(
        arguments("method-op", "class-op", "method-op"),
        arguments(null, "class-op", "class-op"),
        arguments("", "class-op", "class-op"),
        arguments(" ", "class-op", "class-op"),
        arguments("method-op", "class-op", "method-op")
      );
    }

    @MethodSource
    @ParameterizedTest
    void resolvedOperationId(
      String methodValue,
      String clazzValue,
      String result
    ) {
      SnowWhiteInformation method = annotationWith("", "", "", methodValue);
      SnowWhiteInformation clazz = annotationWith("", "", "", clazzValue);

      assertThat(resolveOperationId(method, clazz)).isEqualTo(result);
    }

    @Test
    void fallsBackToClassLevelWhenMethodAnnotationIsNull() {
      SnowWhiteInformation clazz = annotationWith("", "", "", "class-op");

      assertThat(resolveOperationId(null, clazz)).isEqualTo("class-op");
    }

    @Test
    void returnsEmptyStringWhenBothAnnotationsAreNull() {
      assertThat(resolveOperationId(null, null)).isEmpty();
    }

    @Test
    void returnsEmptyStringWhenOnlyMethodAnnotationIsNullAndClassValueIsBlank() {
      SnowWhiteInformation clazz = annotationWith("", "", "", "");

      assertThat(resolveOperationId(null, clazz)).isEmpty();
    }
  }
}
