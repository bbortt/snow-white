/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.archunit.rules;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.lang.ArchRule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

public final class PropertiesRules {

  public static ArchRule configurationPropertiesClassesMustHaveConfigurationAnnotation() {
    return classes()
      .that()
      .areAnnotatedWith(ConfigurationProperties.class)
      .should()
      .beAnnotatedWith(
        describe(
          "@Configuration(proxyBeanMethods = false)",
          annotation ->
            annotation.getRawType().isAssignableFrom(Configuration.class) &&
            !(boolean) annotation.get("proxyBeanMethods").orElse(true)
        )
      )
      .because(
        "@ConfigurationProperties classes must declare @Configuration(proxyBeanMethods = false) to avoid unnecessary CGLIB proxying"
      );
  }
}
