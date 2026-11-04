/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.NO_UNDOCUMENTED_RESPONSE_CODES;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_SCHEMA_CONFORMANCE;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ConformanceVerdictReader.Warnings;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class OpenApiCoverageCalculatorIT extends AbstractOpenApiCoverageServiceIT {

  private static final String TEST_CASE_NAME_ATTRIBUTE = "test.case.name";

  private static final String RESPONSE_SCHEMA_VALID_ATTRIBUTE =
    "openapi.response.schema.valid";

  private static final String RESPONSE_SCHEMA_VIOLATIONS_ATTRIBUTE =
    "openapi.response.schema.violations";

  private static final String A_TEST_CASE =
    "org.example.PetstoreIT.shouldRejectUnknownPet";

  @Autowired
  private List<OpenApiCoverageCalculator> openApiCoverageCalculators;

  @Test
  void aCalculatorShouldExistForEachOpenApiCoverageCriteria() {
    var uncoveredCriteria = stream(OpenApiCoverageCriteria.values())
      .filter(
        openApiCriteria ->
          openApiCoverageCalculators
            .stream()
            .filter(openApiCoverageCalculator ->
              openApiCoverageCalculator.accepts(openApiCriteria)
            )
            .count() != 1
      )
      .toList();

    assertThat(uncoveredCriteria).isEmpty();
  }

  /**
   * A test identity changes what a report says and never what it scores. Adopting the convention
   * must therefore be invisible to every criterion: identical findings, identical statuses and an
   * identical ratio over telemetry that differs only by carrying the attribute.
   */
  @Test
  @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
  void everyCriterionShouldScoreTheSameWithAndWithoutATestIdentity() {
    var operations = operations();

    var withoutTestIdentity = telemetry(false);
    var withTestIdentity = hoistTestIdentity(telemetry(true));

    assertThat(openApiCoverageCalculators).allSatisfy(calculator ->
      assertThat(
        calculator.calculate(operations, withTestIdentity, WaiverContext.none())
      )
        .usingRecursiveComparison()
        .ignoringFields("duration")
        .ignoringFieldsMatchingRegexes(".*testCaseName")
        .isEqualTo(
          calculator.calculate(
            operations,
            withoutTestIdentity,
            WaiverContext.none()
          )
        )
    );
  }

  /**
   * Guards the invariance assertion above against passing vacuously: the telemetry it runs over has
   * to produce evidence that actually names a test, and evidence that names none.
   */
  @Test
  @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
  void evidenceShouldNameTheTestThatSatisfiedItWhereTheSpanCarriedOne() {
    var operations = operations();
    var telemetry = hoistTestIdentity(telemetry(true));

    var testCaseNames = openApiCoverageCalculators
      .stream()
      .map(calculator ->
        calculator.calculate(operations, telemetry, WaiverContext.none())
      )
      .flatMap(result -> result.findings().stream())
      .map(ApiTestFinding::evidence)
      .flatMap(List::stream)
      .map(FindingEvidence::testCaseName)
      .distinct()
      .toList();

    assertThat(testCaseNames).contains(A_TEST_CASE).containsNull();
  }

  /**
   * A harness's spans are admitted to the analysed dataset to supply conformance verdicts, and for
   * nothing else. Enabling conformance reporting must therefore leave every pre-existing criterion
   * exactly where it was - the same statuses, the same ratio, the same findings - on an API whose
   * behaviour did not change.
   */
  @Test
  @VerifiesCon(
    ConTraceables.CON_015_HARNESS_SPAN_NEVER_MOVES_A_COVERAGE_VERDICT
  )
  void noCriterionButConformanceShouldMoveWhenHarnessSpansArePresent() {
    var operations = operations();

    var applicationOnly = hoistVerdicts(telemetry(true));
    var withHarnessSpans = hoistVerdicts(withHarnessSpans(telemetry(true)));

    assertThat(openApiCoverageCalculators)
      .filteredOn(
        calculator -> !calculator.accepts(RESPONSE_SCHEMA_CONFORMANCE)
      )
      .allSatisfy(calculator ->
        assertThat(
          calculator.calculate(
            operations,
            withHarnessSpans,
            WaiverContext.none()
          )
        )
          .usingRecursiveComparison()
          .ignoringFields("duration")
          .isEqualTo(
            calculator.calculate(
              operations,
              applicationOnly,
              WaiverContext.none()
            )
          )
      );
  }

  /**
   * Guards the invariance above against passing vacuously twice over. The harness spans carry a
   * {@code 418} on an operation that documents no such code and a path of their own, so an inverted
   * criterion consulting them would report undocumented traffic the API never served; and the
   * conformance criterion is asserted to differ between the two runs, which it can only do if the
   * spans reached it.
   */
  @Test
  @VerifiesCon(
    ConTraceables.CON_015_HARNESS_SPAN_NEVER_MOVES_A_COVERAGE_VERDICT
  )
  void onlyConformanceShouldSeeTheHarnessSpansItWasGivenThem() {
    var operations = operations();

    var applicationOnly = hoistVerdicts(telemetry(true));
    var withHarnessSpans = hoistVerdicts(withHarnessSpans(telemetry(true)));

    var conformance = openApiCoverageCalculators
      .stream()
      .filter(calculator -> calculator.accepts(RESPONSE_SCHEMA_CONFORMANCE))
      .findFirst()
      .orElseThrow();

    // The findings rather than the whole result: a result carries its own duration, and two of
    // those are never equal, which would make this assertion pass however inert the spans were.
    assertThat(
      conformance
        .calculate(operations, withHarnessSpans, WaiverContext.none())
        .findings()
    ).isNotEqualTo(
      conformance
        .calculate(operations, applicationOnly, WaiverContext.none())
        .findings()
    );

    var undocumentedCodes = openApiCoverageCalculators
      .stream()
      .filter(calculator -> calculator.accepts(NO_UNDOCUMENTED_RESPONSE_CODES))
      .findFirst()
      .orElseThrow();

    assertThat(
      undocumentedCodes
        .calculate(operations, withHarnessSpans, WaiverContext.none())
        .findings()
    ).isEqualTo(
      undocumentedCodes
        .calculate(operations, applicationOnly, WaiverContext.none())
        .findings()
    );
  }

  /**
   * The given telemetry plus two spans a harness exported: one verdict on a documented pair, and
   * one on a {@code 418} nothing documents.
   */
  private static Map<String, List<OpenTelemetryData>> withHarnessSpans(
    Map<String, List<OpenTelemetryData>> telemetry
  ) {
    Map<String, List<OpenTelemetryData>> withHarnessSpans = new HashMap<>(
      telemetry
    );

    withHarnessSpans.merge(
      "GET_/pets/42",
      List.of(
        verdictSpan("traceId5", "200", true, null),
        verdictSpan("traceId6", "418", false, "[\"/properties/code\"]")
      ),
      (existing, added) -> concat(existing.stream(), added.stream()).toList()
    );

    return withHarnessSpans;
  }

  private static Map<String, List<OpenTelemetryData>> hoistVerdicts(
    Map<String, List<OpenTelemetryData>> telemetry
  ) {
    return telemetry
      .entrySet()
      .stream()
      .collect(
        toMap(Entry::getKey, entry ->
          entry
            .getValue()
            .stream()
            .map(data ->
              data.withConformanceVerdictFrom(
                RESPONSE_SCHEMA_VALID_ATTRIBUTE,
                RESPONSE_SCHEMA_VIOLATIONS_ATTRIBUTE,
                new Warnings()
              )
            )
            .toList()
        )
      );
  }

  /**
   * A span as the shipped producer exports one: no request method and no path, since the pair is
   * located by operation id and status code alone.
   */
  private static OpenTelemetryData verdictSpan(
    String traceId,
    String responseCode,
    boolean valid,
    @Nullable String violations
  ) {
    ObjectNode attributes = JsonMapper.shared().createObjectNode();
    attributes.put("http.response.status_code", responseCode);
    attributes.put("openapi.operation.id", "readPet");
    attributes.put(RESPONSE_SCHEMA_VALID_ATTRIBUTE, String.valueOf(valid));

    if (nonNull(violations)) {
      attributes.put(RESPONSE_SCHEMA_VIOLATIONS_ATTRIBUTE, violations);
    }

    return new OpenTelemetryData(traceId + "-span", traceId, attributes);
  }

  private static Map<String, List<OpenTelemetryData>> hoistTestIdentity(
    Map<String, List<OpenTelemetryData>> telemetry
  ) {
    return telemetry
      .entrySet()
      .stream()
      .collect(
        toMap(Entry::getKey, entry ->
          entry
            .getValue()
            .stream()
            .map(data -> data.withTestIdentityFrom(TEST_CASE_NAME_ATTRIBUTE))
            .toList()
        )
      );
  }

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
      .parameters(
        List.of(
          new Parameter().in("path").name("petId"),
          new Parameter().in("query").name("page"),
          new Parameter().in("header").name("X-Api-Key")
        )
      )
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
          .addApiResponse("default", new ApiResponse().content(jsonContent))
      );

    var createPet = new Operation()
      .operationId("createPet")
      .requestBody(
        new RequestBody().content(
          new Content()
            .addMediaType("application/json", new MediaType())
            .addMediaType("multipart/form-data", new MediaType())
        )
      )
      .responses(
        new ApiResponses()
          .addApiResponse("201", new ApiResponse())
          .addApiResponse("400", new ApiResponse().content(jsonContent))
      );

    return Map.of("GET_/pets/{petId}", readPet, "POST_/pets", createPet);
  }

  /**
   * Four spans over two operations: a success, a documented error, an undocumented {@code 418} that
   * the inverted criteria judge, and a creation. With {@code withTestIdentity}, only the first,
   * second and fourth carry the attribute, so both branches of the identity lookup are exercised;
   * without it no span carries it at all, which is what the invariance claim compares against.
   */
  private static Map<String, List<OpenTelemetryData>> telemetry(
    boolean withTestIdentity
  ) {
    var testCaseName = withTestIdentity ? A_TEST_CASE : null;
    return Map.of(
      "GET_/pets/42",
      List.of(
        span("traceId1", "GET", "/pets/42", "200", testCaseName),
        span("traceId2", "GET", "/pets/42", "404", testCaseName),
        span("traceId3", "GET", "/pets/42", "418", null)
      ),
      "POST_/pets",
      List.of(span("traceId4", "POST", "/pets", "201", testCaseName))
    );
  }

  private static OpenTelemetryData span(
    String traceId,
    String httpMethod,
    String urlPath,
    String responseCode,
    @Nullable String testCaseName
  ) {
    ObjectNode attributes = JsonMapper.shared().createObjectNode();
    attributes.put("http.request.method", httpMethod);
    attributes.put("url.path", urlPath);
    attributes.put("http.response.status_code", responseCode);
    attributes.put("url.query", "page=2");
    attributes.put("http.request.header.content-type", "application/json");
    attributes.put("http.request.header.x-api-key", "aKey");

    if (nonNull(testCaseName)) {
      attributes.put(TEST_CASE_NAME_ATTRIBUTE, testCaseName);
    }

    return new OpenTelemetryData(traceId + "-span", traceId, attributes);
  }
}
