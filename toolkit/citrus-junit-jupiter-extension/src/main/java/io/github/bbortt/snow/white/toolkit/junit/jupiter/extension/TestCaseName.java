/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static io.github.bbortt.snow.white.commons.event.dto.FindingEvidence.MAX_TEST_CASE_NAME_BYTES;
import static java.nio.charset.StandardCharsets.UTF_8;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Derives a test's {@code test.case.name} from the context JUnit hands the extension.
 * <p>
 * The value is {@code <fully qualified class>#<method>}, plus {@code [<n>]} where JUnit
 * distinguishes one invocation of a method from another. Qualifying it is the decision: this
 * repository already has two classes named {@code TestIdentityBaggageAppTest}, and two tests
 * resolving to one identity is worse than none, because the reading side is forbidden from parsing
 * the value to disambiguate it.
 */
@NullMarked
final class TestCaseName {

  /**
   * The tail JUnit appends to a template invocation's unique id - {@code @ParameterizedTest},
   * {@code @RepeatedTest}, any {@code @TestTemplate}. A plain {@code @Test} ends in
   * {@code [method:name()]} instead and so matches nothing here.
   * <p>
   * The unique id is the only place the invocation index is exposed; the display name is not used,
   * because it is author-controlled free text that usually embeds the arguments, which would make
   * the identity change whenever the data does.
   */
  private static final Pattern TEMPLATE_INVOCATION = Pattern.compile(
    "\\[test-template-invocation:#(\\d+)]$"
  );

  private TestCaseName() {
    // utility class
  }

  /**
   * The identity of the test this context describes, or empty where there is none.
   * <p>
   * Empty is an ordinary outcome and never an error: downstream, a finding with no test name
   * simply falls back to its trace id.
   */
  static Optional<String> from(ExtensionContext context) {
    var testClass = context.getTestClass().map(Class::getName);
    var testMethod = context.getTestMethod().map(Method::getName);

    if (testClass.isEmpty() || testMethod.isEmpty()) {
      return Optional.empty();
    }

    return of(testClass.get(), testMethod.get(), context.getUniqueId());
  }

  /**
   * The naming rule itself, as a pure function of the three things it depends on.
   * <p>
   * Separated from {@link #from(ExtensionContext)} because a real {@code Method} cannot be given an
   * arbitrary name, and the byte bound below is only interesting for names no real test method
   * has.
   */
  @VisibleForTesting
  @RealizesSw(SwTraceables.SW_046_TEST_CASE_NAME_IS_THE_QUALIFIED_INVOCATION)
  static Optional<String> of(
    String testClassName,
    String testMethodName,
    String uniqueId
  ) {
    return storable(
      testClassName + '#' + testMethodName + invocationSuffix(uniqueId)
    );
  }

  private static String invocationSuffix(String uniqueId) {
    var invocation = TEMPLATE_INVOCATION.matcher(uniqueId);
    return invocation.find() ? "[" + invocation.group(1) + "]" : "";
  }

  /**
   * Drops a name the storing side could not keep anyway.
   * <p>
   * Truncating is forbidden, so an over-long name is no identity at all rather than a shortened
   * one. The bound is read from the event contract both sides speak rather than restated here,
   * which is the whole reason that constant lives there.
   */
  private static Optional<String> storable(String testCaseName) {
    return testCaseName.getBytes(UTF_8).length > MAX_TEST_CASE_NAME_BYTES
      ? Optional.empty()
      : Optional.of(testCaseName);
  }
}
