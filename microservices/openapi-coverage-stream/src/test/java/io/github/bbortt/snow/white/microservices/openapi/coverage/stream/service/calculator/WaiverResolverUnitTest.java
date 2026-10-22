/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.WAIVED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.OPERATION_SUCCESS_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_ERROR_FIELDS_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.WaiverResolver.applyWaivers;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.WaiverResolver.describeExpiredWaivers;
import static java.time.ZoneOffset.UTC;
import static java.util.Locale.ROOT;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.event.dto.FindingStatus;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class WaiverResolverUnitTest {

  private static final String REASON =
    "mocked downstream cannot be made to fail";

  private static final LocalDate EXPIRES = LocalDate.of(2026, 6, 15);

  private static final Instant TRIGGERED_AT = EXPIRES.minusDays(7)
    .atTime(LocalTime.NOON)
    .toInstant(UTC);

  /**
   * A finding addressed the way a calculator emits one: the pointer identifies the node judged, and
   * the discriminators beside it are what a waiver selects through.
   */
  private static ApiTestFinding finding(
    FindingStatus status,
    String httpPath,
    String httpMethod,
    @Nullable String responseCode
  ) {
    return ApiTestFinding.builder()
      .specPointer("/paths/~1pets~1{petId}/get")
      .status(status)
      .httpPath(httpPath)
      .httpMethod(httpMethod)
      .responseCode(responseCode)
      .evidence(
        COVERED.equals(status)
          ? List.of(new FindingEvidence("traceId", null))
          : List.of()
      )
      .build();
  }

  private static Waiver waiver(
    String path,
    String method,
    @Nullable String responseCode,
    @Nullable String criterion
  ) {
    return new Waiver(
      path,
      method,
      responseCode,
      null,
      null,
      criterion,
      REASON,
      EXPIRES
    );
  }

  private static WaiverContext inForce(Waiver... waivers) {
    return new WaiverContext(Arrays.asList(waivers), TRIGGERED_AT);
  }

  private static WaiverContext expired(Waiver... waivers) {
    return new WaiverContext(
      Arrays.asList(waivers),
      EXPIRES.plusDays(1).atTime(LocalTime.NOON).toInstant(UTC)
    );
  }

  @Nested
  class ApplyWaiversTest {

    /**
     * The selection in its primary form: one waiver, one target, every other finding returned as
     * the calculator emitted it - including the one on the same operation under a different
     * response code, which is the near miss a looser comparison would also have taken.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS
    )
    void shouldWaiveOnlyTheAddressedTarget() {
      var addressed = finding(UNCOVERED, "/pets/{petId}", "GET", "500");
      var sameOperation = finding(UNCOVERED, "/pets/{petId}", "GET", "404");
      var otherOperation = finding(UNCOVERED, "/pets", "POST", "500");

      var result = applyWaivers(
        RESPONSE_CODE_COVERAGE,
        List.of(addressed, sameOperation, otherOperation),
        inForce(waiver("/pets/{petId}", "GET", "500", null))
      );

      assertThat(result)
        .extracting(
          ApiTestFinding::responseCode,
          ApiTestFinding::status,
          ApiTestFinding::waiverReason,
          ApiTestFinding::waiverExpiresOn
        )
        .containsExactly(
          tuple("500", WAIVED, REASON, EXPIRES),
          tuple("404", UNCOVERED, null, null),
          tuple("500", UNCOVERED, null, null)
        );
    }

    /**
     * The templated path is matched because the finding carries it templated - the calculator put
     * the specification's own spelling there. Nothing here expands {@code {petId}} or compares it
     * against a concrete path, which is the second templating implementation this spec exists to
     * prevent.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS
    )
    void shouldMatchATemplatedPathAsTheFindingSpellsIt() {
      var templated = finding(UNCOVERED, "/pets/{petId}", "GET", "404");

      assertThat(
        applyWaivers(
          RESPONSE_CODE_COVERAGE,
          List.of(templated),
          inForce(waiver("/pets/{petId}", "GET", "404", null))
        )
      )
        .extracting(ApiTestFinding::status)
        .containsExactly(WAIVED);

      // The concrete path a trace carried is not what the finding is addressed by, so a waiver
      // written against it addresses nothing.
      assertThat(
        applyWaivers(
          RESPONSE_CODE_COVERAGE,
          List.of(templated),
          inForce(waiver("/pets/42", "GET", "404", null))
        )
      )
        .extracting(ApiTestFinding::status)
        .containsExactly(UNCOVERED);
    }

    /**
     * {@code default} is a response code like any other here. The calculator resolved the wildcard
     * when it emitted the finding, so a waiver reaches it by naming the code the finding carries -
     * which is literally {@code default} - and the criterion's own wildcard rule is neither
     * reimplemented nor consulted.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS
    )
    void shouldMatchADefaultResponseCodeAsTheCalculatorResolvedIt() {
      var declaredAsDefault = finding(
        UNCOVERED,
        "/pets/{petId}",
        "GET",
        "default"
      );

      assertThat(
        applyWaivers(
          RESPONSE_CODE_COVERAGE,
          List.of(declaredAsDefault),
          inForce(waiver("/pets/{petId}", "GET", "default", null))
        )
      )
        .extracting(ApiTestFinding::status)
        .containsExactly(WAIVED);
    }

    /**
     * A covered target is waived like any other. If application depended on the status, the same
     * request against the same specification would apply the waiver on a day the telemetry missed
     * the target and skip it on a day it did not - the waived count moving with the traces, which
     * is precisely the non-determinism an analysis result may never show. It cannot flatter the
     * ratio either: a covered target leaving the fraction can only lower it.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS
    )
    void shouldWaiveATargetThatWasCoveredAnyway() {
      var covered = finding(COVERED, "/pets/{petId}", "GET", "404");

      var result = applyWaivers(
        RESPONSE_CODE_COVERAGE,
        List.of(covered),
        inForce(waiver("/pets/{petId}", "GET", "404", null))
      );

      assertThat(result)
        .singleElement()
        .satisfies(waived -> {
          assertThat(waived.status()).isEqualTo(WAIVED);
          assertThat(waived.waiverReason()).isEqualTo(REASON);
          // The evidence survives the rewrite: it is how a reviewer sees the waiver is no longer
          // needed.
          assertThat(waived.evidence()).isNotEmpty();
        });
    }

    /**
     * An omitted discriminator addresses the target that has none - the operation itself - rather
     * than standing for every response code under it. A waiver names one target, so reading an
     * omission as a wildcard would make the narrowest-looking entry in a waiver document the
     * broadest one in effect.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS
    )
    void shouldTreatAnOmittedDiscriminatorAsAddressingTheTargetWithoutOne() {
      var operation = finding(UNCOVERED, "/pets/{petId}", "GET", null);
      var responseCode = finding(UNCOVERED, "/pets/{petId}", "GET", "404");

      var result = applyWaivers(
        HTTP_METHOD_COVERAGE,
        List.of(operation, responseCode),
        inForce(waiver("/pets/{petId}", "GET", null, null))
      );

      assertThat(result)
        .extracting(ApiTestFinding::responseCode, ApiTestFinding::status)
        .containsExactly(tuple(null, WAIVED), tuple("404", UNCOVERED));
    }

    /**
     * The method is the one scalar normalised, because a finding carries it as the operation key
     * spells it - upper case - while an author types the case their specification uses.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS
    )
    void shouldMatchTheMethodRegardlessOfTheCaseTheWaiverWasWrittenIn() {
      var emitted = finding(UNCOVERED, "/pets/{petId}", "GET", "404");

      assertThat(
        applyWaivers(
          RESPONSE_CODE_COVERAGE,
          List.of(emitted),
          inForce(waiver("/pets/{petId}", "get", "404", null))
        )
      )
        .extracting(ApiTestFinding::status)
        .containsExactly(WAIVED);
    }

    /**
     * Two entries addressing one target are a duplicate in the request, not a composition, so the
     * first is quoted back - the one the author reading their own document would expect to see.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS
    )
    void shouldApplyTheFirstOfTwoWaiversAddressingOneTarget() {
      var first = new Waiver(
        "/pets/{petId}",
        "GET",
        "404",
        null,
        null,
        null,
        "the first reason",
        EXPIRES
      );
      var second = new Waiver(
        "/pets/{petId}",
        "GET",
        "404",
        null,
        null,
        null,
        "the second reason",
        EXPIRES
      );

      var result = applyWaivers(
        RESPONSE_CODE_COVERAGE,
        List.of(finding(UNCOVERED, "/pets/{petId}", "GET", "404")),
        inForce(first, second)
      );

      assertThat(result)
        .singleElement()
        .extracting(ApiTestFinding::waiverReason)
        .isEqualTo("the first reason");
    }

    @Test
    void shouldReturnTheFindingsUntouched_withoutAnyWaiver() {
      var findings = List.of(finding(UNCOVERED, "/pets/{petId}", "GET", "404"));

      assertThat(
        applyWaivers(RESPONSE_CODE_COVERAGE, findings, WaiverContext.none())
      ).isSameAs(findings);
    }

    /**
     * An expired waiver suppresses nothing. The target is judged on its evidence, which is what
     * turns the build red and sends the author back to the date they wrote.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldApplyNoExpiredWaiver() {
      var result = applyWaivers(
        RESPONSE_CODE_COVERAGE,
        List.of(finding(UNCOVERED, "/pets/{petId}", "GET", "404")),
        expired(waiver("/pets/{petId}", "GET", "404", null))
      );

      assertThat(result)
        .singleElement()
        .extracting(ApiTestFinding::status, ApiTestFinding::waiverReason)
        .containsExactly(UNCOVERED, null);
    }
  }

  @Nested
  class CriterionScopeTest {

    private static final List<ApiTestFinding> AT_ONE_POINTER = List.of(
      finding(UNCOVERED, "/pets/{petId}", "GET", "500")
    );

    private static FindingStatus statusUnder(
      OpenApiCoverageCriteria criterion,
      @Nullable String named
    ) {
      return applyWaivers(
        criterion,
        AT_ONE_POINTER,
        inForce(waiver("/pets/{petId}", "GET", "500", named))
      )
        .getFirst()
        .status();
    }

    /**
     * The broad claim: the response code is unprovokable, which is a property of the target rather
     * than of any one criterion, so every criterion judging it stops together - including
     * {@code REQUIRED_ERROR_FIELDS_COVERAGE}, which shares the pointer without containing or being
     * contained by the other two.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
    )
    void shouldWaiveATargetForEveryCriterion_whenNoCriterionIsNamed() {
      assertThat(
        List.of(
          ERROR_RESPONSE_CODE_COVERAGE,
          RESPONSE_CODE_COVERAGE,
          REQUIRED_ERROR_FIELDS_COVERAGE
        )
      ).allSatisfy(criterion ->
        assertThat(statusUnder(criterion, null)).isEqualTo(WAIVED)
      );
    }

    /**
     * The narrow claim reaches upward: naming the child concedes the parent's weaker statement
     * about the same target, so leaving the parent judging would make the waiver buy nothing while
     * reading as though it had. It reaches no sideways: {@code REQUIRED_ERROR_FIELDS_COVERAGE}
     * shares the pointer but makes a different claim about it, and keeps judging.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
    )
    void shouldWaiveATargetForTheNamedCriterionAndTheOnesContainingIt() {
      var named = ERROR_RESPONSE_CODE_COVERAGE.name();

      assertThat(statusUnder(ERROR_RESPONSE_CODE_COVERAGE, named)).isEqualTo(
        WAIVED
      );
      assertThat(statusUnder(RESPONSE_CODE_COVERAGE, named)).isEqualTo(WAIVED);
      assertThat(statusUnder(REQUIRED_ERROR_FIELDS_COVERAGE, named)).isEqualTo(
        UNCOVERED
      );
    }

    /**
     * Downward would be a privilege escalation in a mechanism whose whole purpose is to be narrow:
     * the parent's claim is weaker than the child's, so conceding it concedes nothing about the
     * stricter criteria underneath.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
    )
    void shouldNotWaiveATargetForACriterionContainedByTheNamedOne() {
      var named = RESPONSE_CODE_COVERAGE.name();

      assertThat(statusUnder(RESPONSE_CODE_COVERAGE, named)).isEqualTo(WAIVED);
      assertThat(statusUnder(ERROR_RESPONSE_CODE_COVERAGE, named)).isEqualTo(
        UNCOVERED
      );
    }

    /**
     * The strength edge carries the reach despite judging the same targets rather than fewer:
     * conceding that an operation cannot be made to succeed concedes that it cannot be called,
     * while conceding the call concedes nothing about the success.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
    )
    void shouldReachAlongTheStrengthEdgeUpwardOnly() {
      var strict = OPERATION_SUCCESS_COVERAGE.name();
      var weak = HTTP_METHOD_COVERAGE.name();

      assertThat(statusUnder(OPERATION_SUCCESS_COVERAGE, strict)).isEqualTo(
        WAIVED
      );
      assertThat(statusUnder(HTTP_METHOD_COVERAGE, strict)).isEqualTo(WAIVED);

      assertThat(statusUnder(HTTP_METHOD_COVERAGE, weak)).isEqualTo(WAIVED);
      assertThat(statusUnder(OPERATION_SUCCESS_COVERAGE, weak)).isEqualTo(
        UNCOVERED
      );
    }

    /**
     * Derived from the enum rather than listed by hand, so a criterion whose containment is
     * declared after this spec is covered the day it is declared - which is the staleness the
     * optional form of {@code criterion} exists to avoid in waiver documents, and would be no
     * better here.
     */
    @MethodSource(
      "io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.WaiverResolverUnitTest#containedCriteria"
    )
    @ParameterizedTest
    @VerifiesSw(
      SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
    )
    void shouldReachEveryDeclaredContainer(OpenApiCoverageCriteria contained) {
      var named = contained.name();

      assertThat(contained.getContainingCriteria())
        .isNotEmpty()
        .allSatisfy(container ->
          assertThat(statusUnder(container, named)).isEqualTo(WAIVED)
        );
    }

    /**
     * A criterion the gate excluded is not required to be running, so a waiver naming it is still
     * applied to the criteria that do judge its target. Nothing in this pass knows which criteria
     * the gate selected - the narrowing is decided by containment alone - and the match accounting
     * across every criterion of the API test is what answers an entry that addressed nothing
     * anywhere.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
    )
    void shouldApplyAWaiverNamingACriterionTheGateExcluded() {
      // ERROR_RESPONSE_CODE_COVERAGE is not among the criteria calculated here; its container is.
      assertThat(
        statusUnder(RESPONSE_CODE_COVERAGE, ERROR_RESPONSE_CODE_COVERAGE.name())
      ).isEqualTo(WAIVED);
    }

    /**
     * Case-insensitively, because a waiver document is written by hand and the criterion ids are
     * published in both cases.
     */
    @EnumSource
    @ParameterizedTest
    @VerifiesSw(
      SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
    )
    void shouldResolveEveryCriterionNameRegardlessOfCase(
      OpenApiCoverageCriteria criterion
    ) {
      assertThat(
        statusUnder(criterion, criterion.name().toLowerCase(ROOT))
      ).isEqualTo(WAIVED);
    }

    /**
     * A name no criterion carries addresses nothing here rather than failing the calculation from
     * inside a calculator: the request that carried the typo is where it can still be answered
     * usefully, and the match accounting across the whole API test is what reports an entry that
     * matched nothing anywhere.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
    )
    void shouldWaiveNothing_whenTheNamedCriterionIsNotOneOfOurs() {
      assertThat(
        statusUnder(RESPONSE_CODE_COVERAGE, "PATH_COVERAGE_TYPO")
      ).isEqualTo(UNCOVERED);
    }
  }

  @Nested
  class DescribeExpiredWaiversTest {

    /**
     * Named rather than dropped, with the date, so a build that turns red says why instead of
     * sending its team to debug telemetry that is working.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldNameTheExpiredWaiverAndItsDate() {
      var result = describeExpiredWaivers(
        RESPONSE_CODE_COVERAGE,
        List.of(finding(UNCOVERED, "/pets/{petId}", "GET", "500")),
        expired(waiver("/pets/{petId}", "get", "500", null))
      );

      assertThat(result)
        .contains("GET /pets/{petId}")
        .contains("(500)")
        .contains(EXPIRES.toString())
        .contains(REASON);
    }

    /**
     * Only for the criteria the waiver would have addressed. An expiry note on a criterion whose
     * targets the waiver never named would read as an explanation of a verdict it had nothing to
     * do with.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldSayNothing_whenNoFindingOfThisCriterionWasAddressed() {
      var result = describeExpiredWaivers(
        RESPONSE_CODE_COVERAGE,
        List.of(finding(UNCOVERED, "/pets", "POST", "201")),
        expired(waiver("/pets/{petId}", "GET", "500", null))
      );

      assertThat(result).isNull();
    }

    @Test
    void shouldSayNothing_withoutAnyExpiredWaiver() {
      assertThat(
        describeExpiredWaivers(
          RESPONSE_CODE_COVERAGE,
          List.of(finding(UNCOVERED, "/pets/{petId}", "GET", "500")),
          inForce(waiver("/pets/{petId}", "GET", "500", null))
        )
      ).isNull();
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldNameEveryExpiredWaiverThatAddressedThisCriterion() {
      var result = describeExpiredWaivers(
        RESPONSE_CODE_COVERAGE,
        List.of(
          finding(UNCOVERED, "/pets/{petId}", "GET", "500"),
          finding(UNCOVERED, "/pets", "POST", "503")
        ),
        expired(
          waiver("/pets/{petId}", "GET", "500", null),
          waiver("/pets", "POST", "503", null)
        )
      );

      assertThat(result).contains("/pets/{petId}").contains("/pets");
    }
  }

  static List<OpenApiCoverageCriteria> containedCriteria() {
    return Arrays.stream(OpenApiCoverageCriteria.values())
      .filter(criterion -> nonNull(criterion.getContainedBy()))
      .toList();
  }
}
