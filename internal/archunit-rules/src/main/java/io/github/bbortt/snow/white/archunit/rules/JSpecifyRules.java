/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toSet;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Builder;

public final class JSpecifyRules {

  public static OnlyUseJSpecifyNullable.OnlyUseJSpecifyNullableBuilder onlyUseJSpecifyNullable() {
    return OnlyUseJSpecifyNullable.builder();
  }

  public static OnlyUseJSpecifyNonNull.OnlyUseJSpecifyNonNullBuilder onlyUseJSpecifyNonNull() {
    return OnlyUseJSpecifyNonNull.builder();
  }

  @Builder
  public record OnlyUseJSpecifyNullable(Set<String> ignoringPackages) {
    private static final String JSPECIFY_NULLABLE =
      "org.jspecify.annotations.Nullable";

    private static final Set<String> FORBIDDEN_NULLABLE_ANNOTATIONS = Set.of(
      "jakarta.annotation.Nullable",
      "javax.annotation.Nullable",
      "org.springframework.lang.Nullable",
      "org.jetbrains.annotations.Nullable",
      "androidx.annotation.Nullable",
      "android.support.annotation.Nullable",
      "edu.umd.cs.findbugs.annotations.Nullable",
      "org.eclipse.jdt.annotation.Nullable",
      "org.checkerframework.checker.nullness.qual.Nullable",
      "lombok.NonNull"
    );

    public static class OnlyUseJSpecifyNullableBuilder {

      public ArchRule build() {
        return classes()
          .should(
            new AnnotationsShouldSatisfyCondition(
              "only use JSpecify @Nullable annotation",
              FORBIDDEN_NULLABLE_ANNOTATIONS,
              "Class %s uses forbidden nullable annotation(s): %s. Use %s instead.",
              JSPECIFY_NULLABLE,
              ignoringPackages
            )
          )
          .because(
            "Only JSpecify @Nullable annotation should be used for nullability annotations"
          );
      }
    }
  }

  @Builder
  public record OnlyUseJSpecifyNonNull(Set<String> ignoringPackages) {
    private static final String JSPECIFY_NON_NULL =
      "org.jspecify.annotations.NonNull";

    private static final Set<String> FORBIDDEN_NON_NULL_ANNOTATIONS = Set.of(
      "jakarta.annotation.Nonnull",
      "javax.annotation.Nonnull",
      "org.springframework.lang.NonNull",
      "org.jetbrains.annotations.NotNull",
      "androidx.annotation.NonNull",
      "android.support.annotation.NonNull",
      "edu.umd.cs.findbugs.annotations.NonNull",
      "org.eclipse.jdt.annotation.NonNull",
      "org.checkerframework.checker.nullness.qual.NonNull",
      "lombok.NonNull"
    );

    public static class OnlyUseJSpecifyNonNullBuilder {

      public ArchRule build() {
        return classes()
          .should(
            new AnnotationsShouldSatisfyCondition(
              "only use JSpecify @NonNull annotation",
              FORBIDDEN_NON_NULL_ANNOTATIONS,
              "Class %s uses forbidden non-null annotation(s): %s. Use %s instead.",
              JSPECIFY_NON_NULL,
              ignoringPackages
            )
          )
          .because(
            "Only JSpecify @NonNull annotation should be used for non-null annotations"
          );
      }
    }
  }

  static class AnnotationsShouldSatisfyCondition
    extends ArchCondition<JavaClass> {

    private final Set<String> forbiddenAnnotations;
    private final String errorMessage;
    private final String annotationThatShouldBeUsed;
    private final Set<String> ignoringPackages;

    AnnotationsShouldSatisfyCondition(
      String condition,
      Set<String> forbiddenAnnotations,
      String errorMessage,
      String annotationThatShouldBeUsedString,
      Set<String> ignoringPackages
    ) {
      super(condition);
      this.forbiddenAnnotations = forbiddenAnnotations;
      this.errorMessage = errorMessage;
      this.annotationThatShouldBeUsed = annotationThatShouldBeUsedString;
      this.ignoringPackages = ignoringPackages;
    }

    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
      if (packageIsInIgnoreList(javaClass.getPackageName())) {
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

      // Check field annotations
      javaClass
        .getFields()
        .forEach(field ->
          checkAnnotations(
            javaClass,
            field
              .getAnnotations()
              .stream()
              .map(annotation -> annotation.getRawType().getName()),
            events
          )
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

      // Check constructor parameter annotations
      javaClass
        .getConstructors()
        .forEach(constructor ->
          constructor
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
            )
        );
    }

    private boolean packageIsInIgnoreList(String packageName) {
      return ignoringPackages.stream().anyMatch(packageName::equals);
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
