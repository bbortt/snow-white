/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.LOOKBACK_WINDOW;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.defaultApiInformation;
import static java.time.LocalTime.NOON;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnmatchedWaiverException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * A waiver that addresses nothing stops the calculation, over the real criteria rather than a
 * stand-in for them.
 * <p>
 * {@code WaiverMatchAccountingUnitTest} states the rule against findings it writes itself. What it
 * cannot say is that a waiver an author would call correct actually lands on a finding the real
 * calculators emit - the check is only as trustworthy as the agreement between how a waiver spells
 * its target and how eleven criteria spell theirs, and a disagreement in either direction turns
 * this guarantee into a build that fails on correct input.
 */
class UnmatchedWaiverFailsTheCalculationIT
  extends AbstractOpenApiCoverageServiceIT
{

  private static final String PATH = "/pets/{petId}";
  private static final String METHOD = "GET";

  private static final String REASON =
    "the mocked downstream cannot be made to fail";
  private static final LocalDate EXPIRES = LocalDate.of(2026, 6, 15);
  private static final Instant TRIGGERED_AT = EXPIRES.minusDays(7)
    .atTime(NOON)
    .toInstant(UTC);

  @Autowired
  private OpenApiCoverageService openApiCoverageService;

  /**
   * A path no criterion enumerated, because the document does not declare it. Nothing in the
   * calculation would otherwise notice - the entry would simply never be consulted.
   */
  @Test
  @VerifiesCon(
    ConTraceables.CON_011_WAIVER_MATCHING_NO_FINDING_FAILS_THE_CALCULATION
  )
  void shouldFail_whenTheWaiverNamesAPathTheSpecificationDoesNotDeclare() {
    var context = contextWith(
      specificationDeclaring("500"),
      new Waiver("/pets", METHOD, null, null, null, null, REASON, EXPIRES)
    );

    assertThatThrownBy(() -> openApiCoverageService.calculateCoverage(context))
      .isInstanceOf(UnmatchedWaiverException.class)
      .hasMessageContaining("'GET /pets'")
      .hasMessageContaining("\"apiName\": \"apiName\"");
  }

  /**
   * The likelier mistake: the operation is real and the discriminator is not. A response code
   * nobody documented is one nobody judged.
   */
  @Test
  @VerifiesCon(
    ConTraceables.CON_011_WAIVER_MATCHING_NO_FINDING_FAILS_THE_CALCULATION
  )
  void shouldFail_whenTheWaiverNamesAResponseCodeTheSpecificationDoesNotDeclare() {
    var context = contextWith(
      specificationDeclaring("500"),
      waiverOn("418", null)
    );

    assertThatThrownBy(() -> openApiCoverageService.calculateCoverage(context))
      .isInstanceOf(UnmatchedWaiverException.class)
      .hasMessageContaining("'GET /pets/{petId} (418)'");
  }

  /**
   * Matching under one criterion is matching. Several criteria judge this {@code 404} and most of
   * the eleven judge nothing of the sort, so a check that wanted every criterion to place the entry
   * would fail here - on an entry that is plainly correct.
   */
  @Test
  @VerifiesCon(
    ConTraceables.CON_011_WAIVER_MATCHING_NO_FINDING_FAILS_THE_CALCULATION
  )
  void shouldPass_whenTheWaiverMatchesUnderAtLeastOneCriterion() {
    var context = contextWith(
      specificationDeclaring("500"),
      waiverOn("404", null)
    );

    assertThatCode(() -> {
      var results = openApiCoverageService.calculateCoverage(context);
      assertThat(results).isNotEmpty();
    }).doesNotThrowAnyException();
  }

  /**
   * The criterion the entry named judges parameters and will never emit a finding for a response
   * code. Its target exists all the same, and that is the whole of what is being asserted - a gate
   * the pipeline happens to run decides which criteria are consulted, not which entries are
   * well-formed.
   */
  @Test
  @VerifiesCon(
    ConTraceables.CON_011_WAIVER_MATCHING_NO_FINDING_FAILS_THE_CALCULATION
  )
  void shouldPass_whenTheWaiverNamesACriterionThatNeverJudgesItsTarget() {
    var context = contextWith(
      specificationDeclaring("500"),
      waiverOn("404", REQUIRED_PARAMETER_COVERAGE)
    );

    assertThatCode(() ->
      openApiCoverageService.calculateCoverage(context)
    ).doesNotThrowAnyException();
  }

  /**
   * The consequence of checking per API test: one waiver list, two versions of the same API, and
   * the version that still declares the target is unaffected by the one that dropped it. A wildcard
   * entry is the only one whose target can disappear without its author editing anything, and this
   * is what it costs them - a report against the version that lost it, and nothing more.
   */
  @Test
  @VerifiesCon(
    ConTraceables.CON_011_WAIVER_MATCHING_NO_FINDING_FAILS_THE_CALCULATION
  )
  void shouldFailOnlyTheVersionThatDroppedTheTarget() {
    var waiver = waiverOn("500", null);

    assertThatCode(() ->
      openApiCoverageService.calculateCoverage(
        contextWith(specificationDeclaring("500"), waiver)
      )
    ).doesNotThrowAnyException();

    assertThatThrownBy(() ->
      openApiCoverageService.calculateCoverage(
        contextWith(specificationDeclaring(null), waiver)
      )
    )
      .isInstanceOf(UnmatchedWaiverException.class)
      .hasMessageContaining("'GET /pets/{petId} (500)'");
  }

  private static OpenApiTestContext contextWith(
    OpenAPI openAPI,
    Waiver waiver
  ) {
    return new OpenApiTestContext(
      defaultApiInformation(),
      openAPI,
      LOOKBACK_WINDOW,
      emptySet(),
      new WaiverContext(List.of(waiver), TRIGGERED_AT)
    ).withOpenTelemetryData(telemetry());
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
      nonNull(criterion) ? criterion.name() : null,
      REASON,
      EXPIRES
    );
  }

  /**
   * One operation, documenting a {@code 200}, a {@code 404} and - where the caller asks for it - one
   * further response code. Dropping that last one is how a target disappears between two versions
   * of the same API without anything else about the document changing.
   */
  private static OpenAPI specificationDeclaring(
    @Nullable String furtherResponseCode
  ) {
    var jsonContent = new Content().addMediaType(
      "application/json",
      new MediaType()
    );

    var responses = new ApiResponses()
      .addApiResponse("200", new ApiResponse().content(jsonContent))
      .addApiResponse("404", new ApiResponse().content(jsonContent));

    if (nonNull(furtherResponseCode)) {
      responses.addApiResponse(
        furtherResponseCode,
        new ApiResponse().content(jsonContent)
      );
    }

    var readPet = new Operation()
      .operationId("readPet")
      .parameters(List.of(new Parameter().in("path").name("petId")))
      .responses(responses);

    var paths = new Paths();
    paths.addPathItem(PATH, new PathItem().get(readPet));

    return new OpenAPI().paths(paths);
  }

  /**
   * A {@code 200} only, so every other documented code is uncovered on its own merits - which is
   * what makes a waiver on one of them match a finding that exists rather than one the telemetry
   * happened to erase.
   */
  private static Set<OpenTelemetryData> telemetry() {
    ObjectNode attributes = JsonMapper.shared().createObjectNode();
    attributes.put("http.request.method", METHOD);
    attributes.put("url.path", "/pets/42");
    attributes.put("http.response.status_code", "200");

    return Set.of(new OpenTelemetryData("traceId-span", "traceId", attributes));
  }
}
