/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.archunit.rules;

import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseAnySpringConditionalAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnBeanAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnClassAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnCloudPlatformAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnExpressionAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnJavaAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnJndiAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnMissingBeanAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnMissingClassAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnNotWebApplicationAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnPropertyAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnResourceAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnSingleCandidateAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnWarDeploymentAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseConditionalOnWebApplicationAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseGenericConditionalAnnotation;
import static io.github.bbortt.snow.white.archunit.rules.NoSpringConditionalsRules.classesShouldNotUseProfileAnnotation;
import static lombok.AccessLevel.PROTECTED;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PROTECTED)
public abstract class AbstractNoSpringConditionalsArchitectureTest {

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnBeanAnnotation =
    classesShouldNotUseConditionalOnBeanAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnMissingBeanAnnotation =
    classesShouldNotUseConditionalOnMissingBeanAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnClassAnnotation =
    classesShouldNotUseConditionalOnClassAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnMissingClassAnnotation =
    classesShouldNotUseConditionalOnMissingClassAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnPropertyAnnotation =
    classesShouldNotUseConditionalOnPropertyAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnExpressionAnnotation =
    classesShouldNotUseConditionalOnExpressionAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnWebApplicationAnnotation =
    classesShouldNotUseConditionalOnWebApplicationAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnNotWebApplicationAnnotation =
    classesShouldNotUseConditionalOnNotWebApplicationAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnResourceAnnotation =
    classesShouldNotUseConditionalOnResourceAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnJavaAnnotation =
    classesShouldNotUseConditionalOnJavaAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnCloudPlatformAnnotation =
    classesShouldNotUseConditionalOnCloudPlatformAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnJndiAnnotation =
    classesShouldNotUseConditionalOnJndiAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnSingleCandidateAnnotation =
    classesShouldNotUseConditionalOnSingleCandidateAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseConditionalOnWarDeploymentAnnotation =
    classesShouldNotUseConditionalOnWarDeploymentAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseGenericConditionalAnnotation =
    classesShouldNotUseGenericConditionalAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseProfileAnnotation =
    classesShouldNotUseProfileAnnotation();

  @ArchTest
  public static ArchRule classesShouldNotUseAnySpringConditionalAnnotation =
    classesShouldNotUseAnySpringConditionalAnnotation();
}
