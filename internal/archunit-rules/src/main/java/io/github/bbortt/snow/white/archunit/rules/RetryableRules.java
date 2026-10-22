/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static lombok.AccessLevel.PRIVATE;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import lombok.NoArgsConstructor;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@NoArgsConstructor(access = PRIVATE)
public final class RetryableRules {

  public static ArchRule servicesShouldNotHaveRetriableMethods() {
    return noClasses().that().areAnnotatedWith(Service.class).should(
      new ArchCondition<>("have methods annotated with @Retryable") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
          javaClass
            .getMethods()
            .stream()
            .filter(method -> method.isAnnotatedWith(Retryable.class))
            .forEach(method ->
              events.add(
                SimpleConditionEvent.violated(
                  javaClass,
                  javaClass.getName() +
                    "." +
                    method.getName() +
                    "() is annotated with @Retryable but declared in a @Service class"
                )
              )
            );
        }
      }
    );
  }

  /**
   * Classes with an {@code @Retryable} method and no implemented interface are CGLIB-proxied by
   * Spring AOP. Spring normally instantiates that proxy without calling any constructor
   * (Objenesis), but falls back to invoking a genuine no-arg constructor on the generated
   * subclass when Objenesis can't - which happens with some JVM/agent combinations (observed
   * during a JDK {@code -XX:AOTCacheOutput} training run). A class with only a parameterized
   * constructor has no such fallback and fails to start. Use field/setter injection instead of
   * constructor injection for these classes.
   */
  public static ArchRule retryableClassesShouldDeclareNoArgConstructor() {
    return classes()
      .that(haveAMethodAnnotatedWithRetryable())
      .and(implementNoInterfaces())
      .should(declareANoArgConstructor())
      .allowEmptyShould(true);
  }

  private static DescribedPredicate<JavaClass> haveAMethodAnnotatedWithRetryable() {
    return new DescribedPredicate<>("have a method annotated with @Retryable") {
      @Override
      public boolean test(JavaClass javaClass) {
        return javaClass
          .getMethods()
          .stream()
          .anyMatch(method -> method.isAnnotatedWith(Retryable.class));
      }
    };
  }

  private static DescribedPredicate<JavaClass> implementNoInterfaces() {
    return new DescribedPredicate<>("implement no interfaces") {
      @Override
      public boolean test(JavaClass javaClass) {
        return javaClass.getInterfaces().isEmpty();
      }
    };
  }

  private static ArchCondition<JavaClass> declareANoArgConstructor() {
    return new ArchCondition<>("declare a no-arg constructor") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        boolean hasNoArgConstructor = javaClass
          .getConstructors()
          .stream()
          .anyMatch(constructor -> constructor.getParameters().isEmpty());

        if (!hasNoArgConstructor) {
          events.add(
            SimpleConditionEvent.violated(
              javaClass,
              javaClass.getName() +
                " has a @Retryable method, implements no interface (so Spring CGLIB-proxies it)," +
                " but declares no no-arg constructor - use setter/field injection instead of" +
                " constructor injection"
            )
          );
        }
      }
    };
  }
}
