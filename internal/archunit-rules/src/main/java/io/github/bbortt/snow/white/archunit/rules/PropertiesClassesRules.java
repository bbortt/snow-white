/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static lombok.AccessLevel.PRIVATE;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

@NoArgsConstructor(access = PRIVATE)
public final class PropertiesClassesRules {

  public static ArchRule propertiesClassesMustNotProxyBeanMethods() {
    return classes()
      .that()
      .areTopLevelClasses()
      .and()
      .haveSimpleNameEndingWith("Properties")
      .should()
      .beAnnotatedWith(Configuration.class)
      .andShould(haveProxyBeanMethodsDisabled());
  }

  public static ArchRule propertiesClassesMustNotProxyBeanMethods(
    Class<?>... exceptions
  ) {
    return classes()
      .that()
      .areTopLevelClasses()
      .and()
      .haveSimpleNameEndingWith("Properties")
      .and()
      .doNotBelongToAnyOf(exceptions)
      .should()
      .beAnnotatedWith(Configuration.class)
      .andShould(haveProxyBeanMethodsDisabled());
  }

  private static ArchCondition<JavaClass> haveProxyBeanMethodsDisabled() {
    return new ArchCondition<>(
      "have @Configuration(proxyBeanMethods = false)"
    ) {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        if (!javaClass.isAnnotatedWith(Configuration.class)) {
          return;
        }

        boolean proxyBeanMethods = javaClass
          .getAnnotationOfType(Configuration.class)
          .proxyBeanMethods();

        if (proxyBeanMethods) {
          events.add(
            SimpleConditionEvent.violated(
              javaClass,
              javaClass.getName() +
                " has @Configuration(proxyBeanMethods = true), expected false"
            )
          );
        }
      }
    };
  }
}
