/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJndi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;

@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = DoNotIncludeTests.class
)
class NoSpringConditionalsArchitectureTest {

  private static final String BASE_PACKAGE =
    NoSpringConditionalsArchitectureTest.class.getPackageName();

  private final JavaClasses importedClasses =
    new ClassFileImporter().importPackages(BASE_PACKAGE);

  @Test
  void classes_should_not_use_conditional_on_bean_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnBean.class)
      .because(
        "@ConditionalOnBean can cause bean resolution issues in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_missing_bean_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnMissingBean.class)
      .because(
        "@ConditionalOnMissingBean can cause bean resolution issues in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_class_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnClass.class)
      .because(
        "@ConditionalOnClass relies on classpath scanning which is problematic in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_missing_class_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnMissingClass.class)
      .because(
        "@ConditionalOnMissingClass relies on classpath scanning which is problematic in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_property_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnProperty.class)
      .because(
        "@ConditionalOnProperty can cause configuration issues in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_expression_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnExpression.class)
      .because(
        "@ConditionalOnExpression uses SpEL which can be problematic in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_web_application_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnWebApplication.class)
      .because(
        "@ConditionalOnWebApplication can cause application context issues in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_not_web_application_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnNotWebApplication.class)
      .because(
        "@ConditionalOnNotWebApplication can cause application context issues in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_resource_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnResource.class)
      .because(
        "@ConditionalOnResource relies on resource scanning which is problematic in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_java_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnJava.class)
      .because(
        "@ConditionalOnJava can cause runtime version detection issues in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_cloud_platform_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnCloudPlatform.class)
      .because(
        "@ConditionalOnCloudPlatform can cause platform detection issues in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_jndi_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnJndi.class)
      .because(
        "@ConditionalOnJndi can cause JNDI lookup issues in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_single_candidate_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnSingleCandidate.class)
      .because(
        "@ConditionalOnSingleCandidate can cause bean resolution issues in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_conditional_on_war_deployment_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnWarDeployment.class)
      .because(
        "@ConditionalOnWarDeployment can cause deployment detection issues in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_generic_conditional_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(Conditional.class)
      .because(
        "@Conditional with custom conditions can cause unpredictable behavior in native images"
      );

    rule.check(importedClasses);
  }

  @Test
  void classes_should_not_use_profile_annotation() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(Profile.class)
      .because(
        "@Profile can cause profile resolution issues in native images - use compile-time configuration instead"
      );

    rule.check(importedClasses);
  }

  @Test
  void comprehensive_no_spring_conditionals_rule() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(BASE_PACKAGE + "..")
      .should()
      .notBeAnnotatedWith(ConditionalOnBean.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnMissingBean.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnClass.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnMissingClass.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnProperty.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnExpression.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnWebApplication.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnNotWebApplication.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnResource.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnJava.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnCloudPlatform.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnJndi.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnSingleCandidate.class)
      .andShould()
      .notBeAnnotatedWith(ConditionalOnWarDeployment.class)
      .andShould()
      .notBeAnnotatedWith(Conditional.class)
      .andShould()
      .notBeAnnotatedWith(Profile.class)
      .because(
        "Spring conditional annotations can cause runtime issues in native compiled applications. " +
          "Use compile-time configuration with Spring profiles or explicit bean definitions instead."
      );

    rule.check(importedClasses);
  }
}
