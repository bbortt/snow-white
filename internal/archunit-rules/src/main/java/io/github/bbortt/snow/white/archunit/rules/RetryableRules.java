/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static lombok.AccessLevel.PRIVATE;

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
      new ArchCondition<JavaClass>("have methods annotated with @Retryable") {
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
}
