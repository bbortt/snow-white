/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.WAIVED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.OPERATION_SUCCESS_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_ERROR_FIELDS_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_CODE_COVERAGE;
import static java.time.LocalTime.NOON;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingStatus;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * How far a waiver's named criterion reaches, over the real calculators rather than a stand-in for
 * them.
 * <p>
 * {@code WaiverResolverUnitTest} already walks the whole containment map; what it cannot say is
 * that a waiver written against one target in one document actually lands on the finding every
 * criterion emits for it. Three criteria judge the same {@code 404} and two judge the same
 * operation here, each deriving its own spec pointer and its own response-code resolution - and a
 * single character of disagreement in how one of them spells {@code httpPath} would make the reach
 * rules correct and the waiver inert.
 */
class OpenApiWaiverCriterionScopeIT extends AbstractOpenApiCoverageServiceIT {

  private static final String PATH = "/pets/{petId}";
  private static final String METHOD = "GET";

  private static final String REASON =
    "the mocked downstream cannot be made to fail";
  private static final LocalDate EXPIRES = LocalDate.of(2026, 6, 15);
  private static final Instant TRIGGERED_AT = EXPIRES.minusDays(7)
    .atTime(NOON)
    .toInstant(UTC);

  @Autowired
  private List<OpenApiCoverageCalculator> openApiCoverageCalculators;

  /**
   * An omitted criterion concedes the target itself, so every criterion that judged it stops
   * judging it - including the one that shares no containment edge with the others.
   */
  @Test
  @VerifiesSw(
    SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
  )
  void shouldReachEveryCriterionJudgingTheTarget_whenNoCriterionIsNamed() {
    var statuses = statusesPerCriterion(waiverOn("404", null), "404");

    assertThat(statuses)
      .containsEntry(ERROR_RESPONSE_CODE_COVERAGE, WAIVED)
      .containsEntry(RESPONSE_CODE_COVERAGE, WAIVED)
      .containsEntry(REQUIRED_ERROR_FIELDS_COVERAGE, WAIVED);
  }

  /**
   * Naming {@code ERROR_RESPONSE_CODE_COVERAGE} concedes that this error code is not exercised, and
   * {@code RESPONSE_CODE_COVERAGE} contains that claim. {@code REQUIRED_ERROR_FIELDS_COVERAGE}
   * judges the same {@code 404} and is contained by nothing: it asks whether the error body carries
   * the fields the schema requires, which is not implied by conceding the code was never hit. It
   * keeps judging - and that is what makes the narrow form a restriction rather than a formality.
   */
  @Test
  @VerifiesSw(
    SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
  )
  void shouldReachTheContainingCriterionAndNotAnUnrelatedOneJudgingTheSameTarget() {
    var statuses = statusesPerCriterion(
      waiverOn("404", ERROR_RESPONSE_CODE_COVERAGE),
      "404"
    );

    assertThat(statuses)
      .containsEntry(ERROR_RESPONSE_CODE_COVERAGE, WAIVED)
      .containsEntry(RESPONSE_CODE_COVERAGE, WAIVED);
    assertThat(statuses.get(REQUIRED_ERROR_FIELDS_COVERAGE))
      .isNotNull()
      .isNotEqualTo(WAIVED);
  }

  /**
   * Upward only. Conceding that response codes in general are not fully exercised says nothing
   * about the error codes in particular, which is the stricter claim - so the waiver stops at the
   * criterion it named.
   */
  @Test
  @VerifiesSw(
    SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
  )
  void shouldNotReachDownwardAlongTheSubsetEdge() {
    var statuses = statusesPerCriterion(
      waiverOn("404", RESPONSE_CODE_COVERAGE),
      "404"
    );

    assertThat(statuses).containsEntry(RESPONSE_CODE_COVERAGE, WAIVED);
    assertThat(statuses.get(ERROR_RESPONSE_CODE_COVERAGE))
      .isNotNull()
      .isNotEqualTo(WAIVED);
  }

  /**
   * The one {@code STRENGTH} edge, which is not a subset relation: the targets of
   * {@code OPERATION_SUCCESS_COVERAGE} and {@code HTTP_METHOD_COVERAGE} are the same operations, and
   * the containment is that succeeding is a stronger demand than being called at all. Conceding the
   * stronger claim concedes the weaker one.
   */
  @Test
  @VerifiesSw(
    SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
  )
  void shouldReachUpwardAlongTheStrengthEdge() {
    var statuses = statusesPerCriterion(
      waiverOn(null, OPERATION_SUCCESS_COVERAGE),
      null
    );

    assertThat(statuses)
      .containsEntry(OPERATION_SUCCESS_COVERAGE, WAIVED)
      .containsEntry(HTTP_METHOD_COVERAGE, WAIVED);
  }

  /**
   * And not downward along it. Conceding that an operation is never called does not concede that it
   * could not have succeeded if it were.
   */
  @Test
  @VerifiesSw(
    SwTraceables.SW_042_CRITERION_NARROWING_REACHES_THE_CONTAINING_CRITERIA
  )
  void shouldNotReachDownwardAlongTheStrengthEdge() {
    var statuses = statusesPerCriterion(
      waiverOn(null, HTTP_METHOD_COVERAGE),
      null
    );

    assertThat(statuses).containsEntry(HTTP_METHOD_COVERAGE, WAIVED);
    assertThat(statuses.get(OPERATION_SUCCESS_COVERAGE))
      .isNotNull()
      .isNotEqualTo(WAIVED);
  }

  /**
   * A waiver names one target, not a criterion: the other response codes of the same operation
   * under the same criterion go on being judged. Without this the reach assertions above would hold
   * just as well of an implementation that waived whole criteria.
   */
  @Test
  @VerifiesSw(SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS)
  void shouldLeaveTheOtherTargetsOfTheSameCriterionJudged() {
    var findings = findingsPerCriterion(inForce(waiverOn("404", null))).get(
      RESPONSE_CODE_COVERAGE
    );

    assertThat(findings)
      .filteredOn(finding -> !"404".equals(finding.responseCode()))
      .isNotEmpty()
      .noneMatch(finding -> WAIVED.equals(finding.status()));
  }

  /**
   * The status each criterion gave the one target the waiver addressed, by criterion. A criterion
   * that emitted no finding for that target is absent rather than present with a null status, which
   * keeps "did not judge it" distinguishable from "judged it and said nothing".
   */
  private Map<OpenApiCoverageCriteria, FindingStatus> statusesPerCriterion(
    Waiver waiver,
    @Nullable String responseCode
  ) {
    return findingsPerCriterion(inForce(waiver))
      .entrySet()
      .stream()
      .flatMap(entry ->
        entry
          .getValue()
          .stream()
          .filter(finding -> addresses(finding, responseCode))
          .findFirst()
          .map(finding -> Map.entry(entry.getKey(), finding.status()))
          .stream()
      )
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<
    OpenApiCoverageCriteria,
    List<ApiTestFinding>
  > findingsPerCriterion(WaiverContext waiverContext) {
    var operations = operations();
    var telemetry = telemetry();

    return openApiCoverageCalculators
      .stream()
      .map(calculator ->
        calculator.calculate(operations, telemetry, waiverContext)
      )
      .collect(
        toMap(OpenApiTestResult::openApiCriteria, OpenApiTestResult::findings)
      );
  }

  private static boolean addresses(
    ApiTestFinding finding,
    @Nullable String responseCode
  ) {
    return (
      PATH.equals(finding.httpPath()) &&
      METHOD.equals(finding.httpMethod()) &&
      Objects.equals(responseCode, finding.responseCode()) &&
      Objects.isNull(finding.parameterName()) &&
      Objects.isNull(finding.contentType())
    );
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

  private static WaiverContext inForce(Waiver waiver) {
    return new WaiverContext(List.of(waiver), TRIGGERED_AT);
  }

  /**
   * One operation, whose {@code 404} carries an error schema with required fields - so the same
   * target is judged by the two response-code criteria and by the required-fields criterion, which
   * is the overlap these claims are about.
   */
  private static Map<String, Operation> operations() {
    var errorSchema = new ObjectSchema()
      .addProperty("code", new StringSchema())
      .addProperty("message", new StringSchema());
    errorSchema.setRequired(List.of("code", "message"));

    var jsonContent = new Content().addMediaType(
      "application/json",
      new MediaType()
    );

    var readPet = new Operation()
      .operationId("readPet")
      .parameters(List.of(new Parameter().in("path").name("petId")))
      .responses(
        new ApiResponses()
          .addApiResponse("200", new ApiResponse().content(jsonContent))
          .addApiResponse(
            "404",
            new ApiResponse().content(
              new Content().addMediaType(
                "application/json",
                new MediaType().schema(errorSchema)
              )
            )
          )
          .addApiResponse("500", new ApiResponse().content(jsonContent))
      );

    return Map.of(METHOD + "_" + PATH, readPet);
  }

  /**
   * A {@code 200} only. The waived {@code 404} is therefore uncovered on its own merits, which is
   * what makes a {@code WAIVED} status in the assertions above attributable to the waiver rather
   * than to the telemetry.
   */
  private static Map<String, List<OpenTelemetryData>> telemetry() {
    ObjectNode attributes = JsonMapper.shared().createObjectNode();
    attributes.put("http.request.method", METHOD);
    attributes.put("url.path", "/pets/42");
    attributes.put("http.response.status_code", "200");

    return Map.of(
      METHOD + "_/pets/42",
      List.of(new OpenTelemetryData("traceId-span", "traceId", attributes))
    );
  }
}
