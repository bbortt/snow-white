/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static io.github.bbortt.snow.white.commons.event.dto.FindingEvidence.MAX_TEST_CASE_NAME_BYTES;
import static io.github.bbortt.snow.white.toolkit.junit.jupiter.extension.TestCaseName.from;
import static io.github.bbortt.snow.white.toolkit.junit.jupiter.extension.TestCaseName.of;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestCaseNameUnitTest {

  private static final String CLASS_NAME = "com.example.ExampleIT";
  private static final String METHOD_NAME = "shouldDoThing";

  private static final String PLAIN_TEST_UNIQUE_ID =
    "[engine:junit-jupiter]/[class:com.example.ExampleIT]/[method:shouldDoThing()]";
  private static final String TEMPLATE_INVOCATION_UNIQUE_ID =
    "[engine:junit-jupiter]/[class:com.example.ExampleIT]" +
    "/[test-template:shouldDoThing(java.lang.String)]/[test-template-invocation:#2]";

  @Nested
  class OfTest {

    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldQualifyTheClassNameAndSeparateTheMethodWithAHash() {
      assertThat(of(CLASS_NAME, METHOD_NAME, PLAIN_TEST_UNIQUE_ID)).contains(
        "com.example.ExampleIT#shouldDoThing"
      );
    }

    /**
     * The package is the whole point: this repository already has two classes named
     * {@code TestIdentityBaggageAppTest}, and an unqualified name would collapse them into one
     * identity - which the reading side is forbidden from disambiguating.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldKeepTwoLikeNamedClassesInDifferentPackagesApart() {
      var first = of(
        "com.example.one.TestIdentityBaggageAppTest",
        METHOD_NAME,
        PLAIN_TEST_UNIQUE_ID
      );
      var second = of(
        "com.example.two.TestIdentityBaggageAppTest",
        METHOD_NAME,
        PLAIN_TEST_UNIQUE_ID
      );

      assertThat(first).isNotEqualTo(second);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldAppendNoInvocationSuffixForAPlainTest() {
      assertThat(of(CLASS_NAME, METHOD_NAME, PLAIN_TEST_UNIQUE_ID))
        .get()
        .asString()
        .isEqualTo("com.example.ExampleIT#shouldDoThing")
        .doesNotContain("[");
    }

    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldAppendTheInvocationIndexForATemplateInvocation() {
      assertThat(
        of(CLASS_NAME, METHOD_NAME, TEMPLATE_INVOCATION_UNIQUE_ID)
      ).contains("com.example.ExampleIT#shouldDoThing[2]");
    }

    /**
     * Two invocations differ only in the suffix, which is what keeps the identity stable when a
     * test's arguments change but its structure does not.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldDistinguishTwoInvocationsOfOneMethodOnlyByTheirIndex() {
      var first = of(
        CLASS_NAME,
        METHOD_NAME,
        TEMPLATE_INVOCATION_UNIQUE_ID.replace("#2", "#1")
      );
      var second = of(CLASS_NAME, METHOD_NAME, TEMPLATE_INVOCATION_UNIQUE_ID);

      assertThat(first).contains("com.example.ExampleIT#shouldDoThing[1]");
      assertThat(second).contains("com.example.ExampleIT#shouldDoThing[2]");
      assertThat(first).isNotEqualTo(second);
    }

    /**
     * A multi-digit index must survive whole - a suite of more than nine cases is ordinary, and
     * truncating to one digit would silently merge invocation 1 with 10.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldKeepAMultiDigitInvocationIndexWhole() {
      assertThat(
        of(
          CLASS_NAME,
          METHOD_NAME,
          TEMPLATE_INVOCATION_UNIQUE_ID.replace("#2", "#137")
        )
      ).contains("com.example.ExampleIT#shouldDoThing[137]");
    }

    /**
     * The pattern is anchored at the end on purpose: a class or method whose own name happens to
     * contain the marker text must not be read as an invocation.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldOnlyReadAnInvocationIndexFromTheEndOfTheUniqueId() {
      assertThat(
        of(
          CLASS_NAME,
          METHOD_NAME,
          "[engine:junit-jupiter]/[test-template-invocation:#9]/[method:shouldDoThing()]"
        )
      )
        .get()
        .asString()
        .doesNotContain("[9]");
    }
  }

  @Nested
  class StorableBoundTest {

    /**
     * Over-long is dropped whole rather than truncated, matching what the storing side does with
     * such a name, so the two sides cannot disagree about which names exist.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldYieldNoIdentityOneByteOverTheStorableBound() {
      assertThat(
        of(
          CLASS_NAME,
          methodNameFillingTo(MAX_TEST_CASE_NAME_BYTES + 1),
          PLAIN_TEST_UNIQUE_ID
        )
      ).isEmpty();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldProduceANameExactlyAtTheStorableBound() {
      assertThat(
        of(
          CLASS_NAME,
          methodNameFillingTo(MAX_TEST_CASE_NAME_BYTES),
          PLAIN_TEST_UNIQUE_ID
        )
      )
        .get()
        .asString()
        .hasSize(MAX_TEST_CASE_NAME_BYTES);
    }

    /**
     * The bound is in bytes, not characters: such a name sits under the limit in characters and
     * over it in bytes, and it is the byte count the column and its constraint btree must hold.
     */
    @ParameterizedTest
    @ValueSource(strings = { "中", "😀" })
    @DisplayName("should apply the bound in UTF-8 bytes rather than characters")
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldApplyTheBoundInBytes(String multiByteCharacter) {
      var bytesPerCharacter = multiByteCharacter.getBytes(UTF_8).length;
      var methodName = multiByteCharacter.repeat(
        MAX_TEST_CASE_NAME_BYTES / bytesPerCharacter + 1
      );

      assertThat(methodName.length()).isLessThan(MAX_TEST_CASE_NAME_BYTES);
      assertThat(methodName.getBytes(UTF_8).length).isGreaterThan(
        MAX_TEST_CASE_NAME_BYTES
      );
      assertThat(of(CLASS_NAME, methodName, PLAIN_TEST_UNIQUE_ID)).isEmpty();
    }

    private static String methodNameFillingTo(int totalBytes) {
      return "a".repeat(totalBytes - (CLASS_NAME + "#").length());
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class FromTest {

    @Mock
    private ExtensionContext extensionContext;

    static class ExampleIT {

      void shouldDoThing() {
        // a real method to reflect over
      }
    }

    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldDeriveTheIdentityFromTheExtensionContext()
      throws NoSuchMethodException {
      when(extensionContext.getTestClass()).thenReturn(
        Optional.of(ExampleIT.class)
      );
      when(extensionContext.getTestMethod()).thenReturn(
        Optional.of(exampleMethod())
      );
      when(extensionContext.getUniqueId()).thenReturn(PLAIN_TEST_UNIQUE_ID);

      assertThat(from(extensionContext)).contains(
        ExampleIT.class.getName() + "#shouldDoThing"
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldYieldNoIdentityWithoutATestMethod() {
      when(extensionContext.getTestClass()).thenReturn(
        Optional.of(ExampleIT.class)
      );
      when(extensionContext.getTestMethod()).thenReturn(Optional.empty());

      assertThat(from(extensionContext)).isEmpty();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
    void shouldYieldNoIdentityWithoutATestClass() throws NoSuchMethodException {
      when(extensionContext.getTestClass()).thenReturn(Optional.empty());
      when(extensionContext.getTestMethod()).thenReturn(
        Optional.of(exampleMethod())
      );

      assertThat(from(extensionContext)).isEmpty();
    }

    private static Method exampleMethod() throws NoSuchMethodException {
      return ExampleIT.class.getDeclaredMethod("shouldDoThing");
    }
  }
}
