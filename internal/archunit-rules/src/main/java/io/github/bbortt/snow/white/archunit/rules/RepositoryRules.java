/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static io.github.bbortt.snow.white.archunit.rules.PackageUtils.packageIsInIgnoreList;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Builder;

public final class RepositoryRules {

  public static RepositoryRules.OnlyUseSpringTransactional.OnlyUseSpringTransactionalBuilder onlyUseSpringTransactional() {
    return RepositoryRules.OnlyUseSpringTransactional.builder();
  }

  @Builder
  public record OnlyUseSpringTransactional(Set<String> ignoringPackages) {
    private static final String SPRING_TRANSACTIONAL =
      "org.springframework.transaction.annotation.Transactional";

    private static final Set<String> FORBIDDEN_TRANSACTIONAL_ANNOTATIONS =
      Set.of("jakarta.transaction.Transactional");

    public static class OnlyUseSpringTransactionalBuilder {

      public ArchRule build() {
        return classes()
          .should(
            new RepositoryRules.AnnotationsShouldSatisfyCondition(
              "only use Spring @Transactional annotation",
              FORBIDDEN_TRANSACTIONAL_ANNOTATIONS,
              "Class %s uses forbidden transactional annotation(s): %s. Use %s instead.",
              SPRING_TRANSACTIONAL,
              ignoringPackages
            )
          )
          .because(
            "Only Spring @Transactional annotation should be used for transactional annotations"
          );
      }
    }
  }

  static class AnnotationsShouldSatisfyCondition
    extends ArchCondition<JavaClass>
  {

    private final Set<String> forbiddenAnnotations;
    private final String errorMessage;
    private final String annotationThatShouldBeUsed;
    private final @Nonnull Set<String> ignoringPackages;

    AnnotationsShouldSatisfyCondition(
      String condition,
      Set<String> forbiddenAnnotations,
      String errorMessage,
      String annotationThatShouldBeUsedString,
      @Nullable Set<String> ignoringPackages
    ) {
      super(condition);
      this.forbiddenAnnotations = forbiddenAnnotations;
      this.errorMessage = errorMessage;
      this.annotationThatShouldBeUsed = annotationThatShouldBeUsedString;
      this.ignoringPackages = isNull(ignoringPackages)
        ? emptySet()
        : ignoringPackages;
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
      if (packageIsInIgnoreList(javaClass.getPackageName(), ignoringPackages)) {
        return;
      }

      // Check class-level annotations
      checkAnnotations(
        javaClass,
        javaClass
          .getAnnotations()
          .stream()
          .map(annotation -> annotation.getRawType().getName()),
        events
      );

      // Check method annotations and parameter annotations
      javaClass
        .getMethods()
        .forEach(method -> {
          checkAnnotations(
            javaClass,
            method
              .getAnnotations()
              .stream()
              .map(annotation -> annotation.getRawType().getName()),
            events
          );
          method
            .getParameters()
            .forEach(parameter ->
              checkAnnotations(
                javaClass,
                parameter
                  .getAnnotations()
                  .stream()
                  .map(annotation -> annotation.getRawType().getName()),
                events
              )
            );
        });
    }

    private void checkAnnotations(
      JavaClass javaClass,
      Stream<String> annotationNames,
      ConditionEvents events
    ) {
      Set<String> forbiddenAnnotationsFound = annotationNames
        .filter(forbiddenAnnotations::contains)
        .collect(toSet());

      if (!forbiddenAnnotationsFound.isEmpty()) {
        String message = format(
          errorMessage,
          javaClass.getName(),
          join(", ", forbiddenAnnotationsFound),
          annotationThatShouldBeUsed
        );
        events.add(SimpleConditionEvent.violated(javaClass, message));
      }
    }
  }
}
