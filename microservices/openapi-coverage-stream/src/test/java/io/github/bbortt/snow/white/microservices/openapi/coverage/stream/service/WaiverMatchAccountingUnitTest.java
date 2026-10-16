/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.WaiverMatchAccounting.verifyEveryWaiverMatched;
import static java.time.LocalTime.NOON;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnmatchedWaiverException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@VerifiesCon(
  ConTraceables.CON_011_WAIVER_MATCHING_NO_FINDING_FAILS_THE_CALCULATION
)
class WaiverMatchAccountingUnitTest {

  private static final String PATH = "/pets/{petId}";
  private static final String METHOD = "GET";
  private static final String REASON = "the downstream cannot be made to fail";

  private static final LocalDate EXPIRES = LocalDate.of(2026, 6, 15);
  private static final Instant BEFORE_EXPIRY = EXPIRES.minusDays(7)
    .atTime(NOON)
    .toInstant(UTC);
  private static final Instant AFTER_EXPIRY = EXPIRES.plusDays(7)
    .atTime(NOON)
    .toInstant(UTC);

  private static final ApiInformation API_INFORMATION = ApiInformation.builder()
    .serviceName("pet-store")
    .apiName("pets")
    .apiVersion("1.0.0")
    .apiType(OPENAPI)
    .build();

  @Test
  void shouldPass_whenEveryWaiverAddressedAFinding() {
    var waivers = List.of(waiverOn("404", null), waiverOn("500", null));
    var results = Set.of(
      resultOf(RESPONSE_CODE_COVERAGE, findingOn("404"), findingOn("500"))
    );

    assertThatCode(() ->
      verifyEveryWaiverMatched(
        API_INFORMATION,
        results,
        new WaiverContext(waivers, BEFORE_EXPIRY)
      )
    ).doesNotThrowAnyException();
  }

  @Test
  void shouldFail_whenAWaiverAddressedNoFinding() {
    var waivers = List.of(waiverOn("418", null));
    var results = Set.of(resultOf(RESPONSE_CODE_COVERAGE, findingOn("404")));

    assertThatThrownBy(() ->
      verifyEveryWaiverMatched(
        API_INFORMATION,
        results,
        new WaiverContext(waivers, BEFORE_EXPIRY)
      )
    )
      .isInstanceOf(UnmatchedWaiverException.class)
      .hasMessageContaining("1 waiver(s) matched no finding")
      .hasMessageContaining("\"serviceName\": \"pet-store\"")
      .hasMessageContaining("\"apiName\": \"pets\"")
      .hasMessageContaining("\"apiVersion\": \"1.0.0\"")
      .hasMessageContaining("'GET /pets/{petId} (418)'");
  }

  /**
   * One miss among several matches is still a miss: the entry that placed itself says nothing about
   * the one that did not.
   */
  @Test
  void shouldFail_andNameOnlyTheWaiversThatMatchedNothing() {
    var waivers = List.of(
      waiverOn("404", null),
      waiverOn("418", null),
      waiverOn("503", ERROR_RESPONSE_CODE_COVERAGE)
    );
    var results = Set.of(resultOf(RESPONSE_CODE_COVERAGE, findingOn("404")));

    assertThatThrownBy(() ->
      verifyEveryWaiverMatched(
        API_INFORMATION,
        results,
        new WaiverContext(waivers, BEFORE_EXPIRY)
      )
    )
      .isInstanceOf(UnmatchedWaiverException.class)
      .hasMessageContaining("2 waiver(s) matched no finding")
      .hasMessageContaining("'GET /pets/{petId} (418)'")
      .hasMessageContaining(
        "'GET /pets/{petId} (503)' naming criterion 'ERROR_RESPONSE_CODE_COVERAGE'"
      )
      .hasMessageNotContaining("(404)");
  }

  /**
   * A waiver is placed by the API test and not by one criterion: matching anywhere among the
   * results is matching, even where the criterion that did emit the finding is not the one the
   * author had in mind.
   */
  @Test
  void shouldPass_whenAWaiverMatchedUnderOneCriterionAndNotAnother() {
    var waivers = List.of(waiverOn("404", null));
    var results = Set.of(
      resultOf(RESPONSE_CODE_COVERAGE, findingOn("404")),
      resultOf(PATH_COVERAGE, findingOn(null))
    );

    assertThatCode(() ->
      verifyEveryWaiverMatched(
        API_INFORMATION,
        results,
        new WaiverContext(waivers, BEFORE_EXPIRY)
      )
    ).doesNotThrowAnyException();
  }

  /**
   * The criterion a waiver names is not part of the question. A repository's waiver list is written
   * once and run against whichever gate the pipeline names, so an entry naming a criterion this
   * gate excluded is dormant rather than wrong - its target exists, which is all that is being
   * asserted.
   */
  @Test
  void shouldPass_whenTheWaiverNamedACriterionThatDidNotRun() {
    var waivers = List.of(waiverOn("404", ERROR_RESPONSE_CODE_COVERAGE));
    var results = Set.of(resultOf(RESPONSE_CODE_COVERAGE, findingOn("404")));

    assertThatCode(() ->
      verifyEveryWaiverMatched(
        API_INFORMATION,
        results,
        new WaiverContext(waivers, BEFORE_EXPIRY)
      )
    ).doesNotThrowAnyException();
  }

  /**
   * Expiry is a separate question, answered separately. An expired entry whose target still exists
   * passes here - and one whose target does not is reported for the structural problem, which is
   * the one that outlives the date.
   */
  @Test
  void shouldJudgeAnExpiredWaiverOnTheSameTerms() {
    var matching = new WaiverContext(
      List.of(waiverOn("404", null)),
      AFTER_EXPIRY
    );
    var unmatched = new WaiverContext(
      List.of(waiverOn("418", null)),
      AFTER_EXPIRY
    );
    var results = Set.of(resultOf(RESPONSE_CODE_COVERAGE, findingOn("404")));

    assertThatCode(() ->
      verifyEveryWaiverMatched(API_INFORMATION, results, matching)
    ).doesNotThrowAnyException();

    assertThatThrownBy(() ->
      verifyEveryWaiverMatched(API_INFORMATION, results, unmatched)
    )
      .isInstanceOf(UnmatchedWaiverException.class)
      .hasMessageContaining("'GET /pets/{petId} (418)'");
  }

  @Test
  void shouldPass_whenTheCalculationCarriedNoWaivers() {
    var results = Set.of(resultOf(RESPONSE_CODE_COVERAGE, findingOn("404")));

    assertThatCode(() ->
      verifyEveryWaiverMatched(API_INFORMATION, results, WaiverContext.none())
    ).doesNotThrowAnyException();
  }

  /**
   * No result at all means no criterion ran, and nothing was enumerated to be absent from. The
   * emptiness that makes an unmatched waiver meaningful elsewhere means the opposite here.
   */
  @Test
  void shouldPass_whenNoCriterionProducedAResult() {
    var waivers = List.of(waiverOn("418", null));

    assertThatCode(() ->
      verifyEveryWaiverMatched(
        API_INFORMATION,
        emptySet(),
        new WaiverContext(waivers, BEFORE_EXPIRY)
      )
    ).doesNotThrowAnyException();
  }

  /**
   * A criterion that ran and enumerated nothing is still a criterion that ran. It is not a reason
   * to excuse a waiver that places itself nowhere.
   */
  @Test
  void shouldFail_whenEveryResultIsFindingless() {
    var waivers = List.of(waiverOn("418", null));
    var results = Set.of(resultOf(RESPONSE_CODE_COVERAGE));

    assertThatThrownBy(() ->
      verifyEveryWaiverMatched(
        API_INFORMATION,
        results,
        new WaiverContext(waivers, BEFORE_EXPIRY)
      )
    ).isInstanceOf(UnmatchedWaiverException.class);
  }

  private static Waiver waiverOn(
    @Nullable String responseCode,
    @Nullable OpenApiCoverageCriteria criterion
  ) {
    return new Waiver(
      PATH,
      METHOD,
      responseCode,
      null,
      null,
      criterion != null ? criterion.name() : null,
      REASON,
      EXPIRES
    );
  }

  private static ApiTestFinding findingOn(@Nullable String responseCode) {
    return ApiTestFinding.builder()
      .specPointer("/paths/~1pets~1{petId}/get")
      .status(UNCOVERED)
      .httpPath(PATH)
      .httpMethod(METHOD)
      .responseCode(responseCode)
      .evidence(List.of())
      .build();
  }

  private static OpenApiTestResult resultOf(
    OpenApiCoverageCriteria criterion,
    ApiTestFinding... findings
  ) {
    return new OpenApiTestResult(
      criterion,
      BigDecimal.ZERO,
      Duration.ZERO,
      null,
      List.of(findings)
    );
  }
}
