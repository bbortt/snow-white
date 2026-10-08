/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static lombok.AccessLevel.PRIVATE;

import com.tngtech.archunit.lang.ArchRule;
import lombok.NoArgsConstructor;
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

@NoArgsConstructor(access = PRIVATE)
public final class NoSpringConditionalsRules {

  public static ArchRule classesShouldNotUseConditionalOnBeanAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnBean.class)
      .because(
        "@ConditionalOnBean can cause bean resolution issues in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnMissingBeanAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnMissingBean.class)
      .because(
        "@ConditionalOnMissingBean can cause bean resolution issues in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnClassAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnClass.class)
      .because(
        "@ConditionalOnClass relies on classpath scanning which is problematic in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnMissingClassAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnMissingClass.class)
      .because(
        "@ConditionalOnMissingClass relies on classpath scanning which is problematic in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnPropertyAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnProperty.class)
      .because(
        "@ConditionalOnProperty can cause configuration issues in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnExpressionAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnExpression.class)
      .because(
        "@ConditionalOnExpression uses SpEL which can be problematic in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnWebApplicationAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnWebApplication.class)
      .because(
        "@ConditionalOnWebApplication can cause application context issues in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnNotWebApplicationAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnNotWebApplication.class)
      .because(
        "@ConditionalOnNotWebApplication can cause application context issues in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnResourceAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnResource.class)
      .because(
        "@ConditionalOnResource relies on resource scanning which is problematic in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnJavaAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnJava.class)
      .because(
        "@ConditionalOnJava can cause runtime version detection issues in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnCloudPlatformAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnCloudPlatform.class)
      .because(
        "@ConditionalOnCloudPlatform can cause platform detection issues in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnJndiAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnJndi.class)
      .because(
        "@ConditionalOnJndi can cause JNDI lookup issues in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnSingleCandidateAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnSingleCandidate.class)
      .because(
        "@ConditionalOnSingleCandidate can cause bean resolution issues in native images"
      );
  }

  public static ArchRule classesShouldNotUseConditionalOnWarDeploymentAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(ConditionalOnWarDeployment.class)
      .because(
        "@ConditionalOnWarDeployment can cause deployment detection issues in native images"
      );
  }

  public static ArchRule classesShouldNotUseGenericConditionalAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(Conditional.class)
      .because(
        "@Conditional with custom conditions can cause unpredictable behavior in native images"
      );
  }

  public static ArchRule classesShouldNotUseProfileAnnotation() {
    return classes()
      .should()
      .notBeAnnotatedWith(Profile.class)
      .because(
        "@Profile can cause profile resolution issues in native images - use compile-time configuration instead"
      );
  }

  public static ArchRule classesShouldNotUseAnySpringConditionalAnnotation() {
    return classes()
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
  }
}
