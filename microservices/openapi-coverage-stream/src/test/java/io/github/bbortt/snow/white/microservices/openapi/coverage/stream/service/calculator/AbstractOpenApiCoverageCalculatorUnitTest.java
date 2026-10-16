/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.WAIVED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalTime.NOON;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.nCopies;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.event.dto.FindingStatus;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import io.swagger.v3.oas.models.Operation;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AbstractOpenApiCoverageCalculatorUnitTest {

  private static final Map<String, Operation> NO_OPERATIONS = emptyMap();
  private static final Map<String, List<OpenTelemetryData>> NO_TELEMETRY =
    emptyMap();

  private static final String PATH = "/api";
  private static final String METHOD = "GET";
  private static final String REASON = "the upstream cannot be provoked";
  private static final LocalDate EXPIRES = LocalDate.of(2026, 6, 15);

  /**
   * A waiver addressing one of the fixture's targets, told apart from its neighbours by the
   * response code - the same discriminator a calculator puts on the findings it emits.
   */
  private static Waiver waiverOn(@Nullable String responseCode) {
    return new Waiver(
      PATH,
      METHOD,
      responseCode,
      null,
      null,
      null,
      REASON,
      EXPIRES
    );
  }

  private static WaiverContext inForce(Waiver waiver) {
    return new WaiverContext(
      List.of(waiver),
      EXPIRES.minusDays(1).atTime(NOON).toInstant(UTC)
    );
  }

  private static WaiverContext expired(Waiver waiver) {
    return new WaiverContext(
      List.of(waiver),
      EXPIRES.plusDays(1).atTime(NOON).toInstant(UTC)
    );
  }

  private static BigDecimal deriveCoverageOf(FindingStatus... statuses) {
    return deriveCoverageOf(stream(statuses).map(Fixture::finding).toList());
  }

  private static BigDecimal deriveCoverageOf(List<ApiTestFinding> findings) {
    return new Fixture(findings)
      .calculate(NO_OPERATIONS, NO_TELEMETRY, WaiverContext.none())
      .coverage();
  }

  private static BigDecimal ratioOf(double value) {
    return BigDecimal.valueOf(value).setScale(2, HALF_UP);
  }

  @Nested
  class CalculateTest {

    @Test
    @VerifiesArch(ArchTraceables.ARCH_010_COVERAGE_DERIVED_FROM_FINDINGS)
    void shouldDeriveRatioAsCoveredOverJudgedFindings() {
      var result = deriveCoverageOf(COVERED, COVERED, COVERED, UNCOVERED);

      assertThat(result).isEqualTo(ratioOf(0.75));
    }

    @Test
    @VerifiesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
    void shouldLeaveNotApplicableFindingsOutOfBothSidesOfTheFraction() {
      var withInapplicable = deriveCoverageOf(
        COVERED,
        NOT_APPLICABLE,
        UNCOVERED,
        NOT_APPLICABLE
      );

      assertThat(withInapplicable)
        .isEqualTo(ratioOf(0.5))
        .isEqualTo(deriveCoverageOf(COVERED, UNCOVERED));
    }

    @Test
    void shouldReportVacuousTruthWhenEveryFindingIsNotApplicable() {
      var result = deriveCoverageOf(NOT_APPLICABLE, NOT_APPLICABLE);

      assertThat(result).isEqualTo(ratioOf(1.0));
    }

    @Test
    void shouldReportVacuousTruthWhenThereIsNoTargetAtAll() {
      var result = deriveCoverageOf();

      assertThat(result).isEqualTo(ratioOf(1.0));
    }

    @Test
    void shouldReportZeroWhenNoJudgedFindingIsCovered() {
      var result = deriveCoverageOf(UNCOVERED, UNCOVERED, NOT_APPLICABLE);

      assertThat(result).isEqualTo(ratioOf(0.0));
    }

    @Test
    void shouldClampANearCompleteRatioBelowOne() {
      var findings = IntStream.range(0, 1000)
        .mapToObj(index -> Fixture.finding(index == 0 ? UNCOVERED : COVERED))
        .toList();

      var result = deriveCoverageOf(findings);

      assertThat(result).isEqualTo(ratioOf(0.99));
    }

    @Test
    void shouldRoundHalfUpToTwoDecimals() {
      var result = deriveCoverageOf(COVERED, COVERED, UNCOVERED);

      assertThat(result).isEqualTo(ratioOf(0.67));
    }

    @Test
    void shouldReportTheSupportedCriterionAndAMeasuredDuration() {
      var result = new Fixture(List.of(Fixture.finding(COVERED))).calculate(
        NO_OPERATIONS,
        NO_TELEMETRY,
        WaiverContext.none()
      );

      assertThat(result.openApiCriteria()).isEqualTo(PATH_COVERAGE);
      assertThat(result.duration()).isNotNull();
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_010_COVERAGE_DERIVED_FROM_FINDINGS)
    void shouldHandTheWholeCalculationToTheMessageHook() {
      var findings = List.of(
        Fixture.finding(COVERED),
        Fixture.finding(NOT_APPLICABLE)
      );
      var fixture = new Fixture(findings);

      fixture.calculate(NO_OPERATIONS, NO_TELEMETRY, WaiverContext.none());

      assertThat(fixture.lastCalculation).isEqualTo(
        new AbstractOpenApiCoverageCalculator.Calculation(
          NO_OPERATIONS,
          NO_TELEMETRY,
          findings
        )
      );
    }

    @Test
    void shouldDelegateAdditionalInformationToTheCalculator() {
      var result = new Fixture(List.of(Fixture.finding(UNCOVERED))).calculate(
        NO_OPERATIONS,
        NO_TELEMETRY,
        WaiverContext.none()
      );

      assertThat(result.additionalInformation()).isEqualTo("1 finding");
    }
  }

  @Nested
  class WaiverPassTest {

    private static OpenApiTestResult calculate(
      List<ApiTestFinding> findings,
      WaiverContext waiverContext
    ) {
      return new Fixture(findings).calculate(
        NO_OPERATIONS,
        NO_TELEMETRY,
        waiverContext
      );
    }

    /**
     * The pass runs ahead of the one derivation, so a waived target is outside both sides of the
     * fraction and the ratio is the one the remaining targets imply on their own. Stated against
     * those remaining findings rather than against a literal, so it survives whatever the ratio
     * rules do next.
     */
    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_017_WAIVERS_APPLIED_BEFORE_THE_ONE_DERIVATION
    )
    void shouldLeaveAWaivedTargetOutOfBothSidesOfTheFraction() {
      var result = calculate(
        List.of(
          Fixture.finding(COVERED, "200"),
          Fixture.finding(UNCOVERED, "404"),
          Fixture.finding(UNCOVERED, "500")
        ),
        inForce(waiverOn("500"))
      );

      assertThat(result.coverage())
        .isEqualTo(ratioOf(0.5))
        .isEqualTo(deriveCoverageOf(COVERED, UNCOVERED));

      // Out of the judgement, not out of the report: the finding is still there, saying who took
      // it out.
      assertThat(result.findings())
        .extracting(
          ApiTestFinding::responseCode,
          ApiTestFinding::status,
          ApiTestFinding::waiverReason,
          ApiTestFinding::waiverExpiresOn
        )
        .containsExactly(
          tuple("200", COVERED, null, null),
          tuple("404", UNCOVERED, null, null),
          tuple("500", WAIVED, REASON, EXPIRES)
        );
    }

    /**
     * The pass changes the statuses it matched and the numbers derived from them, and nothing else.
     * Compared against the same calculation run with no waiver at all, so a pass that dropped a
     * finding, reordered them, renamed the criterion or rewrote an unaddressed target fails here
     * rather than in somebody's report.
     */
    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_017_WAIVERS_APPLIED_BEFORE_THE_ONE_DERIVATION
    )
    void shouldChangeNothingBesideTheMatchedStatusesAndWhatDerivesFromThem() {
      var findings = List.of(
        Fixture.finding(COVERED, "200"),
        Fixture.finding(UNCOVERED, "404")
      );

      var withoutPass = calculate(findings, WaiverContext.none());
      var withPass = calculate(findings, inForce(waiverOn("404")));

      assertThat(withPass)
        .usingRecursiveComparison()
        .ignoringFields("coverage", "duration", "findings")
        .isEqualTo(withoutPass);

      assertThat(withPass.findings()).zipSatisfy(
        withoutPass.findings(),
        (waived, asEmitted) ->
          assertThat(waived)
            .usingRecursiveComparison()
            .ignoringFields("status", "waiverReason", "waiverExpiresOn")
            .isEqualTo(asEmitted)
      );

      assertThat(withPass.findings())
        .extracting(ApiTestFinding::status)
        .containsExactly(COVERED, WAIVED);
    }

    /**
     * The message hook is handed the findings the pass left rather than the ones the subclass
     * returned, so a message rendered from them describes the same verdicts the result publishes.
     */
    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_017_WAIVERS_APPLIED_BEFORE_THE_ONE_DERIVATION
    )
    void shouldHandTheWaivedFindingsToTheMessageHook() {
      var fixture = new Fixture(List.of(Fixture.finding(UNCOVERED, "500")));

      fixture.calculate(NO_OPERATIONS, NO_TELEMETRY, inForce(waiverOn("500")));

      assertThat(requireNonNull(fixture.lastCalculation).findings())
        .extracting(ApiTestFinding::status)
        .containsExactly(WAIVED);
    }

    /**
     * Waiving a target the telemetry already covered can only lower the ratio. That is why the pass
     * does not consult a finding's status: skipping covered targets would make the same request
     * answer differently as traces came and went, and the one thing that cannot happen - a waiver
     * inflating a score - is arithmetic rather than policy.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS
    )
    void shouldLowerTheRatioByWaivingACoveredTarget() {
      var findings = List.of(
        Fixture.finding(COVERED, "200"),
        Fixture.finding(COVERED, "404"),
        Fixture.finding(UNCOVERED, "500")
      );

      var withoutPass = calculate(findings, WaiverContext.none());
      var withPass = calculate(findings, inForce(waiverOn("200")));

      assertThat(withPass.coverage())
        .isEqualTo(ratioOf(0.5))
        .isLessThan(withoutPass.coverage());
    }

    static Stream<Arguments> coveredAndUncoveredCounts() {
      var combinations = Stream.<Arguments>builder();

      for (var covered = 1; covered <= 4; covered++) {
        for (var uncovered = 0; uncovered <= 4; uncovered++) {
          combinations.add(arguments(covered, uncovered));
        }
      }

      return combinations.build();
    }

    /**
     * The same claim over every shape the fixture can take rather than one: waiving a covered target
     * never raises the ratio, whatever the other targets said - including at the two ends, where
     * everything was covered and where the waiver leaves nothing judged at all.
     */
    @MethodSource("coveredAndUncoveredCounts")
    @ParameterizedTest
    @VerifiesSw(
      SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS
    )
    void shouldNeverRaiseTheRatioByWaivingACoveredTarget(
      int covered,
      int uncovered
    ) {
      var findings = IntStream.range(0, covered + uncovered)
        .mapToObj(index ->
          Fixture.finding(
            index < covered ? COVERED : UNCOVERED,
            String.valueOf(index)
          )
        )
        .toList();

      var withoutPass = calculate(findings, WaiverContext.none());
      var withPass = calculate(findings, inForce(waiverOn("0")));

      assertThat(withPass.coverage()).isLessThanOrEqualTo(
        withoutPass.coverage()
      );
    }

    /**
     * An expired waiver suppresses nothing - the target is judged on its evidence - and the result
     * says so, ahead of whatever the criterion itself had to report. A criterion that simply starts
     * failing with no mention of the waiver sends its team debugging telemetry that is working.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldJudgeTheTargetAndNameTheWaiver_whenItHadAlreadyExpired() {
      var result = calculate(
        List.of(Fixture.finding(UNCOVERED, "500")),
        expired(waiverOn("500"))
      );

      assertThat(result.coverage()).isEqualTo(ratioOf(0.0));
      assertThat(result.findings())
        .extracting(ApiTestFinding::status, ApiTestFinding::waiverReason)
        .containsExactly(tuple(UNCOVERED, null));

      assertThat(result.additionalInformation())
        .startsWith("Waiver on 'GET /api (500)' expired on " + EXPIRES)
        .contains(REASON)
        .endsWith("1 finding");
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldMentionNoExpiry_whenTheWaiverWasStillInForce() {
      var result = calculate(
        List.of(Fixture.finding(UNCOVERED, "500")),
        inForce(waiverOn("500"))
      );

      assertThat(result.additionalInformation()).isEqualTo("1 finding");
    }

    /**
     * And not for a criterion whose targets it never named: an expiry note beside a verdict the
     * waiver had nothing to do with would read as an explanation of it.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldMentionNoExpiry_whenTheWaiverAddressedNoFindingHere() {
      var result = calculate(
        List.of(Fixture.finding(UNCOVERED, "404")),
        expired(waiverOn("500"))
      );

      assertThat(result.additionalInformation()).isEqualTo("1 finding");
    }
  }

  @Nested
  class CoverageAgreesWithItsFindingsTest {

    /**
     * Every combination of covered, uncovered, inapplicable and waived counts up to a handful each,
     * plus the three boundaries the ratio rules single out: nothing judged because nothing was
     * applicable, nothing judged because everything was waived, and a ratio close enough to
     * complete that it has to be clamped.
     */
    static Stream<Arguments> findingCounts() {
      var combinations = Stream.<Arguments>builder();

      for (var covered = 0; covered <= 4; covered++) {
        for (var uncovered = 0; uncovered <= 4; uncovered++) {
          for (var notApplicable = 0; notApplicable <= 2; notApplicable++) {
            for (var waived = 0; waived <= 2; waived++) {
              combinations.add(
                arguments(covered, uncovered, notApplicable, waived)
              );
            }
          }
        }
      }

      combinations.add(arguments(999, 1, 0, 0));
      combinations.add(arguments(0, 0, 3, 0));
      combinations.add(arguments(0, 0, 0, 3));

      return combinations.build();
    }

    @MethodSource("findingCounts")
    @ParameterizedTest
    @VerifiesCon(ConTraceables.CON_009_COVERAGE_AGREES_WITH_FINDINGS)
    void shouldEqualTheRatioItsOwnFindingsImply(
      int covered,
      int uncovered,
      int notApplicable,
      int waived
    ) {
      var findings = Stream.of(
        nCopies(covered, COVERED),
        nCopies(uncovered, UNCOVERED),
        nCopies(notApplicable, NOT_APPLICABLE),
        nCopies(waived, WAIVED)
      )
        .flatMap(List::stream)
        .map(Fixture::finding)
        .toList();

      var result = new Fixture(findings).calculate(
        NO_OPERATIONS,
        NO_TELEMETRY,
        WaiverContext.none()
      );

      assertThat(result.coverage()).isEqualTo(expectedRatio(result.findings()));
    }

    @Test
    @VerifiesCon(ConTraceables.CON_009_COVERAGE_AGREES_WITH_FINDINGS)
    void shouldNotFlagAResultThatCarriesNoFindingsAtAll() {
      // A result written before the findings migration keeps the ratio it was calculated with and
      // an empty collection beside it. The exemption is what this asserts: the pair is absent, so
      // there is nothing for the ratio to disagree with.
      var preMigrationResult = new OpenApiTestResult(
        PATH_COVERAGE,
        ratioOf(0.42),
        Duration.ofSeconds(1)
      );

      assertThat(preMigrationResult.findings()).isEmpty();
      assertThat(expectedRatio(preMigrationResult.findings())).isEqualTo(
        ratioOf(1.0)
      );
      assertThat(preMigrationResult.coverage())
        .isEqualTo(ratioOf(0.42))
        .isNotEqualTo(expectedRatio(preMigrationResult.findings()));
    }

    /**
     * The ratio rules spelled out again, independently of the implementation under test: covered
     * over covered plus uncovered, inapplicable outside both sides, two decimals half up, clamped
     * below one unless every judged target is covered, and one when nothing was judged.
     */
    private static BigDecimal expectedRatio(List<ApiTestFinding> findings) {
      var covered = findings
        .stream()
        .filter(finding -> COVERED.equals(finding.status()))
        .count();
      var judged = findings
        .stream()
        .filter(
          finding ->
            !NOT_APPLICABLE.equals(finding.status()) &&
            !WAIVED.equals(finding.status())
        )
        .count();

      if (judged == 0) {
        return ratioOf(1.0);
      }

      var ratio = BigDecimal.valueOf(covered).divide(
        BigDecimal.valueOf(judged),
        2,
        HALF_UP
      );

      return covered != judged && ratio.compareTo(BigDecimal.ONE) == 0
        ? new BigDecimal("0.99")
        : ratio;
    }
  }

  private static final class Fixture extends AbstractOpenApiCoverageCalculator {

    private final List<ApiTestFinding> findings;

    private @Nullable Calculation lastCalculation;

    private Fixture(List<ApiTestFinding> findings) {
      this.findings = findings;
    }

    private static ApiTestFinding finding(FindingStatus status) {
      return finding(status, null);
    }

    /**
     * A finding addressed the way a calculator emits one, so a waiver written against this fixture
     * selects through the same discriminators it would in production. The response code is what
     * tells two of these apart.
     */
    private static ApiTestFinding finding(
      FindingStatus status,
      @Nullable String responseCode
    ) {
      return ApiTestFinding.builder()
        .specPointer("/paths/~1api")
        .status(status)
        .httpPath(PATH)
        .httpMethod(METHOD)
        .responseCode(responseCode)
        .evidence(
          COVERED.equals(status)
            ? List.of(new FindingEvidence("traceId", null))
            : List.of()
        )
        .build();
    }

    @Override
    protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
      return PATH_COVERAGE;
    }

    @Override
    protected @NonNull List<ApiTestFinding> calculateFindings(
      Map<String, Operation> pathToOpenAPIOperationMap,
      Map<String, List<OpenTelemetryData>> pathToTelemetryMap
    ) {
      return findings;
    }

    @Override
    protected @Nullable String getAdditionalInformationOrNull(
      @NonNull Calculation calculation
    ) {
      this.lastCalculation = calculation;
      return calculation.findings().size() + " finding";
    }
  }
}
