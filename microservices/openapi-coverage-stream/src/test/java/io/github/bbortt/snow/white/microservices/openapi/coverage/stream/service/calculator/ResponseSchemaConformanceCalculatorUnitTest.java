/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.CONFORMING_RESPONSE;
import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.nonConforming;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_SCHEMA_CONFORMANCE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.ResponseSchemaConformanceCalculator.NO_VERDICT_INFORMATION;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Collections.emptyMap;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.json.JsonMapper;

class ResponseSchemaConformanceCalculatorUnitTest {

  private static final String OPERATION_KEY = "GET_/api/v1/pets";
  private static final String RESPONSE_ENTRY_POINTER =
    "/paths/~1api~1v1~1pets/get/responses/200";

  /**
   * The schema of the response's {@code application/json} entry, as the document addresses it: a
   * violation's pointer is relative to that node, not to the response entry.
   */
  private static String jsonSchema(String relativePointer) {
    return (
      RESPONSE_ENTRY_POINTER +
      "/content/application~1json/schema" +
      relativePointer
    );
  }

  private ResponseSchemaConformanceCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ResponseSchemaConformanceCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenResponseSchemaConformance() {
      assertThat(fixture.accepts(RESPONSE_SCHEMA_CONFORMANCE)).isTrue();
    }

    /**
     * The criterion it inherits its target unit from included: sharing how a response entry
     * collects its telemetry is not sharing what is asked of it.
     */
    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenAnyOtherCriterion(
      OpenApiCoverageCriteria criterion
    ) {
      if (RESPONSE_SCHEMA_CONFORMANCE.equals(criterion)) {
        return;
      }

      assertThat(fixture.accepts(criterion)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenResponseCodeCoverage() {
      assertThat(fixture.accepts(RESPONSE_CODE_COVERAGE)).isFalse();
    }
  }

  @Nested
  class CalculateTest {

    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void reportsThePairCovered_whenEveryVerdictConformed() {
      var result = fixture.calculate(
        operationDocumenting("200"),
        telemetry(
          verdictSpan("200", CONFORMING_RESPONSE),
          verdictSpan("200", CONFORMING_RESPONSE)
        ),
        WaiverContext.none()
      );

      assertThat(result.coverage()).isEqualTo(ratio(1));
      assertThat(result.judgedTargetCount()).isEqualTo(1);
      assertThat(result.findings())
        .extracting(ApiTestFinding::status, ApiTestFinding::specPointer)
        .containsExactly(tuple(COVERED, RESPONSE_ENTRY_POINTER));
    }

    /**
     * One violation anywhere in the pair's telemetry uncovers the pair: a schema the API breaks
     * sometimes is a schema the API does not keep.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void reportsThePairUncovered_whenOneVerdictAmongManyFoundAViolation() {
      var result = fixture.calculate(
        operationDocumenting("200"),
        telemetry(
          verdictSpan("200", CONFORMING_RESPONSE),
          verdictSpan("200", nonConforming(of("/properties/id")))
        ),
        WaiverContext.none()
      );

      assertThat(result.coverage()).isEqualTo(ratio(0));
      assertThat(result.judgedTargetCount()).isEqualTo(1);
      assertThat(result.findings())
        .extracting(ApiTestFinding::status, ApiTestFinding::specPointer)
        .containsExactly(tuple(UNCOVERED, jsonSchema("/properties/id")));
    }

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void producesOneFindingPerDistinctPointer_andOneForAPointerSeenTwice() {
      var result = fixture.calculate(
        operationDocumenting("200"),
        telemetry(
          verdictSpan("200", nonConforming(of("/properties/id", "/required"))),
          verdictSpan("200", nonConforming(of("/properties/id")))
        ),
        WaiverContext.none()
      );

      assertThat(result.findings())
        .extracting(ApiTestFinding::specPointer)
        .containsExactlyInAnyOrder(
          jsonSchema("/properties/id"),
          jsonSchema("/required")
        );
    }

    /**
     * Two pointers the document declares neither of resolve to the same ancestor, and one node
     * reported twice would raise the pair's judged count by two - the same break counted as two.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void producesOneFinding_whenSeveralPointersResolveToTheSameNode() {
      var result = fixture.calculate(
        operationDocumenting("200"),
        telemetry(
          verdictSpan(
            "200",
            nonConforming(of("/properties/nope", "/items/properties/id"))
          )
        ),
        WaiverContext.none()
      );

      assertThat(result.findings())
        .extracting(ApiTestFinding::specPointer)
        .containsExactly(RESPONSE_ENTRY_POINTER);
      assertThat(result.judgedTargetCount()).isEqualTo(1);
    }

    /**
     * A test whose response conformed is not evidence that the response did not: a reader sent to
     * it finds a test that never broke the schema, which is worse than being sent nowhere.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void namesOnlyTheViolatingSpanAsEvidence_whenAnotherSpanConformed() {
      var result = fixture.calculate(
        operationDocumenting("200"),
        telemetry(
          verdictSpan("200", "trace-conformed", CONFORMING_RESPONSE),
          verdictSpan(
            "200",
            "trace-violated",
            nonConforming(of("/properties/id"))
          )
        ),
        WaiverContext.none()
      );

      assertThat(result.findings()).hasSize(1);
      assertThat(result.findings().getFirst().evidence())
        .extracting(FindingEvidence::traceId)
        .containsExactly("trace-violated");
    }

    /**
     * The pointer ladder: a pointer the document does not declare is reported at the nearest node
     * it does, rather than being dropped.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void fallsBackToTheResponseEntry_whenThePointerResolvesNowhere() {
      var result = fixture.calculate(
        operationDocumenting("200"),
        telemetry(
          verdictSpan("200", nonConforming(of("/properties/nothingLikeThis")))
        ),
        WaiverContext.none()
      );

      assertThat(result.findings())
        .extracting(ApiTestFinding::specPointer)
        .containsExactly(RESPONSE_ENTRY_POINTER);
    }

    /**
     * The document is measured as written, so a response schema declared as a reference has no
     * walkable node and every pointer on the pair resolves nowhere. This is the ordinary case
     * rather than a degenerate one - most documents reference their response schemas - so the
     * collapse to one finding per response is pinned here and not planned around.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void producesOneFindingAtTheResponseEntry_whenTheSchemaIsOnlyAReference() {
      var result = fixture.calculate(
        operationDocumentingReferencedSchema("200"),
        telemetry(
          verdictSpan("200", nonConforming(of("/properties/id", "/required")))
        ),
        WaiverContext.none()
      );

      assertThat(result.findings())
        .extracting(ApiTestFinding::specPointer, ApiTestFinding::status)
        .containsExactly(tuple(RESPONSE_ENTRY_POINTER, UNCOVERED));
      assertThat(result.judgedTargetCount()).isEqualTo(1);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
    void leavesAPairWithoutAVerdictUnjudged_whileJudgingTheOneBesideIt() {
      var operations = operationDocumenting("200", "404");

      var result = fixture.calculate(
        operations,
        telemetry(verdictSpan("200", CONFORMING_RESPONSE)),
        WaiverContext.none()
      );

      assertThat(result.coverage()).isEqualTo(ratio(1));
      assertThat(result.judgedTargetCount()).isEqualTo(1);
      assertThat(result.findings())
        .extracting(ApiTestFinding::status, ApiTestFinding::responseCode)
        .containsExactlyInAnyOrder(
          tuple(COVERED, "200"),
          tuple(NOT_APPLICABLE, "404")
        );
      assertThat(result.additionalInformation()).isNull();
    }

    /**
     * An application's own span is not a verdict: a suite reporting none leaves every pair
     * unjudged, which is a different thing from reporting that they all conformed.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void failsAtZeroOverOneTarget_whenNoPairCarriesAVerdict() {
      var result = fixture.calculate(
        operationDocumenting("200", "404"),
        telemetry(span("200", null)),
        WaiverContext.none()
      );

      assertThat(result.coverage()).isEqualTo(ratio(0));
      assertThat(result.judgedTargetCount()).isEqualTo(1);
      assertThat(result.findings())
        .extracting(
          ApiTestFinding::status,
          ApiTestFinding::specPointer,
          ApiTestFinding::httpPath,
          ApiTestFinding::httpMethod,
          ApiTestFinding::responseCode
        )
        .contains(tuple(UNCOVERED, "/paths", null, null, null));
      assertThat(result.findings())
        .filteredOn(finding -> !"/paths".equals(finding.specPointer()))
        .extracting(ApiTestFinding::status)
        .containsOnly(NOT_APPLICABLE);
      assertThat(result.additionalInformation()).isEqualTo(
        NO_VERDICT_INFORMATION
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void emitsNothing_whenTheDocumentDeclaresNoPair() {
      Map<String, Operation> operations = new HashMap<>();
      operations.put(OPERATION_KEY, new Operation());

      var result = fixture.calculate(
        operations,
        telemetry(span("200", null)),
        WaiverContext.none()
      );

      assertThat(result.findings()).isEmpty();
      assertThat(result.coverage()).isEqualTo(ratio(1));
      assertThat(result.additionalInformation()).isNull();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void dropsTheRootFindingAndTheText_onceASinglePairCarriesAVerdict() {
      var result = fixture.calculate(
        operationDocumenting("200", "404"),
        telemetry(verdictSpan("200", CONFORMING_RESPONSE)),
        WaiverContext.none()
      );

      assertThat(result.findings())
        .extracting(ApiTestFinding::specPointer)
        .doesNotContain("/paths");
      assertThat(result.additionalInformation()).isNull();
    }

    @Test
    void judgesNothing_whenNoTelemetryReachedTheOperationAtAll() {
      var result = fixture.calculate(
        operationDocumenting("200"),
        emptyMap(),
        WaiverContext.none()
      );

      assertThat(result.judgedTargetCount()).isEqualTo(1);
      assertThat(result.findings())
        .extracting(ApiTestFinding::specPointer)
        .contains("/paths");
    }

    /**
     * The harness's spans are the only ones this calculator has anything to read, so it is the one
     * calculator that is handed them.
     */
    @Test
    @VerifiesCon(
      ConTraceables.CON_015_HARNESS_SPAN_NEVER_MOVES_A_COVERAGE_VERDICT
    )
    void readsHarnessSpans() {
      assertThat(fixture.readsHarnessSpans()).isTrue();
    }

    /**
     * Read off the root finding rather than derived again: waivers rewrite the statuses the
     * derivation looked at, and a waiver on any one pair would answer "something was judged" and
     * drop the explanation of a gate still failing at 0 over 1.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void keepsTheText_whenAWaiverRewroteAPairOfTheUnjudgedApi() {
      var information = fixture.getAdditionalInformationOrNull(
        new AbstractOpenApiCoverageCalculator.Calculation(
          operationDocumenting("200"),
          emptyMap(),
          of(
            ApiTestFinding.builder()
              .specPointer(RESPONSE_ENTRY_POINTER)
              .status(NOT_APPLICABLE)
              .evidence(of())
              .build()
              .waivedBecause("Not judged yet", LocalDate.now().plusDays(1)),
            ApiTestFinding.builder()
              .specPointer("/paths")
              .status(UNCOVERED)
              .evidence(of())
              .build()
          )
        )
      );

      assertThat(information).isEqualTo(NO_VERDICT_INFORMATION);
    }

    /**
     * The case the no-verdict text alone would misdescribe: a harness judged a response, the
     * document declares no entry that response could satisfy, and the ratio was taken over nothing.
     * Both sentences are needed - one says no documented response was judged, the other says a
     * verdict was read and left uncounted, which is the half no finding and no ratio can show.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void namesTheDrop_whenAVerdictMatchedNoDocumentedEntry() {
      var result = fixture.calculate(
        operationDocumenting("200"),
        telemetry(verdictSpan("500", nonConforming(of("/properties/id")))),
        WaiverContext.none()
      );

      assertThat(result.additionalInformation())
        .startsWith(NO_VERDICT_INFORMATION)
        .contains("published 1 response schema conformance verdict(s)")
        .contains("1 that matched no documented response (status code(s) 500)");
      assertThat(result.coverage()).isEqualTo(ratio(0));
    }

    /**
     * A verdict whose span names no status code is the third way a verdict goes uncounted, and the
     * one an observed-code walk cannot see: it never becomes an observed code to begin with. The
     * no-verdict text asserts two causes, so leaving this one unnamed would describe a calculation
     * by neither of them.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void namesTheCause_whenAJudgedSpanCarriesNoStatusCode() {
      var result = fixture.calculate(
        operationDocumenting("200"),
        telemetry(
          new OpenTelemetryData(
            "span-no-status",
            "trace-no-status",
            JsonMapper.shared().createObjectNode(),
            null,
            CONFORMING_RESPONSE
          )
        ),
        WaiverContext.none()
      );

      assertThat(result.additionalInformation())
        .startsWith(NO_VERDICT_INFORMATION)
        .contains("published 1 response schema conformance verdict(s)")
        .contains("1 on spans carrying no readable HTTP status code")
        .doesNotContain("matched no documented response");
    }

    /**
     * A verdict is equally unplaced when its span reached no documented operation, and the status
     * code it names may be declared under every operation but that one. So the drop is reported as
     * having matched no documented response, never as a code the specification does not declare -
     * the second would be false here, where `200` is declared.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void doesNotCallTheCodeUndeclared_whenTheSpanReachedNoDocumentedOperation() {
      Map<String, List<OpenTelemetryData>> telemetry = new HashMap<>();
      telemetry.put(
        "GET_/api/v1/health",
        of(verdictSpan("200", CONFORMING_RESPONSE))
      );

      var result = fixture.calculate(
        operationDocumenting("200"),
        telemetry,
        WaiverContext.none()
      );

      assertThat(result.additionalInformation())
        .contains("published 1 response schema conformance verdict(s)")
        .contains("1 that matched no documented response (status code(s) 200)")
        .doesNotContain("response codes this specification does not declare");
    }

    /**
     * One span, one drop. A concrete request key matches the pattern of every templated operation
     * it could have been routed to, so the span below is collected by both documented operations -
     * and counting what each operation failed to place would report it twice, against an operation
     * no request reached.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void countsOneDropOnce_whenATemplatedOperationAlsoCollectsTheSpan() {
      Map<String, Operation> operations = new HashMap<>();
      operations.put("GET_/api/v1/pets/mine", documenting("200"));
      operations.put("GET_/api/v1/pets/{petId}", documenting("200"));

      Map<String, List<OpenTelemetryData>> telemetry = new HashMap<>();
      telemetry.put(
        "GET_/api/v1/pets/mine",
        of(verdictSpan("500", nonConforming(of("/properties/id"))))
      );

      var result = fixture.calculate(
        operations,
        telemetry,
        WaiverContext.none()
      );

      assertThat(result.additionalInformation())
        .contains("published 1 response schema conformance verdict(s)")
        .contains("1 that matched no documented response (status code(s) 500)");
    }

    /**
     * A documented catch-all declares every response the operation can answer with, so the verdict
     * lands on that entry and there is no drop to report.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_048_RESPONSE_SCHEMA_CONFORMANCE_CRITERION)
    void saysNothing_whenADocumentedDefaultEntryCaughtTheResponse() {
      var result = fixture.calculate(
        operationDocumenting("200", "default"),
        telemetry(verdictSpan("500", CONFORMING_RESPONSE)),
        WaiverContext.none()
      );

      assertThat(result.additionalInformation()).isNull();
      assertThat(result.coverage()).isEqualTo(ratio(1));
    }
  }

  private static BigDecimal ratio(double value) {
    return BigDecimal.valueOf(value).setScale(2, HALF_UP);
  }

  /**
   * One operation documenting the given response codes, each with a JSON response declaring an
   * {@code id} property - the node a violation pointer resolves into.
   */
  private static Map<String, Operation> operationDocumenting(
    String... responseCodes
  ) {
    Map<String, Operation> operations = new HashMap<>();
    operations.put(OPERATION_KEY, documenting(responseCodes));

    return operations;
  }

  /**
   * One operation documenting the given response codes, for a test that places it under an
   * operation key of its own.
   */
  private static Operation documenting(String... responseCodes) {
    var responses = new ApiResponses();

    for (var responseCode : responseCodes) {
      responses.addApiResponse(
        responseCode,
        new ApiResponse()
          .description("Test")
          .content(
            new Content().addMediaType(
              "application/json",
              new MediaType().schema(petSchema())
            )
          )
      );
    }

    return new Operation().responses(responses);
  }

  private static Schema<?> petSchema() {
    var schema = new ObjectSchema();
    schema.addProperty("id", new StringSchema());
    schema.addRequiredItem("id");

    return schema;
  }

  /**
   * The same operation with the response schema referenced instead of inlined, as a document
   * declaring reusable components states it - and as the parser leaves it, since nothing
   * dereferences the document before it is measured.
   */
  private static Map<String, Operation> operationDocumentingReferencedSchema(
    String responseCode
  ) {
    var responses = new ApiResponses();
    responses.addApiResponse(
      responseCode,
      new ApiResponse()
        .description("Test")
        .content(
          new Content().addMediaType(
            "application/json",
            new MediaType().schema(
              new Schema<>().$ref("#/components/schemas/Pet")
            )
          )
        )
    );

    Map<String, Operation> operations = new HashMap<>();
    operations.put(OPERATION_KEY, new Operation().responses(responses));

    return operations;
  }

  private static Map<String, List<OpenTelemetryData>> telemetry(
    OpenTelemetryData... spans
  ) {
    Map<String, List<OpenTelemetryData>> telemetry = new HashMap<>();
    telemetry.put(OPERATION_KEY, List.of(spans));

    return telemetry;
  }

  private static OpenTelemetryData verdictSpan(
    String statusCode,
    ConformanceVerdict verdict
  ) {
    return span(statusCode, verdict);
  }

  /**
   * A judged span naming its own trace, for the assertions that read which span a finding points
   * at: spans of one status code otherwise share a trace id, and evidence is deduplicated by it.
   */
  private static OpenTelemetryData verdictSpan(
    String statusCode,
    String traceId,
    ConformanceVerdict verdict
  ) {
    var attributes = JsonMapper.shared().createObjectNode();
    attributes.put("http.response.status_code", statusCode);

    return new OpenTelemetryData(
      "span-" + traceId,
      traceId,
      attributes,
      null,
      verdict
    );
  }

  private static OpenTelemetryData span(
    String statusCode,
    @Nullable ConformanceVerdict verdict
  ) {
    var attributes = JsonMapper.shared().createObjectNode();
    attributes.put("http.response.status_code", statusCode);

    return new OpenTelemetryData(
      "span-" + statusCode + "-" + System.nanoTime(),
      "trace-" + statusCode,
      attributes,
      null,
      verdict
    );
  }
}
