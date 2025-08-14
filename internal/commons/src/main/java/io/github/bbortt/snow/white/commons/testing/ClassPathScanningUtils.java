/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.testing;

import static java.util.Collections.addAll;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class ClassPathScanningUtils {

  public static Set<Class<?>> scanPackageForClassesRecursively(
    String packageName
  ) {
    return scanPackageForClassesRecursively(
      packageName,
      new FilterConfiguration(true, false)
    );
  }

  public static Set<Class<?>> scanPackageForClassesRecursively(
    String packageName,
    FilterConfiguration filterConfiguration
  ) {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);

    scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

    Set<Class<?>> classes = new HashSet<>();

    scanner
      .findCandidateComponents(packageName)
      .forEach(beanDefinition -> {
        try {
          Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
          classes.add(clazz);

          if (filterConfiguration.includeNestedClasses()) {
            Class<?>[] nestedClasses = clazz.getDeclaredClasses();
            addAll(classes, nestedClasses);
          }
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      });

    logger.debug("Scanned classes: {}", classes);

    if (filterConfiguration.includeTestSources()) {
      return classes;
    } else {
      return classes
        .stream()
        .filter(
          clazz ->
            !clazz.getName().endsWith("Test") && !clazz.getName().endsWith("IT")
        )
        .collect(toSet());
    }
  }

  public record FilterConfiguration(
    boolean includeNestedClasses,
    boolean includeTestSources
  ) {}
}
