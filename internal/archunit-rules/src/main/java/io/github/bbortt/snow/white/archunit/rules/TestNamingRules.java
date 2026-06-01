/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.archunit.rules;

import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static lombok.AccessLevel.PRIVATE;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Nested;

@NoArgsConstructor(access = PRIVATE)
public final class TestNamingRules {

  public static ArchRule testClassesShouldFollowNamingConvention() {
    return classes()
      .that()
      .areTopLevelClasses()
      .and(haveTestMethodsOrArchTestFields())
      .should()
      .haveSimpleNameEndingWith("UnitTest")
      .orShould()
      .haveSimpleNameEndingWith("IT")
      .because(
        "test classes must use the UnitTest suffix for unit tests or the IT suffix for integration tests"
      )
      .allowEmptyShould(true);
  }

  public static ArchRule nestedTestClassesShouldEndInTest() {
    return classes()
      .that()
      .areAnnotatedWith(Nested.class)
      .and()
      .haveModifier(STATIC)
      .should()
      .haveSimpleNameEndingWith("Test")
      .because(
        "static @Nested test classes must end in Test to be clearly identifiable as grouped test scenarios"
      )
      .allowEmptyShould(true);
  }

  private static DescribedPredicate<
    JavaClass
  > haveTestMethodsOrArchTestFields() {
    return new DescribedPredicate<>(
      "have @Test/@ParameterizedTest methods or @ArchTest fields"
    ) {
      @Override
      public boolean test(JavaClass javaClass) {
        return (
          javaClass
            .getMethods()
            .stream()
            .anyMatch(
              m ->
                m.isAnnotatedWith("org.junit.jupiter.api.Test") ||
                m.isAnnotatedWith("org.junit.jupiter.params.ParameterizedTest")
            ) ||
          javaClass
            .getFields()
            .stream()
            .anyMatch(f -> f.isAnnotatedWith(ArchTest.class))
        );
      }
    };
  }
}
