/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.archunit.rules;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static java.lang.String.format;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class UtilsRules {

  public static ArchRule utilityClassesShouldBeFinalWithPrivateNoArgConstructor() {
    return classes()
      .that()
      .haveSimpleNameEndingWith("Utils")
      .should()
      .haveModifier(FINAL)
      .andShould(haveOnlyOnePrivateNoArgConstructor())
      .because(
        "Utility classes should be final and have only a private no-arg constructor to prevent instantiation and subclassing"
      )
      .allowEmptyShould(true);
  }

  private static ArchCondition<JavaClass> haveOnlyOnePrivateNoArgConstructor() {
    return new ArchCondition<>("have only one private no-arg constructor") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        var constructors = javaClass.getConstructors();
        boolean valid =
          constructors.size() == 1 &&
          constructors.iterator().next().getModifiers().contains(PRIVATE) &&
          constructors.iterator().next().getParameters().isEmpty();
        if (!valid) {
          events.add(
            violated(
              javaClass,
              format(
                "Class %s does not have exactly one private no-arg constructor",
                javaClass.getName()
              )
            )
          );
        }
      }
    };
  }
}
