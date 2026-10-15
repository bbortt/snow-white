/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.Main;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageCalculator;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import java.util.Set;

/**
 * The three waiver claims that are structural rather than behavioural: who may apply a waiver, what
 * the resolver is allowed to look at, and what it is not allowed to ask the time of.
 * <p>
 * Each of these is currently also enforced by a compiler or a modifier - {@code calculate} is
 * {@code final}, {@link WaiverResolver} is package-private. The rules exist because those are one
 * keyword away from gone, and the keyword looks harmless in a diff.
 */
@AnalyzeClasses(
  packagesOf = Main.class,
  importOptions = DoNotIncludeTests.class
)
class WaiverStructureUnitTest {

  /**
   * Reading the clock rather than being handed the instant, in any of the shapes the JDK offers it.
   * Matched on the method name against a {@code java.*} owner, so a form nobody thought of here -
   * {@code Clock.systemUTC()}, a {@code ZoneId.systemDefault()} smuggled into a conversion - is
   * caught by the same rule as {@code Instant.now()}.
   */
  private static final DescribedPredicate<JavaMethodCall> READS_AN_AMBIENT_CLOCK =
    new DescribedPredicate<>("read an ambient clock") {
      private static final Set<String> CLOCK_READS = Set.of(
        "now",
        "currentTimeMillis",
        "nanoTime",
        "systemDefault",
        "systemDefaultZone",
        "systemUTC"
      );

      @Override
      public boolean test(JavaMethodCall call) {
        var target = call.getTarget();

        return (
          CLOCK_READS.contains(target.getName()) &&
          target.getOwner().getPackageName().startsWith("java.")
        );
      }
    };

  /**
   * The waiver pass sits in {@code calculate}, which is where the one coverage derivation sits. A
   * subclass that declared its own {@code calculate} would decide for itself whether waivers apply
   * to it - and a criterion quietly exempt from the mechanism is worse than no mechanism, because
   * the report still says {@code WAIVED} everywhere else.
   */
  @ArchTest
  static final ArchRule onlyTheBaseClassDeclaresCalculate = methods()
    .that()
    .haveName("calculate")
    .and()
    .areDeclaredInClassesThat()
    .areAssignableTo(OpenApiCoverageCalculator.class)
    .and()
    .areDeclaredInClassesThat()
    .areNotInterfaces()
    .should()
    .beDeclaredIn(AbstractOpenApiCoverageCalculator.class)
    .because(
      "the waiver pass and the one coverage derivation both live in the base class's final calculate(), and a subclass declaring its own would opt itself out of each"
    );

  /**
   * A calculator states what it found; it never knows that a finding was waived. Keeping waivers out
   * of every subclass is what makes the pass uniform: there is one place that applies them, so there
   * is no per-criterion interpretation of what a waiver means.
   */
  @ArchTest
  static final ArchRule noCalculatorKnowsAboutWaivers = noClasses()
    .that()
    .areAssignableTo(OpenApiCoverageCalculator.class)
    .and(
      not(
        belongToAnyOf(
          OpenApiCoverageCalculator.class,
          AbstractOpenApiCoverageCalculator.class
        )
      )
    )
    .should()
    .dependOnClassesThat()
    .belongToAnyOf(Waiver.class, WaiverContext.class, WaiverResolver.class)
    .because(
      "a calculator emits findings and never interprets a waiver; the pass is applied once, for every criterion alike, by the base class"
    );

  /**
   * The resolver selects out of findings a calculator already emitted, and the specification is not
   * among the things it is allowed to look at. Reading it would mean re-deriving a target - path
   * templating, {@code default} response codes, parameter tokens - and a second implementation of
   * those rules is a second answer to what a waiver addresses.
   */
  @ArchTest
  static final ArchRule theResolverReadsNoSpecification = noClasses()
    .that()
    .belongToAnyOf(WaiverResolver.class)
    .should()
    .dependOnClassesThat()
    .resideInAnyPackage("io.swagger.v3.oas.models..")
    .because(
      "a waiver selects from the findings the calculators emitted and inherits their target rules; resolving one against the document would reimplement those rules a second time"
    );

  /**
   * Nothing outside the calculators resolves a waiver. The resolver is package-private, which says
   * the same thing to the compiler; this says it to whoever is about to widen that.
   */
  @ArchTest
  static final ArchRule nothingOutsideTheCalculatorsResolvesAWaiver =
    noClasses()
      .that()
      .resideOutsideOfPackage(
        "io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator"
      )
      .should()
      .dependOnClassesThat()
      .belongToAnyOf(WaiverResolver.class)
      .because(
        "the waiver pass belongs to the calculators, between their findings and the one derivation, and a second caller would be a second pass"
      );

  /**
   * Expiry is judged against the instant the calculation was triggered at, which travels with the
   * request. A clock read anywhere on this path is the whole failure: the same report replayed a
   * month later would answer differently, and nothing in its output would say why.
   */
  @ArchTest
  static final ArchRule theWaiverPathReadsNoAmbientClock = noClasses()
    .that()
    .belongToAnyOf(WaiverContext.class, WaiverResolver.class)
    .should()
    .callMethodWhere(READS_AN_AMBIENT_CLOCK)
    .because(
      "a waiver's expiry is judged against the triggering instant the request carried, so that a replay reproduces the verdict rather than re-deciding it against today"
    );
}
